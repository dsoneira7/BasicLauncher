package com.example.basiclauncher

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.util.Log
import android.util.SparseArray
import androidx.core.util.containsKey
import androidx.core.util.valueIterator
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.example.basiclauncher.classes.AppIcon
import com.example.basiclauncher.classes.CustomLinearLayoutState
import com.example.basiclauncher.room.AbstractAppDatabase
import java.lang.ref.WeakReference
import java.util.*

const val NUMBER_OF_PAGES_KEY: String = "nPages"
/**
 * Clase Singleton que se utiliza para acceder a la Base de Datos e interactuar con Room.
 */
class Repository private constructor(private val context: Context) {
    private var appList: LiveData<Array<AppIcon>> = MutableLiveData()
    private var appSparseArrayLiveData: MutableLiveData<SparseArray<AppIcon>> = MutableLiveData()
    //Mapea livedata de los estados de las celdas de cada página según la página.
    private var stateListMap = SparseArray<LiveData<Array<CustomLinearLayoutState>>>()
    private val myDao = AbstractAppDatabase.getInstance(context)?.myDao()
    private val stateDao = AbstractAppDatabase.getInstance(context)?.stateDao()
    //Guarda el último estado que se ha eliminado por si hay que revertirlo.
    private var lastDeletedItem: CustomLinearLayoutState? = null
    //Si hay que borrar una página y reestructurar las páginas se guarda el número de la página
    //que se va a borrar.
    private var pageDeleted:Int = -1
    val nPagesLiveData = MutableLiveData<Int>()
    private var sharedPref : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * Lanza el hilo de actualización de la lista de aplicaciones
     */
    fun updateAppList(intent: Intent?) {
        Thread {
            val packageName = intent!!.data.toString()
            if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
                myDao!!.insertApp(AppIcon(
                        packageName,
                        Helper.getAppName(context, packageName),
                        Helper.getActivityIcon(context, packageName)
                ))
            }
            else {
                myDao!!.deleteApp(findAppByPackageName(packageName)!!)
                if (nPagesLiveData.value!! > 1) {
                    //Comprobamos si había accesos directos de esta aplicación que se hayan
                    //borrado al desinstalarla que hayan dejado páginas vacías con necesidad de
                    //recolocación.
                    for (j in 1 until nPagesLiveData.value!!) {
                        comprobeIfPageIsDeletable(j)
                        reallocatePages(pageDeleted, nPagesLiveData.value!!)
                    }
                }
            }
        }.start()
    }

    /**
     * @param packageName: Nombre de la aplicación cuyo [AppIcon] queremos obtener
     *
     * @return [AppIcon] obtenido.
     */
    private fun findAppByPackageName(packageName: String): AppIcon? {
        for (i in appList.value!!) {
            if (i.packageName == packageName) {
                return i
            }
        }
        return null
    }


    init {
        var nPages = sharedPref.getInt(NUMBER_OF_PAGES_KEY, 0)
        if(nPages == 0){
            sharedPref.edit().putInt(NUMBER_OF_PAGES_KEY, 1).apply()
            nPages = 1
        }
        nPagesLiveData.postValue(nPages)

        //Inicializamos los arrays y mapas
        appSparseArrayLiveData.value = SparseArray()
        appList = myDao!!.getAppList()

        //Observamos un livedata de un array "raw" que viene directo de la BBDD. Cuando se actualiza,
        //actualizamos un mapa que nos permite trabajar de una manera mucho mas facil con las
        //aplicaciones, ya que su key pasa a ser su ID
        appList.observeForever {
            appSparseArrayLiveData.postValue(
                    appSparseArrayLiveData.value!!.apply {
                        if (it.size > this.size()) {
                            for (i in it) {
                                if (!this.containsKey(i.id)) {
                                    this.put(i.id, i)
                                }
                            }
                        }
                    }
            )
        }

        //Añadimos el broadcast receiver de intents de instalación y desinstalación
        val installEventBroadcastReceiver = InstallEventBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addDataScheme("package")
        context.registerReceiver(installEventBroadcastReceiver, intentFilter)
    }

    /**
     * Se llama cuando se ha hehco un drop para una celda.
     *
     * @param packageName: Nombre del paquete de la app que se ha droppeado
     * @param page: Página en la que se encuentra la celda en la que se ha droppeado
     * @param position: Posición de la celda en cuestión.
     */
    fun stateOccupied(packageName: String, page: Int, position: Int) {
        Thread{
            var effectivePage = page
            if(pageDeleted != -1 && pageDeleted != page) {
                if(page>pageDeleted){
                    //Si se va a proceder a recolocar páginas, y la página en la que se ha droppeado este
                    //icono está a la derecha de la borrada, entonces este icono pasa a la página anterior
                    //y se procede a la recolocación del resto
                    effectivePage--
                }
                //Al colocar un nuevo icono es cuando se procede al borrado de las páginas y recolocación
                //de las que fueran necesarias.
                Thread{
                    reallocatePages(pageDeleted, nPagesLiveData.value!!)
                }.start()

                if(page == nPagesLiveData.value!!){
                    //Si la página en la que se ha droppeado el icono es la página nueva, se procede a
                    //a añadirla definitivamente.
                    updateNumberOfPages(nPagesLiveData.value!!+1)
                }
            }

            stateDao!!.insertState(CustomLinearLayoutState(
                    effectivePage,
                    position,
                    findAppByPackageName(packageName)!!.id))
        }.start()
    }

    /**
     * Borra un estado de la BBDD
     *
     * @param page: Página en la que se encuentra la celda cuyo estado vamos a borrar (vaciar).
     * @param position: Posición de dicha celda.
     */
    fun deleteItem(page: Int, position: Int) {
        Thread{
            for (i in stateListMap[page].value!!) {
                if (i.position == position) {
                    //Guardamos una instancia del item borrado por si hay que revertir la operación
                    lastDeletedItem = i
                }
            }

            stateDao!!.deleteState(CustomLinearLayoutState(page, position))
            //Después de borrar el item comprobamos si la página se queda vacía y se puede proceder
            //a borrarla
            comprobeIfPageIsDeletable(page)
        }.start()
    }

    /**
     * Revierte el último item borrado.
     */
    fun revertLastDrag() {
        Thread{
            pageDeleted = -1
            stateDao!!.insertState(lastDeletedItem!!)
        }.start()
    }

    /**
     * Comprueba si la página indicada está vacía y puede ser borrada una vez se haga un drop
     *
     * @param page: Página en cuestión.
     */
    private fun comprobeIfPageIsDeletable(page: Int) {
        //Comprobamos que:
        //la página en cuestión no sea la 0 (esa no se puede borrar)
        //no haya estados de la página en cuestión.
        //la página no sea la página que se añade cuando se inicia un drag.
        //Si cumple estas condiciones: la página puede ser borrada
        if (page != 0
                &&
                (stateListMap.get(page).value == null || stateListMap.get(page).value!!.size == 1)
                &&
                page <= nPagesLiveData.value!!) {
            if (page == nPagesLiveData.value!!-1) {
                //Si la página que se ha quedado vacía es la última, no añadimos una nueva página
                //para el drag.
                updateNumberOfPages(nPagesLiveData.value!! - 1)
            }

            //Guardamos la página que puede ser borrada. Se comprobará de nuevo una vez que se haga
            //un drop. no hay ninguna página que pueda ser borrada esta página queda a -1
            pageDeleted = page

        }
    }

    /**
     * Este método recoloca las páginas que haya desde la página borrada hasta el final.
     *
     * @param page: Página borrada
     * @param npages: número de páginas.
     */
    private fun reallocatePages(page: Int, npages: Int) {
        if(page!=npages) {
            //Se borra la página.
            updateNumberOfPages(nPagesLiveData.value!! - 1)
        }

        for (i in page until npages) {
            if (i == -1) {
                continue
            }
            for (state in stateListMap.get(i).value!!.iterator()) {
                //La recolocación consiste en modificar en la BBDD los estados de páginas posteriores,
                //para que ahora estos pertenezcan a la página anterior.
                if (i != page) {
                    stateDao!!.deleteState(state)
                }
                state.page = state.page - 1
                stateDao!!.insertState(state)
            }
        }

        //La página ha sido borrada, podemos ponerlad e nuevo a -1
        pageDeleted = -1
    }

    /**
     * @return LiveData de un mapa con el ID de la aplicación como key
     */
    fun getAppList(): LiveData<SparseArray<AppIcon>> = appSparseArrayLiveData

    fun getAppListOrderedByAppName(): LiveData<Array<AppIcon>> = appList

    /**
     * @param page: Página cuyo mapa de estados queremos recuperar
     *
     * @return Livedata con mapa de estados de las celdas de una página concreta
     */
    fun getStateListByPage(page: Int): LiveData<Array<CustomLinearLayoutState>> {
        if (stateListMap.get(page) == null) {
            stateListMap.put(page, stateDao!!.getAllStatesByPage(page))
        }
        return stateListMap.get(page)
    }


    /**
     * Llamada que inicia un borrado de páginas si es pertinente
     */
    fun updateIfNecessary(){
        if(pageDeleted!=-1){
            reallocatePages(pageDeleted, nPagesLiveData.value!!)
        }
    }

    fun clean() {
        instance = null
    }

    fun updateNumberOfPages(nPages: Int){
        sharedPref.edit().putInt(NUMBER_OF_PAGES_KEY, nPages).apply()
        nPagesLiveData.postValue(nPages)
    }

    fun getIconsPerRow(): Int{
        val iconsPerRow = sharedPref.getInt(context.resources.getString(R.string.icon_size) , 0)
        if(iconsPerRow == 0){
            sharedPref.edit().putInt(context.resources.getString(R.string.icon_size), 4).apply()
            return 4
        }
        return iconsPerRow
    }


    companion object {
        //El context que se le pasa es el applicationContext, por eso no hay fugas en este caso
        @SuppressLint("StaticFieldLeak")
        private var instance: Repository? = null
        fun getInstance(ctx: Context): Repository? {
            if (instance == null) {
                instance = Repository(ctx)
            }
            return instance
        }
    }
}