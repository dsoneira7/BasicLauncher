package com.example.basiclauncher

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import java.io.*

object Helper {
    val backgroundName = "background.png"

    fun getActivityIcon(context: Context, packageName: String): Bitmap {
        val activityName = getLauncherActivityName(context, packageName)
        val intent = Intent()
        intent.component = ComponentName(packageName, activityName)
        val icon = context.packageManager.resolveActivity(intent, 0).loadIcon(context.packageManager)
        val bitmap = Bitmap.createBitmap(icon.intrinsicWidth, icon.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        icon.setBounds(0,0,canvas.width, canvas.height)
        icon.draw(canvas)
        return bitmap
    }

    fun getAppName(context: Context, packageName: String): String{
        var appInfo = context.packageManager.getApplicationInfo(packageName, 0)
        return context.packageManager.getApplicationLabel(appInfo) as String
    }

    fun getLauncherActivityName(context: Context, packageName: String): String {
        var activityName = ""
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        val activityList = context.packageManager.queryIntentActivities(intent, 0) //todo: Igual es una carga demasiado alta hacer una query por cada aplicación
        if (activityList != null) {
            activityName = activityList.get(0).activityInfo.name
        }
        return activityName
    }

    fun setNewBackground(context: Context, bitmap: Bitmap){
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val bytes = baos.toByteArray()

        val outputStream = context.openFileOutput(backgroundName, Context.MODE_PRIVATE)
        outputStream.write(bytes)
        outputStream.close()
    }

    fun getBackground(context: Context): Bitmap?{
        val file = File(context.filesDir.path+"/"+backgroundName)
        return if(file.exists()) {
            val inputStream = FileInputStream(context.filesDir.path + "/" + backgroundName)
            BitmapFactory.decodeStream(inputStream)
        }
        else{
            null
        }
    }

    fun getGridSizeOfWidget(item: AppWidgetProviderInfo, context: Context, screen_width: Int): Array<Int>{
        val nIconsPerRow = context
                .getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
                .getString("dropdown_size", "0")!!
                .toInt()
        val unitSize = screen_width/nIconsPerRow
        //val widgetWidthInPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, item.minWidth.toFloat(),  context.resources.displayMetrics).toInt()
        //val widgetHeightInPix = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, item.minHeight.toFloat(),  context.resources.displayMetrics).toInt()
        var widgetWidthInCells = (item.minWidth/*widgetWidthInPix*//unitSize) +1
        val widgetHeightInCells = (item.minHeight/*widgetHeightInPix*//unitSize) +1
        if(widgetWidthInCells > nIconsPerRow) {
            widgetWidthInCells = nIconsPerRow
        }
        return arrayOf(widgetHeightInCells, widgetWidthInCells)
    }

    fun getFromSharedPreferences(preferencesName: String?, key: String, defaultValue: String, context:Context): String? {
        val sharedPref = context.getSharedPreferences(preferencesName + "_preferences", Context.MODE_PRIVATE)
        return sharedPref.getString(key, defaultValue)
    }

    fun putInSharedPreferences(preferencesName: String?, key: String, value: String, context: Context) {
        val sharedPref = context.getSharedPreferences(preferencesName + "_preferences", Context.MODE_PRIVATE)
        sharedPref.edit().putString(key, value).apply()
    }

}