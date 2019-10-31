package com.example.basiclauncher.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.basiclauncher.Repository
import com.example.basiclauncher.classes.CustomLinearLayoutState

/**
 * Subclase de [AndroidViewModel] que se corresponde con cada instancia de [ScreenSlidePagerFragment]
 * Es el punto de conexión entre el fragmento y la clase [Repository]. Guarda datos relativos a dicho
 * fragmento.
 */
class ScreenSlidePagerViewModel(val app: Application, val page: Int) : AndroidViewModel(app) {
    //Contiene la lista de estados de la página correspondiente según la pagina
    private var repository = Repository.getInstance(app.applicationContext)
    //Contiene la lista de apps instaladas. Cada estado tiene una referencia a una app de esta lista
    var appList = repository!!.getAppList()
    var stateList: LiveData<Array<CustomLinearLayoutState>>  = repository!!.getStateListByPage(page)
    var numberOfPages : LiveData<Int> = repository!!.nPagesLiveData
    /**
     * Actualiza el estado de una celda en la BBDD
     */
    fun stateOccupied(packageName: String, page: Int, position: Int) {
        repository!!.stateOccupied(packageName, page, position)
        if(page+1 > numberOfPages.value!!){
            updateNumberOfPages(page+1)
        }
    }

    /**
     * Borra un estado de una celda de la BBDD.
     */
    fun emptyState(page: Int, position: Int) {
        repository!!.deleteItem(page, position)
    }

    fun updateNumberOfPages(nPages: Int){
        repository!!.updateNumberOfPages(nPages)
    }
}
