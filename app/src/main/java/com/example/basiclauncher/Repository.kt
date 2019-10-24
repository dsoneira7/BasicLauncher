package com.example.basiclauncher

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.util.SparseArray
import androidx.core.util.containsKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.basiclauncher.classes.AppIcon
import com.example.basiclauncher.classes.CustomLinearLayoutState
import com.example.basiclauncher.room.AbstractAppDatabase
import java.lang.ref.WeakReference
import java.util.*

/**
 * Clase Singleton que se utiliza para acceder a la Base de Datos e interactuar con Room.
 */
class Repository private constructor(ctx: Context) {
    private var context: WeakReference<Context> = WeakReference(ctx)
    private var appList: LiveData<Array<AppIcon>> = MutableLiveData()
    private var appSparseArrayLiveData: MutableLiveData<SparseArray<AppIcon>> = MutableLiveData()
    //Mapea livedata de los estados de las celdas de cada página según la página.
    private var stateListMap = SparseArray<LiveData<Array<CustomLinearLayoutState>>>()
    private val myDao = AbstractAppDatabase.getInstance(context.get()!!)?.myDao()
    private val stateDao = AbstractAppDatabase.getInstance(context.get()!!)?.stateDao()
    private var nPages = 0
    //Guarda el último estado que se ha eliminado por si hay que revertirlo.
    private var lastDeletedItem: CustomLinearLayoutState? = null
    //Si hay que borrar una página y reestructurar las páginas se guarda el número de la página
    //que se va a borrar.
    private var pageDeleted:Int = -1

    //Hilo que actualiza la lista de aplicaciones del sistema cuando se instala o desinstala alguna
    private val getAppDrawerListThread = Runnable {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        //Obtenemos los packageNames de todas las aplicaciones a partir de sus activities Launcher
        val apps = context.get()!!.packageManager.queryIntentActivities(mainIntent, 0)
        val packageNamesList = ArrayList<String>()
        for (i in apps) {
            if(!packageNamesList.contains(i.activityInfo.packageName)) {
                packageNamesList.add(i.activityInfo.packageName)
            }
        }

        //Se comparan los datos guardados con los datos obtenidso ahora. A los datos obtenidos se le
        //resta un paquete. Esto es porque no se guarda el paquete propio
        if (apps.size - 1 < appList.value!!.size) {
            //Si el número de apps guardadas es mayor que los obtenidos ahora, se sabe que se ha
            //desinstalado una aplicación. Se busca cual ha sido y se actualiza la BBDD
            for (i in appList.value!!) {
                if (!packageNamesList.contains(i.packageName)) {
                    Thread { myDao!!.deleteApp(myDao.getAppById(i.id)) }.start()
                    appSparseArrayLiveData.postValue(
                            appSparseArrayLiveData.value!!.apply {
                                this.delete(i.id)
                            }
                    )
                    if (nPages > 1) {
                        //Comprobamos si había accesos directos de esta aplicación que se hayan
                        //borrado al desinstalarla que hayan dejado páginas vacías con necesidad de
                        //recolocación.
                        for (j in 1 until nPages) {
                            comprobeIfPageIsDeletable(j)
                        }
                    }
                }
            }
        } else if (apps.size - 1 > appList.value!!.size) {
            //En este caso sabemos que se ha instalad una aplicación. Buscamos cual es la nueva,
            //la añadimos y actualizamos la BBDD
            var packageNameFound: Boolean
            for (i in packageNamesList) {
                packageNameFound = false
                if (i == context.get()!!.packageName) {
                    //No tenemos en cuenta la app propia
                    continue
                }
                for (j in appList.value!!) {
                    if (j.packageName == i) {
                        packageNameFound = true
                        break
                    }
                }
                if (!packageNameFound) {
                    Thread {
                        myDao!!.insertApp(AppIcon(
                                i,
                                Helper.getAppName(context.get()!!, i),
                                Helper.getActivityIcon(context.get()!!, i)
                        ))
                    }.start()
                    break
                }

            }

        }

    }

    /**
     * Lanza el hilo de actualización de la lista de aplicaciones
     */
    fun updateAppList() {
        getAppDrawerListThread.run()
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
        //Inicializamos los arrays y mapas
        appSparseArrayLiveData.value = SparseArray()
        appList = myDao!!.getAppList()
        nPages = Helper.getFromSharedPreferences(context.get()!!.packageName,
                "nPages", "0", context.get()!!.applicationContext)!!.toInt()
        if (nPages == 0) {
            nPages = 1
        }

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
        context.get()!!.registerReceiver(installEventBroadcastReceiver, intentFilter)
    }

    /**
     * Se llama cuando se ha hehco un drop para una celda.
     *
     * @param packageName: Nombre del paquete de la app que se ha droppeado
     * @param page: Página en la que se encuentra la celda en la que se ha droppeado
     * @param position: Posición de la celda en cuestión.
     */
    fun stateOccupied(packageName: String, page: Int, position: Int) {
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
                reallocatePages(pageDeleted, nPages)
            }.start()

            if(page == nPages){
                //Si la página en la que se ha droppeado el icono es la página nueva, se procede a
                //a añadirla definitivamente, si no, se borra.
                nPages++
                Helper.putInSharedPreferences(context.get()!!.packageName,
                        "nPages", nPages.toString(), context.get()!!.applicationContext)
            }
        }

        stateDao!!.insertState(CustomLinearLayoutState(
                effectivePage,
                position,
                findAppByPackageName(packageName)!!.id))
    }

    /**
     * Borra un estado de la BBDD
     *
     * @param page: Página en la que se encuentra la celda cuyo estado vamos a borrar (vaciar).
     * @param position: Posición de dicha celda.
     */
    fun deleteItem(page: Int, position: Int) {
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
    }

    /**
     * Revierte el último item borrado.
     */
    fun revertLastDrag() {
        pageDeleted = -1
        stateDao!!.insertState(lastDeletedItem!!)
    }

    /**
     * Comprueba si la página indicada está vacía y puede ser borrada una vez se haga un drop
     *
     * @param page: Página en cuestión.
     */
    private fun comprobeIfPageIsDeletable(page: Int) {
        nPages = Helper.getFromSharedPreferences(context.get()!!.packageName,
                "nPages", "0", context.get()!!.applicationContext)!!.toInt()
        //Comprobamos que:
        //la página en cuestión no sea la 0 (esa no se puede borrar)
        //no haya estados de la página en cuestión.
        //la página no sea la página que se añade cuando se inicia un drag.
        //Si cumple estas condiciones: la página puede ser borrada
        if (page != 0
                &&
                (stateListMap.get(page).value == null || stateListMap.get(page).value!!.size == 1)
                &&
                page <= nPages) {
            if (page == nPages-1) {
                //Si la página que se ha quedado vacía es la última, no añadimos una nueva página
                //para el drag.
                nPages--
                Helper.putInSharedPreferences(context.get()!!.packageName,
                        "nPages", nPages.toString(), context.get()!!.applicationContext)
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
            nPages--
            Helper.putInSharedPreferences(context.get()!!.packageName,
                    "nPages", nPages.toString(), context.get()!!.applicationContext)
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
            reallocatePages(pageDeleted, nPages)
        }
    }

    fun clean() {
        context.clear()
        instance = null
    }

    companion object {
        private var instance: Repository? = null
        fun newInstance(ctx: Context): Repository? {
            if (instance == null) {
                instance = Repository(ctx)
            }
            return instance
        }
    }
}