package com.example.basiclauncher.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel

/**
 * Subclase de [AndroidViewModel] que se corresponde con todos lso [MainFragment] que se creen.
 * Guarda datos importantes para el fragmento y supone un punto de comunicación con otras clases.
 * Se utiliza para mantener la current page.
 */
class MainFragmentViewModel(val app: Application) : AndroidViewModel(app) {

    var page: Int = 0
    var iconsPerRow: Int = 0
    var iconsPerColumn = 0

    var iconWidth = 0
    var smallIconWidth: Float = 0f
    var smallerIconWidth: Float = 0f

    var iconHeight = 0
    var smallIconHeight = 0
    var smallerIconHeight = 0
}