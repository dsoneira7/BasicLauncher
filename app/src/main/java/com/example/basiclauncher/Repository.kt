package com.example.basiclauncher

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.util.SparseArray
import androidx.core.util.containsKey
import androidx.core.util.containsValue
import androidx.core.util.valueIterator
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.basiclauncher.classes.AppIcon
import com.example.basiclauncher.classes.CustomLinearLayoutState
import com.example.basiclauncher.room.AbstractAppDatabase
import java.lang.ref.WeakReference
import java.util.*


//todo: estados guardados de paginas transferidas; paginas non actualizadas
//todo: revisar nombres variables
//todo: Contemplar posibilidad de varias activities launcher para un mismo paquete
class Repository private constructor(ctx: Context) {
    private var context: WeakReference<Context> = WeakReference(ctx)
    private var appList: LiveData<Array<AppIcon>> = MutableLiveData()
    private var appSparseArrayLiveData: MutableLiveData<SparseArray<AppIcon>> = MutableLiveData()
    var stateListMap = SparseArray<LiveData<Array<CustomLinearLayoutState>>>()
    private val myDao = AbstractAppDatabase.getInstance(context.get()!!)?.myDao()
    private val stateDao = AbstractAppDatabase.getInstance(context.get()!!)?.stateDao()
    var nPages = 0
    var version: Long = 0

    private val getAppDrawerListThread = Runnable {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val apps = context.get()!!.packageManager.queryIntentActivities(mainIntent, 0)
        val packageNamesList = ArrayList<String>()
        for (i in apps) {
            packageNamesList.add(i.activityInfo.packageName)
        }
        if (apps.size - 2 < appList.value!!.size) {//-2: package duplicado y package del propio launcher
            for (i in appList.value!!) {
                if (!packageNamesList.contains(i.packageName)) {
                    Thread { myDao!!.deleteApp(myDao.getAppById(i.id)) }.start()
                    appSparseArrayLiveData.postValue(
                            appSparseArrayLiveData.value!!.apply{
                                this.delete(i.id)
                            }
                    )
                }
            }
        } else if(apps.size - 2 > appList.value!!.size) {
            var packageNameFound: Boolean
            for (i in packageNamesList) {
                packageNameFound = false
                if (i == context.get()!!.packageName) {
                    continue
                }
                for (j in appList.value!!) {
                    if (j.packageName == i) {
                        packageNameFound = true
                        break
                    }
                }
                if (!packageNameFound) {
                    Thread{
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

    fun updateAppList() {
        getAppDrawerListThread.run()
    }

    fun findAppByPackageName(packageName: String): AppIcon? {
        for (i in appList.value!!) {
            if (i.packageName == packageName) {
                return i
            }
        }
        return null
    }

    /*private val getStateListThread = Thread {
        for (i in -1 until nPages) { //page -1 corresponde a la barra de accesos directos
            stateListMap.put(i, stateDao!!.getAllStatesByPage(i))
            if(stateListMap.get(i).value != null) {
                for (state in stateListMap.get(i).value!!) {
                    if (appListMap.get(i) == null) {
                        appListMap.put(i, MutableLiveData())
                        appListMap.get(i).value = SparseArray()
                    }
                    appListMap.get(i).value!!.put(state.position, appList.value!!.get(state.appId))
                }
            }
        }
    }*/

    /*
    private fun stateListIterationByPage(page: Int){
        var stateArrayByPage = SparseArray<CustomLinearLayoutState>()
        stateArrayByPage.clear()
        stateDao!!.getAllStatesByPage(page).apply{
            for(state in this){
                stateArrayByPage.put(state.position, state)
            }
        }
        stateListMap.put(page,stateArrayByPage)
        var appArrayByPage = SparseArray<AppIcon>()
        appArrayByPage.clear()
        for(j in stateArrayByPage.valueIterator()){
            appArrayByPage.put(j.position,getAppById(j.appId))
        }
        appListMap.put(page,appArrayByPage)
    }*/

    init {
        appSparseArrayLiveData.value = SparseArray()
        appList = myDao!!.getAppList()
        nPages = Helper.getFromSharedPreferences(context.get()!!.packageName,
                "nPages", "0", context.get()!!.applicationContext)!!.toInt()
        if (nPages == 0) {
            nPages = 1
        }

        appList.observeForever {
            appSparseArrayLiveData.postValue(
                    appSparseArrayLiveData.value!!.apply {
                        if(it.size>this.size()) {
                            for (i in it) {
                                if (!this.containsKey(i.id)) {
                                    this.put(i.id, i)
                                }
                            }
                        }
                    }
            )
            Log.d("Repository", "appList has been updated")
        }

        val installEventBroadcastReceiver = InstallEventBroadcastReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addDataScheme("package")
        context.get()!!.registerReceiver(installEventBroadcastReceiver, intentFilter)
    }

    fun updateState(obj: CustomLinearLayoutState) {
        stateDao!!.insertState(obj)
        Log.d("Repository", "State updated")
    }

    fun getAppById(id: Int): AppIcon = myDao!!.getAppById(id)


    fun stateOccupied(packageName: String, page: Int, position: Int) {
        val ok = stateDao!!.insertState(CustomLinearLayoutState(
                page,
                position,
                findAppByPackageName(packageName)!!.id))

        Log.d("debug", "Succesful operation $ok")
        /*nPages = Helper.getFromSharedPreferences(context.get()!!.packageName,
                "nPages", "0", context.get()!!.applicationContext)!!.toInt()
        if((page+1)>=nPages){
            stateListIterationByPage(page)
        }*/
    }

    fun deleteItem(page: Int, position: Int) {
        stateDao!!.deleteState(CustomLinearLayoutState(page, position))
        Log.d("Repository", "item in $page $position deleted.")
        comprobeIfPageIsDeletable(page)

    }

    fun comprobeIfPageIsDeletable(page: Int) {
        nPages = Helper.getFromSharedPreferences(context.get()!!.packageName,
                "nPages", "0", context.get()!!.applicationContext)!!.toInt()
        if (page != 0 &&
                (stateListMap.get(page).value == null
                        || stateListMap.get(page).value!!.size == 1)) {
            if ((page + 1) < nPages) {
                reallocatePages(page, nPages)
            }
            nPages--
            Helper.putInSharedPreferences(context.get()!!.packageName,
                    "nPages", nPages.toString(), context.get()!!.applicationContext)
        }
    }

    fun reallocatePages(page: Int, npages: Int) {
        for (i in page until npages) {
            for (state in stateListMap.get(i).value!!.iterator()) {
                if (i != page) {
                    stateDao!!.deleteState(state)
                }
                state.page = state.page - 1
                stateDao!!.insertState(state)
            }
        }
    }

    fun getAppList(): LiveData<SparseArray<AppIcon>> {
        return appSparseArrayLiveData
    }

    fun getStateListByPage(page: Int): LiveData<Array<CustomLinearLayoutState>> {
        if (stateListMap.get(page) == null) {
            stateListMap.put(page, stateDao!!.getAllStatesByPage(page))
            Log.d("Repository", "stateList of page $page added.")
        }
        return stateListMap.get(page)
    }

    companion object {
        private var instance: Repository? = null
        fun newInstance(ctx: Context): Repository? {
            if (instance == null) {
                Log.d("Repository", "New instance of repository")
                instance = Repository(ctx)
            }
            return instance
        }
    }

    fun clean() { //todo!
        context.clear()
        instance = null
    }


}