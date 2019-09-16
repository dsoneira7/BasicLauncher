package com.example.basiclauncher

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.basiclauncher.fragments.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Parcelable
import android.widget.Toast


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

const val GALLERY_REQUEST_CODE = 10
const val SETTINGS_ACTIVITY_CODE = 11

const val MAXIMUM_WIDTH = 720
const val MAXIMUM_HEIGHT = 1280

//todo: Posible bug: Desinstalamos app con accesos directos en página que se debería borrar ao non ter nada. Capturar intent do sistema de desintalación dunha app.
//todo: Posible bug: É posible que desaparezan todas as páxinas?
//todo: bug: Cambio de páxina en drag. Thread non aborta
//todo: Hacer que si droppeas icono en zona sin draglistener no pase nada
//https://guides.codepath.com/android/viewpager-with-fragmentpageradapter
//todo: Capturar llamada del sistema de desinstalación de apk para borrar paginas que no tengan nada
//todo: Utilizar llamadas del sistema para modificar bbdd de appdrawer
//todo: Omitir basicLauncher en appList
//todo: Mantener grandes cantidades de datos en tabla aparte con clave foranea hacia una primera
//todo: databinding
//todo: Igual es mejor guardar objeto grande serializado en vez de estado de cada no de los states
//todo: A veces non se guarda a bbdd non sei moi ben por que
//todo: Cambiar o destroy dos fragments para que se oculten en segundo plano, ejecuten as tareas e mostralos cando sexa necesario.

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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Repository.newInstance(applicationContext)!! //Inicializamos la clase repository
        supportActionBar?.hide()
        iconSizeDataInitialize()

        val bitmap = Helper.getBackground(applicationContext)
        if (bitmap != null) {
            window.setBackgroundDrawable(BitmapDrawable(resources, bitmap))
        }
        val ft = supportFragmentManager.beginTransaction()
        ft.add(R.id.container_main_fragment, MainFragment.newInstance(iconWidth, iconHeight, iconsPerRow, iconsPerColumn), MAIN_FRAGMENT_TAG)
        ft.replace(R.id.app_drawer_container, ShortcutsBarFragment.newInstance(), SHORTCUTS_BAR_FRAGMENT_TAG)
        ft.commit()
        val linearLayout = findViewById<LinearLayout>(R.id.app_drawer_container)
    }


    private fun setCustomBackground(uri: Uri) {
        Log.d("debug", "INICIO")
        var bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeBitmap(createSource(contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        }
        Log.d("debug", "MEDIO")
        bitmap = rescaleBitmap(bitmap)
        Runnable { Helper.setNewBackground(applicationContext, bitmap) }.run()
        window.setBackgroundDrawable(BitmapDrawable(resources, bitmap))
        Log.d("debug", "FINAL")
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
                + resources.getDimension(R.dimen.app_drawer_container_height).toInt()
                - resources.getDimension(R.dimen.tablayout_height).toInt()
                - resources.getDimension(R.dimen.container_smaller_margin_top).toInt()
                - resources.getDimension(R.dimen.container_smaller_margin_bottom).toInt()) / iconsPerColumn
    }

    override fun onBackPressed() {
        val appDrawerFragment = supportFragmentManager.findFragmentByTag(APP_DRAWER_FRAGMENT_TAG)
        val settingsFragment = supportFragmentManager.findFragmentByTag(SETTINGS_FRAGMENT_TAG)
        if ((appDrawerFragment != null) && !(appDrawerFragment as AppDrawerFragment).isHidden) {
            val ft = supportFragmentManager.beginTransaction()
            ft.hide(appDrawerFragment)
            ft.show(supportFragmentManager.findFragmentByTag(MAIN_FRAGMENT_TAG)!!)
            ft.show(supportFragmentManager.findFragmentByTag(SHORTCUTS_BAR_FRAGMENT_TAG)!!)
            ft.commit()

        } else if ((settingsFragment != null) && !(settingsFragment as SettingsFragment).isHidden) {
            val ft = supportFragmentManager.beginTransaction()
            ft.remove(settingsFragment)
            ft.remove(supportFragmentManager.findFragmentByTag(SMALLER_MAIN_FRAGMENT_TAG)!!)
            ft.add(R.id.container_main_fragment, MainFragment.newInstance(iconWidth, iconHeight, iconsPerRow, iconsPerColumn), MAIN_FRAGMENT_TAG)
            ft.show(supportFragmentManager.findFragmentByTag(SHORTCUTS_BAR_FRAGMENT_TAG)!!)
            ft.commit()
        } else if (pager.currentItem != 0) {
            pager.currentItem = pager.currentItem - 1
        }
    }

    fun rescaleBitmap(bitmap: Bitmap): Bitmap {
        var imageWidth = bitmap.width
        var imageHeight = bitmap.height
        while (true) {
            if (imageWidth > MAXIMUM_WIDTH && imageHeight > MAXIMUM_HEIGHT) {
                imageWidth = (imageWidth.toDouble()/1.5).toInt()
                imageHeight = (imageHeight.toDouble()/1.5).toInt()
            }
            else{
                return Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, true)
            }
        }
    }

    override fun onMainFragmentInteraction(event: Int) { //todo: buscar una mejor manera de manejar constantes

        if (event == ON_ICON_ATTACHED) {
            val ft = supportFragmentManager.beginTransaction()
            val smallMainFragment = supportFragmentManager.findFragmentByTag(SMALL_MAIN_FRAGMENT_TAG)
            ft.hide(smallMainFragment!!)
            val cancelFragment = supportFragmentManager.findFragmentByTag(UNNINSTALL_AND_CANCEL_FRAGMENT_TAG)
            ft.hide(cancelFragment!!)
            var mainFragment = supportFragmentManager.findFragmentByTag(MAIN_FRAGMENT_TAG)
            if(mainFragment!=null){
                ft.remove(mainFragment)
            }
            ft.hide(supportFragmentManager.findFragmentByTag(APP_DRAWER_FRAGMENT_TAG)!!)

                mainFragment = MainFragment.newInstance(iconWidth, iconHeight, iconsPerRow, iconsPerColumn) //todo:cambiar esto
                ft.add(R.id.container_main_fragment, mainFragment, MAIN_FRAGMENT_TAG)

            ft.show(supportFragmentManager.findFragmentByTag(SHORTCUTS_BAR_FRAGMENT_TAG)!!)
            ft.commit()

        }

        if (event == ON_MAIN_MENU_HOLD) {
            val ft = supportFragmentManager.beginTransaction()
            val mainFragment = supportFragmentManager.findFragmentByTag(MAIN_FRAGMENT_TAG)
            ft.hide(mainFragment!!)
            ft.replace(R.id.container_main_fragment_small,
                    MainFragment.newInstance(smallIconWidth.toInt(), smallIconHeight, iconsPerRow, iconsPerColumn),
                    SMALL_MAIN_FRAGMENT_TAG)
            linearLayout.bringToFront()
            val cancelFragment = supportFragmentManager.findFragmentByTag(UNNINSTALL_AND_CANCEL_FRAGMENT_TAG)
            if (cancelFragment != null) {
                ft.show(cancelFragment)
            } else {
                ft.replace(R.id.linearLayout,
                        UnninstallAndCancelFragment.newInstance(),
                        UNNINSTALL_AND_CANCEL_FRAGMENT_TAG)
            }
            ft.commitNow()
        }

        if (event == ON_EMPTY_CLICK) {
            val ft = supportFragmentManager.beginTransaction()
            ft.hide(supportFragmentManager.findFragmentByTag(MAIN_FRAGMENT_TAG)!!)
            ft.replace(R.id.container_main_fragment_smaller,
                    MainFragment.newInstance(smallerIconWidth.toInt(), smallerIconHeight, iconsPerRow, iconsPerColumn),
                    SMALLER_MAIN_FRAGMENT_TAG)
            ft.hide(supportFragmentManager.findFragmentByTag(SHORTCUTS_BAR_FRAGMENT_TAG)!!)
            ft.replace(R.id.container_settings, SettingsFragment.newInstance(), SETTINGS_FRAGMENT_TAG)
            ft.commit()
        }


        /*val ft = supportFragmentManager.beginTransaction() //todo: Quité una regla de Sonar sin querer
        val mainFragment = supportFragmentManager.findFragmentByTag(MAIN_FRAGMENT_TAG)
        ft.hide(mainFragment!!)
        ft.replace(R.id.container_main_fragment_small, MainFragment.newInstance(getScreenMetrics().widthPixels/6), MAIN_FRAGMENT_TAG)
        ft.commit()*/
    }

    override fun onFragmentInteraction() {
        val ft = supportFragmentManager.beginTransaction()
        val appDrawerFragment = supportFragmentManager.findFragmentByTag(APP_DRAWER_FRAGMENT_TAG)
        if (appDrawerFragment != null) {
            ft.hide(appDrawerFragment)
        }
        ft.show(supportFragmentManager.findFragmentByTag(SHORTCUTS_BAR_FRAGMENT_TAG)!!)
        ft.remove(supportFragmentManager.findFragmentByTag(MAIN_FRAGMENT_TAG)!!)
        linearLayout.bringToFront()
        val cancelFragment = supportFragmentManager.findFragmentByTag(UNNINSTALL_AND_CANCEL_FRAGMENT_TAG)
        if (cancelFragment != null) {
            ft.show(cancelFragment)
        } else {
            ft.replace(R.id.linearLayout, UnninstallAndCancelFragment.newInstance(), UNNINSTALL_AND_CANCEL_FRAGMENT_TAG)
        }
        ft.replace(R.id.container_main_fragment_small, MainFragment.newInstance(smallIconWidth.toInt(), smallIconHeight, iconsPerRow, iconsPerColumn), SMALL_MAIN_FRAGMENT_TAG)

        ft.commitNow()
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
                                R.id.constraintlayout,
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

   /* fun addEmptyData(pickIntent: Intent) {
        val customInfo = ArrayList<Parcelable>()
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo)
        val customExtras = ArrayList<Parcelable>()
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras)
    };
*/
    override fun onShortcutsBarFragmentInteraction(event: Int) {
        when (event) {
            OPEN_APP_DRAWER -> {
                container_fragment_app_drawer.layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                )
                container_fragment_app_drawer.bringToFront()
                val ft = supportFragmentManager.beginTransaction()
                val mainFragment = supportFragmentManager.findFragmentByTag(MAIN_FRAGMENT_TAG)
                ft.hide(mainFragment!!)
                ft.hide(supportFragmentManager.findFragmentByTag(SHORTCUTS_BAR_FRAGMENT_TAG)!!)
                var appDrawerFragment = supportFragmentManager.findFragmentByTag(APP_DRAWER_FRAGMENT_TAG)
                if (appDrawerFragment == null) {
                    appDrawerFragment = AppDrawerFragment.newInstance()
                    ft.replace(R.id.container_fragment_app_drawer, appDrawerFragment, APP_DRAWER_FRAGMENT_TAG)
                } else {
                    ft.show(appDrawerFragment)
                }
                ft.commit()
            }
            else -> onMainFragmentInteraction(event)
        }
    }

    override fun onWidgetPickFragmentInteraction() {
        val ft = supportFragmentManager.beginTransaction()
        ft.remove(supportFragmentManager.findFragmentByTag(WIDGET_PICK_FRAGMENT_TAG)!!)
        ft.add(
                R.id.container_main_fragment_small,
                MainFragment.newInstance(smallIconWidth.toInt(), smallIconHeight, iconsPerRow, iconsPerColumn),
                SMALLER_MAIN_FRAGMENT_TAG
        )
        linearLayout.bringToFront()
        val cancelFragment = supportFragmentManager.findFragmentByTag(UNNINSTALL_AND_CANCEL_FRAGMENT_TAG)
        if (cancelFragment != null) {
            ft.show(cancelFragment)
        } else {
            ft.replace(R.id.linearLayout, UnninstallAndCancelFragment.newInstance(), UNNINSTALL_AND_CANCEL_FRAGMENT_TAG)
        }
        ft.commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    setCustomBackground(data!!.data!!)
                    supportFragmentManager.beginTransaction()
                            .remove(supportFragmentManager.findFragmentByTag(SETTINGS_FRAGMENT_TAG)!!)
                            .remove(supportFragmentManager.findFragmentByTag(SMALLER_MAIN_FRAGMENT_TAG)!!)
                            .show(supportFragmentManager.findFragmentByTag(MAIN_FRAGMENT_TAG)!!)
                            .show(supportFragmentManager.findFragmentByTag(SHORTCUTS_BAR_FRAGMENT_TAG)!!)
                            .commit()
                }
                SETTINGS_ACTIVITY_CODE -> {
                    recreate()
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
}