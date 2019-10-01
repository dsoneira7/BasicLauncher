package com.example.basiclauncher

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder.createSource
import android.graphics.ImageDecoder.decodeBitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.basiclauncher.fragments.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main.*


const val MAIN_FRAGMENT_TAG = "mainFragment"
const val SMALL_MAIN_FRAGMENT_TAG = "smallMainFragment"
const val SMALLER_MAIN_FRAGMENT_TAG = "smallerMainFragmentTag"
const val UNNINSTALL_AND_CANCEL_FRAGMENT_TAG = "utilFragment"
const val SETTINGS_FRAGMENT_TAG = "settingsFragment"
const val WIDGET_PICK_FRAGMENT_TAG = "widgetPickFragment"
const val SHORTCUTS_BAR_FRAGMENT_TAG = "shortcutsBarFragment"

const val OPEN_APP_DRAWER = 0
const val ON_ICON_ATTACHED = 1
const val ON_MAIN_MENU_HOLD = 2
const val ON_ANULATE = 3
const val ON_UNNINSTALL = 4
const val ON_EMPTY_CLICK = 5
const val ON_WALLPAPER_CLICK = 6
const val ON_SETTINGS_CLICK = 7
const val ON_WIDGET_CLICK = 8
const val PLUS_ONE_PAGE = 9
const val ON_GRID_CLICK_FROM_SMALLER_MODE = 10

const val GALLERY_REQUEST_CODE = 11
const val SETTINGS_ACTIVITY_CODE = 12

const val FRAGMENT_HIDE = 13
const val FRAGMENT_REMOVE = 14
const val FRAGMENT_SHOW = 15
const val FRAGMENT_ADD = 16
const val FRAGMENT_REPLACE = 17

const val MAXIMUM_WIDTH = 720
const val MAXIMUM_HEIGHT = 1280

//todo: Posible bug: Desinstalamos app con accesos directos en página que se debería borrar ao non ter nada. Capturar intent do sistema de desintalación dunha app.
//https://guides.codepath.com/android/viewpager-with-fragmentpageradapter
//todo: Capturar llamada del sistema de desinstalación de apk para borrar paginas que no tengan nada
//todo: Mantener grandes cantidades de datos en tabla aparte con clave foranea hacia una primera
//todo: databinding
//todo: Igual es mejor guardar objeto grande serializado en vez de estado de cada no de los states
//todo: Cambiar o destroy dos fragments para que se oculten en segundo plano, ejecuten as tareas e mostralos cando sexa necesario.
//todo: ON_GRID_CLICK
//todo: Volver a importar regla de sonar sobre codigo comentado

class MainActivity : AppCompatActivity(),
        MainFragment.OnMainFragmentInteractionListener,
        AppDrawerFragment.OnFragmentInteractionListener,
        UnninstallAndCancelFragment.OnUnninstallAndCancelFragmentInteractionListener,
        SettingsFragment.OnSettingsFragmentInteractionListener,
        WidgetPickFragment.OnWidgetPickFragmentInteractionListener,
        ShortcutsBarFragment.OnShortcutsBarFragmentInteractionListener {

    private var iconsPerRow: Int = 0
    private var iconsPerColumn = 0

    private var iconWidth = 0
    private var smallIconWidth: Float = 0f
    private var smallerIconWidth: Float = 0f

    private var iconHeight = 0
    private var smallIconHeight = 0
    private var smallerIconHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        setContentView(R.layout.activity_main)
        Repository.newInstance(applicationContext)!! //Inicializamos la clase repository
        //window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()
        iconSizeDataInitialize()

        val bitmap = Helper.getBackground(applicationContext)
        if (bitmap != null) {
            window.setBackgroundDrawable(BitmapDrawable(resources, bitmap))
        }

        fragmentOperation(FRAGMENT_ADD, R.id.container_main_fragment, MAIN_FRAGMENT_TAG, null)
        fragmentOperation(FRAGMENT_REPLACE, R.id.app_drawer_container, SHORTCUTS_BAR_FRAGMENT_TAG, null)
    }


    private fun setCustomBackground(uri: Uri) {
        var bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeBitmap(createSource(contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        }
        bitmap = rescaleBitmap(bitmap)
        Runnable { Helper.setNewBackground(applicationContext, bitmap) }.run()
        window.setBackgroundDrawable(BitmapDrawable(resources, bitmap))
    }

    private fun iconSizeDataInitialize() {
        val sharedPref = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
        iconsPerRow = sharedPref.getString("dropdown_size", "0")!!.toInt()
        if (iconsPerRow == 0) {
            sharedPref.edit().putString("dropdown_size", "4").apply()
            iconsPerRow = 4
        }


        smallIconWidth = (getScreenMetrics().widthPixels - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 61f, resources.displayMetrics)) / iconsPerRow //todo: cambiar isto
        smallerIconWidth = (getScreenMetrics().widthPixels - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 157f, resources.displayMetrics)) / iconsPerRow
        iconWidth = getScreenMetrics().widthPixels / iconsPerRow

        val mainFragmentHeight = getScreenMetrics().heightPixels -
                resources.getDimension(R.dimen.tablayout_height).toInt() -
                resources.getDimension(R.dimen.app_drawer_container_height).toInt() -
                getDimen("status_bar_height")
        var nearestDown = 0
        var nearestUp = 0
        var nearestNumberOfRowsDown = 0
        var nearestNumberOfRowsUp = 0
        for (i in 1..20) {
            if (((mainFragmentHeight / i) - iconWidth) > 0) {
                nearestUp = mainFragmentHeight / i
                nearestNumberOfRowsUp = i
            } else {
                nearestDown = mainFragmentHeight / i
                nearestNumberOfRowsDown = i
                break
            }
        }

        iconsPerColumn = if ((nearestUp - iconWidth) < (iconWidth - nearestDown)) {
            nearestNumberOfRowsUp
        } else {
            nearestNumberOfRowsDown
        }
        iconHeight = mainFragmentHeight / iconsPerColumn
        smallIconHeight = (mainFragmentHeight
                - resources.getDimension(R.dimen.tablayout_height).toInt()
                - resources.getDimension(R.dimen.container_small_margin_bottom).toInt()
                - resources.getDimension(R.dimen.container_small_margin_top).toInt()) / iconsPerColumn
        smallerIconHeight = (mainFragmentHeight
                - resources.getDimension(R.dimen.tablayout_height).toInt()
                - resources.getDimension(R.dimen.container_smaller_margin_top).toInt()
                - resources.getDimension(R.dimen.container_smaller_margin_bottom).toInt()) / iconsPerColumn
    }

    override fun onBackPressed() {
        val appDrawerFragment = supportFragmentManager.findFragmentByTag(APP_DRAWER_FRAGMENT_TAG)
        val settingsFragment = supportFragmentManager.findFragmentByTag(SETTINGS_FRAGMENT_TAG)
        if ((appDrawerFragment != null) && !(appDrawerFragment as AppDrawerFragment).isHidden) {
            fragmentOperation(FRAGMENT_HIDE, 0, APP_DRAWER_FRAGMENT_TAG, null)
            fragmentOperation(FRAGMENT_SHOW, R.id.container_main_fragment, MAIN_FRAGMENT_TAG, null)
            fragmentOperation(FRAGMENT_SHOW, R.id.app_drawer_container, SHORTCUTS_BAR_FRAGMENT_TAG, null)
        } else if ((settingsFragment != null) && !(settingsFragment as SettingsFragment).isHidden) {
            fragmentOperation(FRAGMENT_REMOVE, 0, SETTINGS_FRAGMENT_TAG, null)
            fragmentOperation(FRAGMENT_REMOVE, 0, SMALLER_MAIN_FRAGMENT_TAG, null)
            fragmentOperation(FRAGMENT_ADD, R.id.container_main_fragment, MAIN_FRAGMENT_TAG, null)
            fragmentOperation(FRAGMENT_SHOW, R.id.app_drawer_container, SHORTCUTS_BAR_FRAGMENT_TAG, null)
        } else if (pager.currentItem != 0) {
            pager.currentItem = pager.currentItem - 1
        }
    }

    fun rescaleBitmap(bitmap: Bitmap): Bitmap {
        var imageWidth = bitmap.width
        var imageHeight = bitmap.height
        while (true) {
            if (imageWidth > MAXIMUM_WIDTH && imageHeight > MAXIMUM_HEIGHT) {
                imageWidth = (imageWidth.toDouble() / 1.5).toInt()
                imageHeight = (imageHeight.toDouble() / 1.5).toInt()
            } else {
                return Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, true)
            }
        }
    }

    override fun onMainFragmentInteraction(event: Int) { //todo: buscar una mejor manera de manejar constantes
        when (event) {
            ON_ICON_ATTACHED -> {
                fragmentOperation(FRAGMENT_REMOVE, 0, SMALL_MAIN_FRAGMENT_TAG, null) //todo: Necesario esconderlo?
                fragmentOperation(FRAGMENT_HIDE, 0, UNNINSTALL_AND_CANCEL_FRAGMENT_TAG, null)
                fragmentOperation(FRAGMENT_REMOVE, 0, MAIN_FRAGMENT_TAG, null)
                fragmentOperation(FRAGMENT_HIDE, 0, APP_DRAWER_FRAGMENT_TAG, null)
                fragmentOperation(FRAGMENT_ADD, R.id.container_main_fragment, MAIN_FRAGMENT_TAG, null)
                fragmentOperation(FRAGMENT_SHOW, R.id.app_drawer_container, SHORTCUTS_BAR_FRAGMENT_TAG, null)
            }
            ON_MAIN_MENU_HOLD -> {
                fragmentOperation(FRAGMENT_HIDE, 0, MAIN_FRAGMENT_TAG, null)
                fragmentOperation(FRAGMENT_ADD, R.id.container_main_fragment_small, SMALL_MAIN_FRAGMENT_TAG, null)
                linearLayout.bringToFront()
                fragmentOperation(FRAGMENT_SHOW, R.id.linearLayout, UNNINSTALL_AND_CANCEL_FRAGMENT_TAG, null)
            }
            ON_EMPTY_CLICK -> {
                fragmentOperation(FRAGMENT_HIDE, 0, MAIN_FRAGMENT_TAG, null)
                fragmentOperation(FRAGMENT_REPLACE, R.id.container_main_fragment_smaller, SMALLER_MAIN_FRAGMENT_TAG, null)
                fragmentOperation(FRAGMENT_HIDE, 0, SHORTCUTS_BAR_FRAGMENT_TAG, null)
                fragmentOperation(FRAGMENT_REPLACE, R.id.container_settings, SETTINGS_FRAGMENT_TAG, null)
            }
            else -> onBackPressed()
        }
    }

    override fun onFragmentInteraction() {
        fragmentOperation(FRAGMENT_HIDE, 0, APP_DRAWER_FRAGMENT_TAG, null)
        fragmentOperation(FRAGMENT_SHOW, R.id.app_drawer_container, SHORTCUTS_BAR_FRAGMENT_TAG, null)
        fragmentOperation(FRAGMENT_REMOVE, 0, MAIN_FRAGMENT_TAG, null)
        linearLayout.bringToFront()
        fragmentOperation(FRAGMENT_SHOW, R.id.linearLayout, UNNINSTALL_AND_CANCEL_FRAGMENT_TAG, null)
        fragmentOperation(FRAGMENT_ADD, R.id.container_main_fragment_small, SMALL_MAIN_FRAGMENT_TAG, null)
    }

    override fun onUnninstallAndCancelFragmentInteraction(event: Int, app: String) {
        when (event) {

            ON_ANULATE -> {
                Log.d("debug", "Acción anulada")
            }

            ON_UNNINSTALL -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val intent = Intent(this, this.javaClass)
                    val sender = PendingIntent.getActivity(this, 0, intent, 0)
                    packageManager.packageInstaller.uninstall(app, sender.intentSender)
                } else {
                    val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$app"))
                    startActivity(intent)
                }
            }

        }
        onMainFragmentInteraction(ON_ICON_ATTACHED)
    }

    override fun onSettingsFragmentInteraction(event: Int) {
        when (event) {
            ON_WALLPAPER_CLICK -> {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                //intent.addCategory(Intent.CATEGORY_OPENABLE)
                val chooser = Intent.createChooser(intent, "Completar acción con: ")
                if (intent.resolveActivity(packageManager) != null) {
                    startActivityForResult(chooser, GALLERY_REQUEST_CODE)
                }
            }
            ON_SETTINGS_CLICK -> {
                startActivityForResult(Intent(this, SettingsActivity::class.java), SETTINGS_ACTIVITY_CODE)
            }
            ON_WIDGET_CLICK -> {
                Toast.makeText(this, "Not implemented", Toast.LENGTH_LONG).show()
                /*supportFragmentManager.beginTransaction()
                        .remove(supportFragmentManager.findFragmentByTag(SMALLER_MAIN_FRAGMENT_TAG)!!)
                        .remove(supportFragmentManager.findFragmentByTag(SETTINGS_FRAGMENT_TAG)!!)
                        .add(
                                R.appId.constraintlayout,
                                WidgetPickFragment.newInstance(getScreenMetrics().widthPixels),
                                WIDGET_PICK_FRAGMENT_TAG)
                        .commit()*/
                /*val mAppWidgetHost = AppWidgetHost(this, 1024)
                val appWidgetId = mAppWidgetHost.allocateAppWidgetId()
                val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
                pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                addEmptyData(pickIntent)
                startActivityForResult(pickIntent, 32)*/
            }
            else -> {
                Log.d("debug", "Operation code not supported")
            }
        }
    }

    override fun onShortcutsBarFragmentInteraction(event: Int) {
        when (event) {
            OPEN_APP_DRAWER -> {
                if (supportFragmentManager.findFragmentByTag(APP_DRAWER_FRAGMENT_TAG) == null) {
                    container_fragment_app_drawer.layoutParams = ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            ConstraintLayout.LayoutParams.MATCH_PARENT
                    )
                }
                container_fragment_app_drawer.bringToFront()
                fragmentOperation(FRAGMENT_HIDE, 0, MAIN_FRAGMENT_TAG, null)
                fragmentOperation(FRAGMENT_HIDE, 0, SHORTCUTS_BAR_FRAGMENT_TAG, null)
                fragmentOperation(FRAGMENT_SHOW, R.id.container_fragment_app_drawer, APP_DRAWER_FRAGMENT_TAG, null)
            }
            else -> onMainFragmentInteraction(event)
        }
    }

    override fun onWidgetPickFragmentInteraction() {
        fragmentOperation(FRAGMENT_REMOVE, 0, WIDGET_PICK_FRAGMENT_TAG, null)
        fragmentOperation(FRAGMENT_SHOW, R.id.container_main_fragment_small, SMALL_MAIN_FRAGMENT_TAG, null)
        linearLayout.bringToFront()
        fragmentOperation(FRAGMENT_SHOW, R.id.linearLayout, UNNINSTALL_AND_CANCEL_FRAGMENT_TAG, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    setCustomBackground(data!!.data!!)
                    fragmentOperation(FRAGMENT_REMOVE, 0, SETTINGS_FRAGMENT_TAG, null)
                    fragmentOperation(FRAGMENT_REMOVE, 0, SMALLER_MAIN_FRAGMENT_TAG, null)
                    fragmentOperation(FRAGMENT_SHOW, R.id.container_main_fragment, MAIN_FRAGMENT_TAG, null)
                    fragmentOperation(FRAGMENT_SHOW, R.id.app_drawer_container, SHORTCUTS_BAR_FRAGMENT_TAG, null)
                }
                SETTINGS_ACTIVITY_CODE -> {
                    val sharedPref = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
                    if (iconsPerRow != sharedPref.getString("dropdown_size", "0")!!.toInt()) {
                        this.recreate()
                    }
                }
            }
        }
    }

    private fun getScreenMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        return metrics
    }

    fun getDimen(name: String): Int {
        var result = 0
        val resourceId = resources.getIdentifier(name, "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun fragmentOperation(operation: Int, containerId: Int, tag: String, animation: Int?) {
        val ft = supportFragmentManager.beginTransaction()

        when (operation) {

            FRAGMENT_ADD -> {
                val fragment = selectFragmentByTag(tag)
                ft.add(containerId, fragment, tag)
            }

            FRAGMENT_REPLACE -> {
                val fragment = selectFragmentByTag(tag)
                ft.replace(containerId, fragment, tag)
            }

            FRAGMENT_SHOW -> {
                val fragment = supportFragmentManager.findFragmentByTag(tag)
                if (fragment != null) {
                    ft.show(fragment)
                } else {
                    fragmentOperation(FRAGMENT_REPLACE, containerId, tag, animation)
                }
            }

            FRAGMENT_HIDE -> {
                val fragment = supportFragmentManager.findFragmentByTag(tag)
                if(fragment != null){
                    ft.hide(fragment!!)
                }
            }

            FRAGMENT_REMOVE -> {
                val fragment = supportFragmentManager.findFragmentByTag(tag)
                if (fragment != null) {
                    ft.remove(fragment)
                    fragment.onDestroy()
                }
            }
        }

        ft.commit()
    }

    fun selectFragmentByTag(tag: String): Fragment {
        return when (tag) {
            MAIN_FRAGMENT_TAG -> MainFragment.newInstance(iconWidth, iconHeight, iconsPerRow, iconsPerColumn)
            SMALL_MAIN_FRAGMENT_TAG -> MainFragment.newInstance(smallIconWidth.toInt(), smallIconHeight, iconsPerRow, iconsPerColumn)
            SMALLER_MAIN_FRAGMENT_TAG -> MainFragment.newInstance(smallerIconWidth.toInt(), smallerIconHeight, iconsPerRow, iconsPerColumn)
            APP_DRAWER_FRAGMENT_TAG -> AppDrawerFragment.newInstance()
            UNNINSTALL_AND_CANCEL_FRAGMENT_TAG -> UnninstallAndCancelFragment.newInstance()
            SETTINGS_FRAGMENT_TAG -> SettingsFragment.newInstance()
            SHORTCUTS_BAR_FRAGMENT_TAG -> ShortcutsBarFragment.newInstance()
            else -> WidgetPickFragment.newInstance(getScreenMetrics().widthPixels)
        }
    }
}