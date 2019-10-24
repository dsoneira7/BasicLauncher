package com.example.basiclauncher.viewmodels

import android.app.Application
import android.util.SparseArray
import androidx.core.util.containsKey
import androidx.core.util.valueIterator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.basiclauncher.Repository
import com.example.basiclauncher.classes.AppIcon
import java.util.*

/**
 * Subclase de [AndroidViewModel] que se corresponde con un [AppDrawerFragment]. Contiene datos
 * relativos a dicho fragmento y supone el punto de conexión con la clase [Repository].
 */
class AppDrawerViewModel(val app: Application) : AndroidViewModel(app) {
    //Este liveData es el que viene directamente de la clase Repository
    var appLiveData: LiveData<SparseArray<AppIcon>>
    //Este liveData se utiliza para mantener ordenados alfabéticamente los datos, es el que observa
    //el AppDrawerFragment
    val orderedAppArrayLiveData = MutableLiveData<ArrayList<AppIcon>>()
    private var repository = Repository.newInstance(app.applicationContext)


    private val observer = Observer<SparseArray<AppIcon>> {
        //Cuando observamos un cambio en las apps guardadas en la BBDD:
        //Comprobamos si el tamaño de los datos nuevos es menor o mayor que el de los datos viejos
        //Si es mayor tenemos que añadir los datos nuevos
        if (it.size() > orderedAppArrayLiveData.value!!.size) {
            for (i in it.valueIterator()) {
                if (!orderedAppArrayLiveData.value!!.contains(i)) {
                    orderedAppArrayLiveData.postValue(
                            orderedAppArrayLiveData.value!!.apply {
                                this.add(i)
                            }
                    )
                }
            }
        } else {
            //Si es menor tenemos que borrar los datos que ya no deban estar
            var appIcon: AppIcon? = null
            for (i in orderedAppArrayLiveData.value!!) {
                if (!it.containsKey(i.id)) {
                    appIcon = i
                    break
                }
            }
            if (appIcon != null) {
                orderedAppArrayLiveData.postValue(
                        orderedAppArrayLiveData.value!!.apply {
                            this.remove(appIcon)
                        }
                )
            }
        }
    }

    init {
        appLiveData = repository!!.getAppList()
        if (appLiveData.value != null) {
            if (orderedAppArrayLiveData.value == null) { //Inicializamos el liveData de ser necesario
                orderedAppArrayLiveData.value = ArrayList()
            }
            //Introducimos los datos del liveData "raw" en el otro
            for (i in appLiveData.value!!.valueIterator()) {
                if (!orderedAppArrayLiveData.value!!.contains(i)) {
                    orderedAppArrayLiveData.value!!.add(i)
                }
            }
        }

        appLiveData.observeForever(observer)
    }

}
