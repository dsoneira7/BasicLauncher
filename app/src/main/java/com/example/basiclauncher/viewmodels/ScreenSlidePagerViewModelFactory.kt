package com.example.basiclauncher.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.reflect.InvocationTargetException

class ScreenSlidePagerViewModelFactory(private val application: Application, private val page: Int) : ViewModelProvider.AndroidViewModelFactory(application) {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        try {
            return modelClass.getConstructor(Application::class.java, Int::class.java)
                    .newInstance(application, page)
        }catch(e : InvocationTargetException){
            e.targetException.printStackTrace()
        }
        return modelClass.getConstructor(Application::class.java, Int::class.java)
                .newInstance(application, page)
    }
}