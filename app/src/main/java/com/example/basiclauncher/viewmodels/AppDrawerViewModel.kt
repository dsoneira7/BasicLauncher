package com.example.basiclauncher.viewmodels

import android.app.Application
import android.content.Intent
import android.content.pm.ResolveInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.basiclauncher.Helper
import com.example.basiclauncher.Repository
import com.example.basiclauncher.classes.AppIcon
import java.util.*

class AppDrawerViewModel(val app: Application) : AndroidViewModel(app) {
    //var version : Long
    var appArray : LiveData<Array<AppIcon>>
    private var repository = Repository.newInstance(app.applicationContext)

    init{
        appArray = repository!!.getAppList()
        //version = repository!!.version
    }



}
