package com.example.basiclauncher.activities

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.basiclauncher.Helper
import com.example.basiclauncher.R
import com.example.basiclauncher.Repository
import com.example.basiclauncher.fragments.*
import com.example.basiclauncher.viewmodels.MainActivityViewModel
import com.example.basiclauncher.viewmodels.factories.MainActivityViewModelFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main.*

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
const val RETURN_TO_MAIN_MODE = 6
const val RECREATE_ACTIVITY = 7
const val ON_GRID_CLICK_FROM_SMALLER_MODE = 8

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

    //Booleano que está a true si el icono para el drag se ha pillado del app drawer y no del menú
    //principal
    private var iconFromAppDrawer = false

    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)

        setContentView(R.layout.activity_main)

        //Inicializamos la clase repository para que cargue los datos necesarios
        val repository = Repository.getInstance(applicationContext)!!

        //Esta tarea carga el fondo en segundo plano y lo muestra (si lo hay)
        WallpaperLoadingAsyncTask().execute()

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        viewModel = ViewModelProviders.of(this, MainActivityViewModelFactory(application, metrics.widthPixels, metrics.heightPixels)).get(MainActivityViewModel::class.java)
        supportActionBar?.hide()

        if(repository.getIconsPerRow() != viewModel.iconsPerRow){
            viewModel.iconSizeDataInitialize()
        }

        //En el primer drag a veces no se cargaba a tiempo el fragmento contenedor de draggeo,
        //por lo que lo cargamos y escondemos al principio y así ya no hay problema
        val smallMainFragment = MainFragment.newInstance(viewModel.smallIconWidth, viewModel.smallIconHeight, viewModel.iconsPerRow, viewModel.iconsPerColumn)
        supportFragmentManager.beginTransaction()
                .add(R.id.container_main_fragment_small, smallMainFragment, SMALL_MAIN_FRAGMENT_TAG).commitNow()
        supportFragmentManager.beginTransaction()
                .hide(smallMainFragment).commitNow()

        //Mostramos el fragmento principal y la barra de accesos directos
        fragmentOperation(FRAGMENT_ADD, R.id.container_main_fragment, MAIN_FRAGMENT_TAG, R.anim.fade_in)
        fragmentOperation(FRAGMENT_REPLACE, R.id.app_drawer_container, SHORTCUTS_BAR_FRAGMENT_TAG, R.anim.enter_from_bottom)
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
            RETURN_TO_MAIN_MODE -> {
                onBackPressed()
            }
            RECREATE_ACTIVITY -> {
                this.recreate()
            }
            else -> {
                Log.e("ERROR: SettingsFrag", "Operation code not supported")
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
                ft.add(containerId, fragment!!, tag)
            }

            FRAGMENT_REPLACE -> {
                val fragment = selectFragmentByTag(tag)
                ft.replace(containerId, fragment!!, tag)
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
    private fun selectFragmentByTag(tag: String): Fragment? {
        return when (tag) {
            MAIN_FRAGMENT_TAG -> MainFragment.newInstance(viewModel.iconWidth, viewModel.iconHeight, viewModel.iconsPerRow, viewModel.iconsPerColumn)
            SMALL_MAIN_FRAGMENT_TAG -> MainFragment.newInstance(viewModel.smallIconWidth, viewModel.smallIconHeight, viewModel.iconsPerRow, viewModel.iconsPerColumn)
            SMALLER_MAIN_FRAGMENT_TAG -> MainFragment.newInstance(viewModel.smallerIconWidth, viewModel.smallerIconHeight, viewModel.iconsPerRow, viewModel.iconsPerColumn)
            APP_DRAWER_FRAGMENT_TAG -> AppDrawerFragment.newInstance()
            UNNINSTALL_AND_CANCEL_FRAGMENT_TAG -> UnninstallAndCancelFragment.newInstance()
            SETTINGS_FRAGMENT_TAG -> SettingsFragment.newInstance()
            SHORTCUTS_BAR_FRAGMENT_TAG -> ShortcutsBarFragment.newInstance()
            else -> {
                Log.e("ERROR", "Operation not supported")
                null
            }
        }
    }

    private inner class WallpaperLoadingAsyncTask: AsyncTask<Unit,Unit, Bitmap>(){
        override fun doInBackground(vararg p0: Unit?): Bitmap? {
            //Si hay algún fondo guardado, lo cargamos y utilizamos
            return Helper.getBackground(applicationContext)
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            if (result != null) {
                window.setBackgroundDrawable(BitmapDrawable(resources, result))
            }
        }
    }


}