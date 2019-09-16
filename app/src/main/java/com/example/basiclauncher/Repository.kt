package com.example.basiclauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.util.SparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.basiclauncher.classes.AppIcon
import com.example.basiclauncher.classes.CustomLinearLayoutState
import com.example.basiclauncher.room.AbstractAppDatabase
import java.lang.ref.WeakReference
import java.util.*

//todo: estados guardados de paginas transferidas; paginas non actualizadas
class Repository private constructor(ctx: Context) {
    private var context: WeakReference<Context> = WeakReference(ctx)
    var isUpdated = false
    private var appList: LiveData<Array<AppIcon>> = MutableLiveData()
    var stateList = ArrayList<CustomLinearLayoutState>()
    var stateListMap = SparseArray<LiveData<Array<CustomLinearLayoutState>>>()
    var appListMap = SparseArray<MutableLiveData<SparseArray<AppIcon>>>()
    private val myDao = AbstractAppDatabase.getInstance(context.get()!!)?.myDao()
    private val stateDao = AbstractAppDatabase.getInstance(context.get()!!)?.stateDao()
    var nPages = 0
    var version: Long = 0

    private val getAppDrawerListThread = Thread { //todo: Cargando en memoria cada vez, innecesario
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val apps = context.get()!!.packageManager.queryIntentActivities(mainIntent, 0)
        Collections.sort(apps, ResolveInfo.DisplayNameComparator(context.get()!!.packageManager))
        Thread {
            var contador = 1
            for (i in apps) {
                if (i.activityInfo.packageName == context.get()!!.packageName) {
                    continue
                }
                myDao!!.insertApp(AppIcon(
                        i.activityInfo.packageName,
                        Helper.getAppName(context.get()!!, i.activityInfo.packageName),
                        (Helper.getActivityIcon(context.get()!!, i.activityInfo.packageName) as BitmapDrawable).bitmap,
                        contador
                ))
                contador++
            }
        }.start()

    }

    private val getStateListThread = Thread {
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
    }

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
        nPages = Helper.getFromSharedPreferences(context.get()!!.packageName,
                "nPages", "0", context.get()!!.applicationContext)!!.toInt()
        if (nPages == 0) {
            nPages = 1
        }
        getAppDrawerListThread.start()
        appList = myDao!!.getAppList()
        getStateListThread.start()
    }

    fun updateState(obj: CustomLinearLayoutState) {
        stateDao!!.insertState(obj)
    }

    fun getAppById(appId: Int): AppIcon {
        return myDao!!.getAppById(appId)
    }

    fun stateOccupied(packageName: String, page: Int, position: Int): Int {
        val appId = myDao!!.getAppIdByPackageName(packageName)

        val ok = stateDao!!.insertState(CustomLinearLayoutState(
                page,
                position,
                appId
        ))
        appListMap[page].postValue(
                appListMap[page].value.apply {
                    this!!.put(position, appList.value!!.get(appId-1))
                }
        )

        Log.d("debun", "Succesful operation $ok")
        /*nPages = Helper.getFromSharedPreferences(context.get()!!.packageName,
                "nPages", "0", context.get()!!.applicationContext)!!.toInt()
        if((page+1)>=nPages){
            stateListIterationByPage(page)
        }*/
        return appId
    }

    fun deleteItem(page: Int, position: Int) {
        stateDao!!.deleteState(CustomLinearLayoutState(page, position))
        comprobeIfPageIsDeletable(page)

    }

    fun comprobeIfPageIsDeletable(page: Int) {
        nPages = Helper.getFromSharedPreferences(context.get()!!.packageName,
                "nPages", "0", context.get()!!.applicationContext)!!.toInt()
        if (page != 0 &&
                (stateListMap.get(page).value != null
                        || stateListMap.get(page).value!!.isEmpty())) {
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

    fun getAppList(): LiveData<Array<AppIcon>> {
        return appList
    }

    fun getStateListByPage(page: Int): LiveData<Array<CustomLinearLayoutState>> {
        if (stateListMap.get(page) == null) {
            stateListMap.put(page, stateDao!!.getAllStatesByPage(page))
        }
        return stateListMap[page]
    }

    fun getAppListByPage(page: Int): MutableLiveData<SparseArray<AppIcon>> {
        if(appListMap.get(page)==null){
            appListMap.put(page, MutableLiveData())
            appListMap.get(page).value = SparseArray()
        }
        return appListMap.get(page)
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

    fun clean() { //todo!
        context.clear()
        instance = null
    }


}