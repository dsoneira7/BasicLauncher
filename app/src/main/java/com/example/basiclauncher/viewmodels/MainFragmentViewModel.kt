package com.example.basiclauncher.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class MainFragmentViewModel(val app: Application) : AndroidViewModel(app) {

    var page : Int = 0
    var iconsPerRow: Int = 0
    var iconsPerColumn = 0

    var iconWidth = 0
    var smallIconWidth: Float = 0f
    var smallerIconWidth: Float = 0f

    var iconHeight = 0
    var smallIconHeight = 0
    var smallerIconHeight = 0
}