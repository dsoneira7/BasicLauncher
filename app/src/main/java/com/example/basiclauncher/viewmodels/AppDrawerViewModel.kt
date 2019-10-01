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

//todo: Valorar que la bbdd devuelva una lista
class AppDrawerViewModel(val app: Application) : AndroidViewModel(app) {
    var appLiveData: LiveData<SparseArray<AppIcon>>
    val appArray = MutableLiveData<ArrayList<AppIcon>>()
    private var repository = Repository.newInstance(app.applicationContext)



    private val observer = Observer<SparseArray<AppIcon>> {
        if (it.size() > appArray.value!!.size) {
            for (i in it.valueIterator()) {
                if (!appArray.value!!.contains(i)) {
                    appArray.postValue(
                            appArray.value!!.apply {
                                this.add(i)
                            }
                    )
                }
            }
        } else {
            var appIcon: AppIcon? = null
            for (i in appArray.value!!) {
                if (!it.containsKey(i.id)) {
                    appIcon = i
                    break
                }
            }
            if (appIcon != null) {
                appArray.postValue(
                        appArray.value!!.apply {
                            this.remove(appIcon)
                        }
                )
            }
        }
    }

    init {
        appLiveData = repository!!.getAppList()
        if (appLiveData.value != null) {
            if (appArray.value == null) {
                appArray.value = ArrayList()
            }
            for (i in appLiveData.value!!.valueIterator()) {
                if (!appArray.value!!.contains(i)) {
                    appArray.value!!.add(i)
                }
            }
        }

        appLiveData.observeForever(observer)
    }

}
