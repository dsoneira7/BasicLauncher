package com.example.basiclauncher.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.example.basiclauncher.Repository
import com.example.basiclauncher.classes.AppIcon

/**
 * Subclase de [AndroidViewModel] que se corresponde con un [AppDrawerFragment]. Contiene datos
 * relativos a dicho fragmento y supone el punto de conexión con la clase [Repository].
 */
class AppDrawerViewModel(val app: Application) : AndroidViewModel(app) {
    private var repository = Repository.getInstance(app.applicationContext)
    //Este liveData es el que viene directamente de la clase Repository
    var appLiveData: LiveData<Array<AppIcon>> = repository!!.getAppListOrderedByAppName()
}
