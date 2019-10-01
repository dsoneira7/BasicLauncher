package com.example.basiclauncher.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.basiclauncher.Repository
import com.example.basiclauncher.classes.CustomLinearLayoutState

class ScreenSlidePagerViewModel(app: Application, val page: Int) : AndroidViewModel(app) {
    var stateList: LiveData<Array<CustomLinearLayoutState>>
    private var repository = Repository.newInstance(app.applicationContext)
    var appList = repository!!.getAppList()

    private val stateListObserver = Observer<Array<CustomLinearLayoutState>> {
        for (i in it) {
            Log.d(this.toString(), "Updated value of state: page: " + i.page + " " + i.position)
        }
    }

    init {
        stateList = repository!!.getStateListByPage(page)
        stateList.observeForever(stateListObserver)
    }


    fun update(obj: CustomLinearLayoutState) {
        repository!!.updateState(obj)

    }

    fun stateOccupied(packageName: String, page: Int, position: Int) {
        val appId = repository!!.stateOccupied(packageName, page, position)
    }

    fun emptyState(page: Int, position: Int) {
        repository!!.deleteItem(page, position)
    }

    override fun onCleared() {
        stateList.removeObserver(stateListObserver)
        super.onCleared()
    }
}
