package com.example.basiclauncher.viewmodels

import android.app.Application
import android.util.Log
import android.util.SparseArray
import androidx.core.util.valueIterator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.basiclauncher.Repository
import com.example.basiclauncher.classes.AppIcon
import com.example.basiclauncher.classes.CustomLinearLayoutState

class ScreenSlidePagerViewModel(app: Application, val page: Int) : AndroidViewModel(app) {
    var stateList: LiveData<Array<CustomLinearLayoutState>>
    private var repository = Repository.newInstance(app.applicationContext)
    var appList : MutableLiveData<SparseArray<AppIcon>>
    var version: Long
    /*private val observer = Observer<Array<CustomLinearLayoutState>> {
        Thread {
            for (i in it) {
                if (appList.get(i.position) == null || appList.get(i.position).id != i.appId) {
                    appList.put(i.position, repository!!.getAppById(i.appId))
                }
            }
        }.start()
    }*/

    init {
        version = repository!!.version
        stateList = repository!!.getStateListByPage(page)
        appList = repository!!.getAppListByPage(page)
        stateList.observeForever{
            for(i in it){
                Log.d("ScreenSlidePagerViewMod", "Updated value of state: page: " + i.page + " " + i.position)
            }
        }
        appList.observeForever{
            for(i in it.valueIterator()){
                Log.d("qerty2", i.packageName )
            }
        }
    /*    if(stateList.value != null) {
            Thread{
                for (i in stateList.value!!.iterator()) {
                    if (appList.get(i.position) == null || appList.get(i.position).id != i.appId) {
                        appList.put(i.position, repository!!.getAppById(i.appId))
                    }
                }
            }.start()
        }
        stateList.observeForever(observer)
    */}

    fun update(obj: CustomLinearLayoutState) {
        repository!!.updateState(obj)

    }

    fun stateOccupied(packageName: String, page: Int, position: Int) {
        val appId = repository!!.stateOccupied(packageName, page, position)
    }

    fun emptyState(page: Int, position: Int) {
        repository!!.deleteItem(page, position)
    }


}
