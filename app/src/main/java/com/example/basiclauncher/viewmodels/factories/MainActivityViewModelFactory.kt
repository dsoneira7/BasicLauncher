package com.example.basiclauncher.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Clase factoría que se utiliza para generar una instancia personalizada de [ScreenSlidePagerViewModel]
 * Se necesitaba para tener un ViewModel diferente para cada item del [ViewPager] y poder pasarle
 * la página con la que se corresponde por parámetros.
 */
class MainActivityViewModelFactory(private val application: Application, private val screenWidth: Int, private val screenHeight: Int) : ViewModelProvider.AndroidViewModelFactory(application) {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java, Int::class.java, Int::class.java)
                .newInstance(application, screenWidth, screenHeight)
    }
}