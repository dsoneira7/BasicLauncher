package com.example.basiclauncher

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

/**
 * Este objeto es equivalente a un singleton en java. Es un Helper utilizado para ejecutar funciones
 * recurrentes en la app
 */
object Helper {
    //Nombre del archivo que contendrá el fondo serializado
    val backgroundName = "background.png"

    /**
     * Devuelve el icono de una aplicación
     *
     * @param context: Contexto de la aplicación
     * @param packageName: Nombre del paquete de la aplicación
     *
     * @return [Bitmap] del icono en resolución 96x96
     */
    fun getActivityIcon(context: Context, packageName: String): Bitmap {
        val activityName = getLauncherActivityName(context, packageName)
        val intent = Intent()
        intent.component = ComponentName(packageName, activityName)
        val icon = context.packageManager.resolveActivity(intent, 0)!!.loadIcon(context.packageManager)
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        icon.setBounds(0, 0, canvas.width, canvas.height)
        icon.draw(canvas)
        return bitmap
    }

    /**
     * Devuelve el nombre a mostrar de una aplicación
     *
     * @param context: Contexto de la aplicación
     * @param packageName: Nombre del paquete de la aplicación
     *
     * @return Nombre que muestra por defecto la aplicación en formato [String]
     */
    fun getAppName(context: Context, packageName: String): String {
        val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
        return context.packageManager.getApplicationLabel(appInfo) as String
    }

    /**
     * Devuelve el nombre de la actividad launcher de un paquete
     *
     * @param context: Contexto de la aplicación
     * @param packageName: Nombre del paquete de la aplicación
     *
     * @return Nombre de la actividad launcher en formato [String]
     */
    private fun getLauncherActivityName(context: Context, packageName: String): String {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        val activityList = context.packageManager.queryIntentActivities(intent!!, 0)
        return activityList[0].activityInfo.name
    }

    /**
     * Serializa un nuevo fondo y lo guarda en un archivo.
     *
     * @param context: Contexto de la aplicación
     * @param bitmap: Fondo de la aplicación en formato [Bitmap]
     */
    fun setNewBackground(context: Context, bitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val bytes = baos.toByteArray()

        val outputStream = context.openFileOutput(backgroundName, Context.MODE_PRIVATE)
        outputStream.write(bytes)
        outputStream.close()
    }

    /**
     * Deserializa y devuelve el fondo guardado en el dispositivo
     *
     * @param context: Contexto de la aplicación
     *
     * @return [Bitmap] del fondo o null si no se encuentra
     */
    fun getBackground(context: Context): Bitmap? {
        val file = File(context.filesDir.path + "/" + backgroundName)
        return if (file.exists()) {
            val inputStream = FileInputStream(context.filesDir.path + "/" + backgroundName)
            BitmapFactory.decodeStream(inputStream)
        } else {
            null
        }
    }

    /**
     * Borra el fondo serializado y guardado en el teléfono si existiese
     *
     * @param context: Contexto de la aplicación
     */
    fun deleteBackgroundData(context: Context) {
        val file = File(context.filesDir.path + "/" + backgroundName)
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * Método utilizado anteriormente para devolver el ancho y el largo que va a ocupar un widget en
     * pantalla en función del ancho y largo de cada una de las celdas.
     *
     * @param item: [AppWidgetProviderInfo] del widget que se va a enlazar.
     * @param context: Contexto de la aplicación.
     * @param screenWidth: Ancho de la pantalla en píxeles.
     *
     * @return [Array] que contiene el número de celdas de ancho por el número de celdas de largo
     *         que ocupará el widget en cuestión.
     */
    fun getGridSizeOfWidget(item: AppWidgetProviderInfo, context: Context, screenWidth: Int): Array<Int> {
        val nIconsPerRow = context
                .getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
                .getString("dropdown_size", "0")!!
                .toInt()
        val unitSize = screenWidth / nIconsPerRow
        var widgetWidthInCells = (item.minWidth / unitSize) + 1
        val widgetHeightInCells = (item.minHeight / unitSize) + 1
        if (widgetWidthInCells > nIconsPerRow) {
            widgetWidthInCells = nIconsPerRow
        }
        return arrayOf(widgetHeightInCells, widgetWidthInCells)
    }

    /**
     * Accede a las SharedPreferences para obtener un dato.
     *
     * @param preferencesName: Nombre de las SharedPreferences.
     * @param key: Clave del dato que queremos recuperar.
     * @param defaultValue: Valor por defecto que tendrá el dato si hay algún problema al recuperarlo
     * @param context: Contexto de la aplicación.
     *
     * @return Devuelve el dato obtenido en formato [String]
     */
    fun getFromSharedPreferences(preferencesName: String?, key: String, defaultValue: String, context: Context): String? {
        val sharedPref = context.getSharedPreferences(preferencesName + "_preferences", Context.MODE_PRIVATE)
        return sharedPref.getString(key, defaultValue)
    }

    /**
     * Guarda un dato en SharedPreferences.
     *
     * @param preferencesName: Nombre de las SharedPreferences.
     * @param key: Clave del dato que queremos guardar.
     * @param value: Valor del dato que queremos guardar
     * @param context: Contexto de la aplicación.
     */
    fun putInSharedPreferences(preferencesName: String?, key: String, value: String, context: Context) {
        val sharedPref = context.getSharedPreferences(preferencesName + "_preferences", Context.MODE_PRIVATE)
        sharedPref.edit().putString(key, value).apply()
    }

}