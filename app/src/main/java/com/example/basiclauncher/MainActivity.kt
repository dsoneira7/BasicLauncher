package com.example.basiclauncher

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder.createSource
import android.graphics.ImageDecoder.decodeBitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.basiclauncher.fragments.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

//Tags que utilizan los fragmentos
const val MAIN_FRAGMENT_TAG = "mainFragment"
const val SMALL_MAIN_FRAGMENT_TAG = "smallMainFragment"
const val SMALLER_MAIN_FRAGMENT_TAG = "smallerMainFragmentTag"
const val APP_DRAWER_FRAGMENT_TAG = "appDrawerFragment"
const val UNNINSTALL_AND_CANCEL_FRAGMENT_TAG = "utilFragment"
const val SETTINGS_FRAGMENT_TAG = "settingsFragment"
const val WIDGET_PICK_FRAGMENT_TAG = "widgetPickFragment"
const val SHORTCUTS_BAR_FRAGMENT_TAG = "shortcutsBarFragment"

//Constantes que utilizan los callbacks para comunicarse con la Actividad contenedora
const val OPEN_APP_DRAWER = 0
const val ON_ICON_ATTACHED = 1
const val ON_MAIN_MENU_HOLD = 2
const val ON_ANULATE = 3
const val ON_UNNINSTALL = 4
const val ON_EMPTY_CLICK = 5
const val ON_WALLPAPER_CLICK = 6
const val ON_SETTINGS_CLICK = 7
const val ON_WIDGET_CLICK = 8
const val ON_GRID_CLICK_FROM_SMALLER_MODE = 9

//RequestCodes de los Intent implícitos.
const val GALLERY_REQUEST_CODE = 10
const val SETTINGS_ACTIVITY_CODE = 11
const val CAMERA_REQUEST_CODE = 12

//Constantes para operaciones que se llevan a cabo en el método fragmentOperation
const val FRAGMENT_HIDE = 12
const val FRAGMENT_REMOVE = 13
const val FRAGMENT_SHOW = 14
const val FRAGMENT_ADD = 15
const val FRAGMENT_REPLACE = 16

//Anchura y Altura máximas de la imagen
const val MAXIMUM_WIDTH = 720
const val MAXIMUM_HEIGHT = 1280

/**
 * Actividad principal del Launcher.
 */
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

    //Esta variable contiene el Path en el que se guarda un archivo que contendrá temporalmente
    //la fotografía que se tome para utilizar como fondo.
    private var currentBackgroundImagePath: String = ""

    //Booleano que está a true si el icono para el drag se ha pillado del app drawer y no del menú
    //principal
    private var iconFromAppDrawer = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        setContentView(R.layout.activity_main)
        //Inicializamos la clase repository para que cargue los datos necesarios
        Repository.newInstance(applicationContext)!!
        supportActionBar?.hide()

        //Este método inicializará los datos sobre anchura y altura de los iconos en función del
        //tamaño de la pantalla
        iconSizeDataInitialize()

        //Si hay algún fondo guardado, lo cargamos y utilizamos
        val bitmap = Helper.getBackground(applicationContext)
        if (bitmap != null) {
            window.setBackgroundDrawable(BitmapDrawable(resources, bitmap))
        }

        //En el primer drag a veces no se cargaba a tiempo el fragmento contenedor de draggeo,
        //por lo que lo cargamos y escondemos al principio y así ya no hay problema
        val smallMainFragment = MainFragment.newInstance(smallIconWidth.toInt(), smallIconHeight, iconsPerRow, iconsPerColumn)
        supportFragmentManager.beginTransaction()
                .add(R.id.container_main_fragment_small, smallMainFragment, SMALL_MAIN_FRAGMENT_TAG).commitNow()
        supportFragmentManager.beginTransaction()
                .hide(smallMainFragment).commitNow()

        //Mostramos el fragmento principal y la barra de accesos directos
        fragmentOperation(FRAGMENT_ADD, R.id.container_main_fragment, MAIN_FRAGMENT_TAG, R.anim.fade_in)
        fragmentOperation(FRAGMENT_REPLACE, R.id.app_drawer_container, SHORTCUTS_BAR_FRAGMENT_TAG, R.anim.enter_from_bottom)
    }

    /**
     * Método destinado a obtener un bitmap de una imagen escogida del dispositivo y guardarla
     *
     * @param uri: Uri del archivo.
     */
    private fun setCustomBackground(uri: Uri) {
        var bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeBitmap(createSource(contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        }
        //Reescalamos el bitmap para aumentar la eficiencia si la resolución de la imagen
        //es demasiado alta
        bitmap = rescaleBitmap(bitmap)

        //Guardamos la imagen y la configuramos como fondo
        Runnable { Helper.setNewBackground(applicationContext, bitmap) }.run()
        window.setBackgroundDrawable(BitmapDrawable(resources, bitmap))
    }

    /**
     * Método destinado a obtener un bitmap de una imagen tomada por nosotros con la cámara
     * del dispositivo
     */
    private fun setCustomBackground() {
        val bmOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            val scaleFactor: Int = Math.min(outWidth / MAXIMUM_WIDTH, outHeight / MAXIMUM_HEIGHT)
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }
        var bitmap = BitmapFactory.decodeFile(currentBackgroundImagePath, bmOptions)
        bitmap = rescaleBitmap(bitmap)
        Runnable { Helper.setNewBackground(applicationContext, bitmap) }.run()
        window.setBackgroundDrawable(BitmapDrawable(resources, bitmap))
    }

    /**
     * Este método inicializa los datos de anchura y altura de cada celda en función de la altura
     * y anchurad de la pantalla
     */
    private fun iconSizeDataInitialize() {
        //Obtenemos el número de iconos configurado de las SharedPreferences
        val sharedPref = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
        iconsPerRow = sharedPref.getInt(resources.getString(R.string.icon_size), -2)
        if (iconsPerRow == -2) {
            //Si no está configurado lo ponemos por defecto a cuatro iconos por fila
            sharedPref.edit().putInt(resources.getString(R.string.icon_size), 1).apply()
            iconsPerRow = 4
        } else {
            //Necesario sumar 3 por el funcionamiento del SeekBar (ver SettingsFragmentActivity y
            // xml/preference.xml)
            iconsPerRow += 3
        }

        //El ancho se obtiene dividiendo el ancho del fragmento entre el número de iconos.
        //En el caso del fragmento principal este ancho es la pantalla. En los otros casos hay que
        //restarle márgenes y paddings que se introducen. En el caso del smallIconWidth tiene un
        //changer y un margin por cada lado.
        smallIconWidth = (getScreenMetrics().widthPixels - (resources.getDimension(R.dimen.changer_width) + resources.getDimension(R.dimen.changer_margin)) * 2) / iconsPerRow
        smallerIconWidth = (getScreenMetrics().widthPixels - (resources.getDimension(R.dimen.viewpager_paddingLeft) + resources.getDimension(R.dimen.viewpager_paddingRight))) / iconsPerRow
        iconWidth = getScreenMetrics().widthPixels / iconsPerRow

        //Ahora obtenemos la altura del MainFragment grande. La altura de la pantalla menos la altura
        //de la barra de notificaciones, de la barra de accesos y directos y del tabLayout (las
        // bolitas que indican en que pagina estamos).
        val mainFragmentHeight = getScreenMetrics().heightPixels -
                resources.getDimension(R.dimen.tablayout_height).toInt() -
                resources.getDimension(R.dimen.app_drawer_container_height).toInt() -
                getDimen("status_bar_height")

        // Lo que queremos es que cada celda sea lo más cuadrada posible llenando toodo el espacio
        // disponible. El algoritmo para buscar la altura por icono y el número de iconos por
        // culumna consiste en iterar el número de iconos, comparando la altura que tendría cada
        // icono con la anchura que ya hemos obtenido. Una vez tengamos las alturas que más se apro-
        // ximen por arriba y por abajo a la anchura conocida seleccionamos la más próxima.
        var nearestDown = 0
        var nearestUp = 0
        var nearestNumberOfRowsDown = 0
        var nearestNumberOfRowsUp = 0
        var i = 1

        while(true){
            if (((mainFragmentHeight / i) - iconWidth) > 0) {
                nearestUp = mainFragmentHeight / i
                nearestNumberOfRowsUp = i
            } else {
                nearestDown = mainFragmentHeight / i
                nearestNumberOfRowsDown = i
                break
            }
            i++
        }

        //Seleccionamos el número de iconos por arriba o por abajo según la altura que más se apro-
        //xime a la anchura. Así conseguimos que la celda sea lo más cuadrada posible
        iconsPerColumn = if ((nearestUp - iconWidth) < (iconWidth - nearestDown)) {
            nearestNumberOfRowsUp
        } else {
            nearestNumberOfRowsDown
        }

        //Ahora configuramos la anchura para los diferentes tamaños de fragmento, dividiendo
        //la altura que tenedría cada fragmento entre el número de iconos por columna ya conocido.
        iconHeight = mainFragmentHeight / iconsPerColumn
        smallIconHeight = (mainFragmentHeight
                - resources.getDimension(R.dimen.container_small_margin_bottom).toInt()
                - resources.getDimension(R.dimen.container_small_margin_top).toInt()) / iconsPerColumn
        smallerIconHeight = (mainFragmentHeight
                + resources.getDimension(R.dimen.app_drawer_container_height).toInt()
                - resources.getDimension(R.dimen.container_smaller_margin_top).toInt()
                - resources.getDimension(R.dimen.container_smaller_margin_bottom).toInt()) / iconsPerColumn
    }

    /**
     * El botón atrás tiene la función de volver al modo normal (fragmento principal). Si está
     * en modo normal tiene la función de ir a la página 0. Si está en la página 0 no hace nada.
     */
    override fun onBackPressed() {
        val appDrawerFragment = supportFragmentManager.findFragmentByTag(APP_DRAWER_FRAGMENT_TAG)
        val settingsFragment = supportFragmentManager.findFragmentByTag(SETTINGS_FRAGMENT_TAG)
        if ((appDrawerFragment != null) && !(appDrawerFragment as AppDrawerFragment).isHidden) {
            fragmentOperation(FRAGMENT_HIDE, 0, APP_DRAWER_FRAGMENT_TAG, R.anim.exit_to_bottom)
            fragmentOperation(FRAGMENT_SHOW, R.id.container_main_fragment, MAIN_FRAGMENT_TAG, R.anim.enter_from_top_w_fade_in)
            fragmentOperation(FRAGMENT_SHOW, R.id.app_drawer_container, SHORTCUTS_BAR_FRAGMENT_TAG, R.anim.enter_from_top_w_fade_in)
        } else if ((settingsFragment != null) && !(settingsFragment as SettingsFragment).isHidden) {
            fragmentOperation(FRAGMENT_REMOVE, 0, SETTINGS_FRAGMENT_TAG, R.anim.fade_out)
            fragmentOperation(FRAGMENT_REMOVE, 0, SMALLER_MAIN_FRAGMENT_TAG, R.anim.fade_out)
            fragmentOperation(FRAGMENT_SHOW, R.id.container_main_fragment, MAIN_FRAGMENT_TAG, R.anim.fade_in)
            fragmentOperation(FRAGMENT_SHOW, R.id.app_drawer_container, SHORTCUTS_BAR_FRAGMENT_TAG, R.anim.enter_from_bottom)
        } else if (pager.currentItem != 0) {
            pager.currentItem = pager.currentItem - 1
        }
    }


    /**
     * Método destinado a reescalar un bitmap según constantes dadas MAXIMUM_WIDTH Y MAXIMUM_HEIGHT.
     *
     * @param bitmap: [Bitmap] a reescalar.
     *
     * return [Bitmap] ya reescalado
     */
    fun rescaleBitmap(bitmap: Bitmap): Bitmap {
        val imageWidth = bitmap.width
        val imageHeight = bitmap.height

        //Buscamos una resolucion de 1280x720. Obtenemos el ancho y la altura en función de la reso
        //lución que queremos. Lo que llamo factor de anchura y de altura
        val widthFactor = (imageWidth.toDouble())/(MAXIMUM_WIDTH.toDouble())
        val heightFactor = (imageHeight.toDouble())/(MAXIMUM_HEIGHT.toDouble())

        //El factor que sea mayor va a ser el que "sobre" más, por lo que la operación a hacer es:
        //1.- Reescalar el bitmap proporcionalmente para hacer que cuadre con la dimensión que
        //"sobre" menos.
        //2.- Recortar lo que sobre de la dimensión que sobra más.
        return if(widthFactor <= heightFactor){
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, MAXIMUM_WIDTH, (imageHeight.toDouble()/widthFactor).toInt(), true)
            Bitmap.createBitmap(scaledBitmap, 0,(((imageHeight.toDouble()/widthFactor)-MAXIMUM_HEIGHT)/2).toInt(), MAXIMUM_WIDTH, MAXIMUM_HEIGHT)
        }
        else{
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, (imageWidth/heightFactor).toInt(), MAXIMUM_HEIGHT, true)
            Bitmap.createBitmap(scaledBitmap, (((imageWidth.toDouble()/heightFactor)-MAXIMUM_WIDTH)/2).toInt(), 0, MAXIMUM_WIDTH, MAXIMUM_HEIGHT)

        }
    }

    /**
     * Llamada de los [MainFragment] para interactuar con la actividad. La actividad interpreta la
     * operación a realizar según la constante que se le haya pasado.
     *
     * @param action: Constante que denota la función a realizar
     */
    override fun onMainFragmentInteraction(action: Int) {
        when (action) {
            ON_ICON_ATTACHED -> {
                //Esta llamada se lleva a cabo cuando se ha droppeado un icono en una celda
                //Escondemos el fragmento contenedor del drag y el fragmento que contiene los
                //contenedores de cancelación y desinstalación
                fragmentOperation(FRAGMENT_HIDE, 0, SMALL_MAIN_FRAGMENT_TAG, R.anim.fade_out)
                fragmentOperation(FRAGMENT_HIDE, 0, UNNINSTALL_AND_CANCEL_FRAGMENT_TAG, R.anim.exit_to_top)
                container_main_fragment.bringToFront()
                val animation = if (iconFromAppDrawer) {
                    //Si el icono viene del appDrawer hay un desplazamiento en el valor y del
                    //fragmento principal que se debe corregir. Para eso tenemos dos animaciones
                    //diferentes, una con corrección de la y, y otra sin ella. La corrección no es
                    //apreciable a la vista
                    iconFromAppDrawer = false
                    R.anim.fade_in_w_y_correction
                } else {
                    R.anim.fade_in
                }
                //Mostramos el fragmento principal
                fragmentOperation(FRAGMENT_SHOW, R.id.container_main_fragment, MAIN_FRAGMENT_TAG, animation)
            }
            ON_MAIN_MENU_HOLD -> {
                //Llevada a cabo cuando se inicia un drag sobre un icono en el menú principal
                //Se esconde el fragmento principal y se muestran el contenedor de drags
                //y el contenedor de cancelación y desinstalación
                fragmentOperation(FRAGMENT_HIDE, 0, MAIN_FRAGMENT_TAG, R.anim.fade_out)
                fragmentOperation(FRAGMENT_SHOW, R.id.container_main_fragment_small, SMALL_MAIN_FRAGMENT_TAG, R.anim.fade_in)
                linearLayout.bringToFront()
                fragmentOperation(FRAGMENT_SHOW, R.id.linearLayout, UNNINSTALL_AND_CANCEL_FRAGMENT_TAG, R.anim.enter_from_top)
            }
            ON_EMPTY_CLICK -> {
                //Llevada a cabo cuando se mantiene pulsado sobre una celda vacía. Pasa al modo
                //de ajustes. Se esconde el fragmento principal y la barra de accesos directos
                //y se cargan el fragmento pequeño y el fragmento de ajustes.
                fragmentOperation(FRAGMENT_HIDE, 0, MAIN_FRAGMENT_TAG, R.anim.fade_out)
                container_main_fragment_smaller.bringToFront()
                fragmentOperation(FRAGMENT_REPLACE, R.id.container_main_fragment_smaller, SMALLER_MAIN_FRAGMENT_TAG, R.anim.fade_in)
                fragmentOperation(FRAGMENT_HIDE, 0, SHORTCUTS_BAR_FRAGMENT_TAG, R.anim.exit_to_bottom)
                fragmentOperation(FRAGMENT_REPLACE, R.id.container_settings, SETTINGS_FRAGMENT_TAG, R.anim.fade_in)
            }
            //Para otras constantes que lleguen el funcionamiento es análogo a cuando pulsas el botón
            //atrás dependiendo del modo en el que se halle
            else -> onBackPressed()
        }
    }

    /**
     * Método que usa [AppDrawerFragment] para interactuar con la actividad contenedora.
     */
    override fun onFragmentInteraction() {
        //Se lleva a cabo cuando se inicia un Drag desde el App Drawer. Se esconde el appDrawer,
        //y se muestran los fragemntos contenedores del drag y la barra de accesos directos.
        fragmentOperation(FRAGMENT_HIDE, 0, APP_DRAWER_FRAGMENT_TAG, R.anim.exit_to_bottom)
        fragmentOperation(FRAGMENT_SHOW, R.id.app_drawer_container, SHORTCUTS_BAR_FRAGMENT_TAG, R.anim.enter_from_top_w_fade_in)
        iconFromAppDrawer = true
        linearLayout.bringToFront()
        container_main_fragment_small.bringToFront()
        fragmentOperation(FRAGMENT_SHOW, R.id.linearLayout, UNNINSTALL_AND_CANCEL_FRAGMENT_TAG, R.anim.enter_from_top)
        fragmentOperation(FRAGMENT_SHOW, R.id.container_main_fragment_small, SMALL_MAIN_FRAGMENT_TAG, R.anim.fade_in)
    }

    /**
     * Método que usa [UnninstallAndCancelFragment] para interactuar con la actividad contenedora.
     */
    override fun onUnninstallAndCancelFragmentInteraction(event: Int, argument: String) {
        when (event) {

            ON_ANULATE -> {
                Thread {
                    if (argument.contains(";")) {
                        val dataFragmented = argument.split(";") //formato: packageName;page;position
                        Repository.newInstance(applicationContext)!!.stateOccupied(dataFragmented[0],
                                Integer.parseInt(dataFragmented[1]),
                                Integer.parseInt(dataFragmented[2]))
                    }
                }.start()
            }

            ON_UNNINSTALL -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val intent = Intent(this, this.javaClass)
                    val sender = PendingIntent.getActivity(this, 0, intent, 0)
                    packageManager.packageInstaller.uninstall(argument, sender.intentSender)
                } else {
                    val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$argument"))
                    startActivity(intent)
                }
            }

        }
        //Los operaciones de fragmentos que se llevan a cabo una vez terminada la actividad específicas,
        //son las mismas que cuando se añade un icono al menú principal
        onMainFragmentInteraction(ON_ICON_ATTACHED)
    }

    /**
     * Método que usa [SettignsFragment] para interactuar con la actividad contenedora.
     */
    override fun onSettingsFragmentInteraction(event: Int) {
        when (event) {
            ON_WALLPAPER_CLICK -> {
                showPictureDialog()
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
                        .commit()
                        val mAppWidgetHost = AppWidgetHost(this, 1024)
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

    /**
     * Método que usa [ShortcutsBarFragment] para interactuar con la actividad contenedora.
     */
    override fun onShortcutsBarFragmentInteraction(event: Int) {
        when (event) {
            OPEN_APP_DRAWER -> {
                //Llamada cuando se arrastra desde la barra hacia arriba.
                //Se esconden el fragmento princiapl y la barra de accesos directos y
                //se muestra el lanzador de aplicaciones
                if (supportFragmentManager.findFragmentByTag(APP_DRAWER_FRAGMENT_TAG) == null) {
                    container_fragment_app_drawer.layoutParams = ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            ConstraintLayout.LayoutParams.MATCH_PARENT
                    )
                }
                container_fragment_app_drawer.bringToFront()
                fragmentOperation(FRAGMENT_HIDE, 0, MAIN_FRAGMENT_TAG, R.anim.exit_to_top_w_fade_out)
                fragmentOperation(FRAGMENT_HIDE, 0, SHORTCUTS_BAR_FRAGMENT_TAG, R.anim.fade_out)
                fragmentOperation(FRAGMENT_SHOW, R.id.container_fragment_app_drawer, APP_DRAWER_FRAGMENT_TAG, R.anim.enter_from_bottom)
            }
            //La barra de accesos directos es análoga a una página del viewPager, por lo que se pueden
            //aplicar las mismas funciones exactamente quitando el apartado anterior.
            else -> onMainFragmentInteraction(event)
        }
    }

    /**
     * Método que usa [WidgetPickFragment] para interactuar con la actividad contenedora.
     */
    override fun onWidgetPickFragmentInteraction() {
        fragmentOperation(FRAGMENT_REMOVE, 0, WIDGET_PICK_FRAGMENT_TAG, null)
        fragmentOperation(FRAGMENT_SHOW, R.id.container_main_fragment_small, SMALL_MAIN_FRAGMENT_TAG, null)
        linearLayout.bringToFront()
        fragmentOperation(FRAGMENT_SHOW, R.id.linearLayout, UNNINSTALL_AND_CANCEL_FRAGMENT_TAG, R.anim.enter_from_top)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    //Se configura el fondo según la imágen seleccionada en la galería y se vuelve
                    //al estado inicial
                    setCustomBackground(data!!.data!!)
                    onBackPressed()
                }

                CAMERA_REQUEST_CODE -> {
                    //Se añade la fotografía a la galería, se configura el fondo según la imagen
                    //seleccionada y se vuelve al estado inicial
                    Thread {
                        addPictureToGallery()
                    }.start()
                    setCustomBackground()
                    onBackPressed()
                }

                SETTINGS_ACTIVITY_CODE -> {
                    //Se llama cuando se presiona atrás en la actividad de preferencias,
                    //Se comprueba si ha habido algún cambio, y si es así, se recarga la activdad
                    val sharedPref = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
                    if ((iconsPerRow -3)  != sharedPref.getInt(resources.getString(R.string.icon_size), -2)) {
                        this.recreate()
                    }
                }
            }
        }
    }

    /**
     * @return [DisplayMetrics] de la pantalla.
     */
    private fun getScreenMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        return metrics
    }

    /**
     * @param name: Nombre del recurso que se quiere obtener
     * @return Valor del recurso
     */
    private fun getDimen(name: String): Int {
        var result = 0
        val resourceId = resources.getIdentifier(name, "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * Este método se define para eliminar código repetido y aumentar la escalabilidad al operar con
     * [Fragment]. Para cada fragmento que se quiera operar, se inicia una [FragmentTransaction] y
     * se opera según se le indique.
     *
     * @param operation: Constante que indica la operación a realizar con el fragmento.
     * @param containerId: ID del container en el que se va a colocar el fragmento.
     * @param tag: Etiqueta del fragmento en cuestión. La etiqueta también indica que instancia de que
     *             fragmento necesitamos.
     * @param animation: Si es diferente de null nos indica el ID de la animación que va utilizar
     *                   esta transacción.
     */
    private fun fragmentOperation(operation: Int, containerId: Int, tag: String, animation: Int?) {
        val ft = supportFragmentManager.beginTransaction()

        if (animation != null) {
            ft.setCustomAnimations(animation, animation)
        }

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
                if (fragment != null) {
                    ft.hide(fragment)
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

    /**
     * @param tag: La etiqueta del fragmento que queremos operar.
     *
     * @return Instancia del fragmento que solicitemos según el tag.
     */
    private fun selectFragmentByTag(tag: String): Fragment {
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

    /**
     * Este método muestra un Dialog para seleccionar la acción a realizar para configurar un fondo.
     */
    private fun showPictureDialog() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            choosePhotoFromGallery()
        }
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Selecciona una opción")
        val pictureDialogItems = arrayOf("Imagen almacenada en dispositivo", "Capturar una fotografía con la cámara", "Poner fondo blanco")
        pictureDialog.setItems(pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> choosePhotoFromGallery()
                1 -> takePhotoFromCamera()
                2 -> resetWallpaperToWhite()
            }
        }
        pictureDialog.show()
    }

    /**
     * Lanza un [Intent] implícito para seleccionar una imagen de la galería.
     */
    private fun choosePhotoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        //intent.addCategory(Intent.CATEGORY_OPENABLE)
        val chooser = Intent.createChooser(intent, "Completar acción con: ")
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(chooser, GALLERY_REQUEST_CODE)
        }
    }

    /**
     * Lanza un [Intent] implícito para tomar una fotografía y establecerla como fondo de pantalla.
     */
    private fun takePhotoFromCamera() {
        val imageFile = try {
            //Es necesario crear un archivo en el que se guardará la imagen al tomarla.
            createImageFile()
        } catch (e: IOException) {
            Log.e("ERROR", e.stackTrace!!.contentToString())
            Toast.makeText(this, "Error al crear el archivo para guardar la imagen", Toast.LENGTH_LONG).show()
            return
        }
        val imageURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", imageFile)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI)
        intent.resolveActivity(packageManager)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)

    }

    /**
     * Pone el fondo de pantalla blanco y borra los datos guardados sobre el fondo de pantalla.
     */
    private fun resetWallpaperToWhite() {
        window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        Helper.deleteBackgroundData(applicationContext)
        onBackPressed()
    }

    /**
     * Crea un archivo en el que se guardará la imagen al tomarla con la cámara.
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentBackgroundImagePath = absolutePath
        }
    }

    /**
     * Añade la imágen tomada anteriormente a la galería para que sea pública para el usuario.
     */
    private fun addPictureToGallery() {
        val mediaScanIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Intent(Intent.ACTION_MEDIA_MOUNTED)
        } else {
            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        }
        val f = File(currentBackgroundImagePath)
        mediaScanIntent.data = Uri.fromFile(f)
        sendBroadcast(mediaScanIntent)

    }


}