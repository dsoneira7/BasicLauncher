package com.example.basiclauncher.classes

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Clase que contiene la información necesaria sobre cada una de las aplicaciones
 *
 * @param packageName: Nombre del paquete de la aplicación.
 * @param appName: El nombre que se va a mostrar.
 * @param icon: Bitmap del icono que se mostrará.
 */
@Entity(tableName = "apps",
        indices = [Index(value = ["packageName"], unique = true)])
data class AppIcon(
        var packageName: String,
        var appName: String,
        var icon: Bitmap
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    //Identificador único de la aplicación autogenerado cuando se introduce en la BB.DD.
}