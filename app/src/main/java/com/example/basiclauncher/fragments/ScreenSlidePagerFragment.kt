package com.example.basiclauncher.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.basiclauncher.*
import com.example.basiclauncher.activities.ON_GRID_CLICK_FROM_SMALLER_MODE
import com.example.basiclauncher.adapters.IS_NORMAL
import com.example.basiclauncher.adapters.IS_SMALL
import com.example.basiclauncher.adapters.IS_SMALLER
import com.example.basiclauncher.classes.CustomLinearLayout
import com.example.basiclauncher.classes.CustomLinearLayoutState
import com.example.basiclauncher.viewmodels.MainFragmentViewModel
import com.example.basiclauncher.viewmodels.ScreenSlidePagerViewModel
import com.example.basiclauncher.viewmodels.factories.ScreenSlidePagerViewModelFactory
import kotlinx.android.synthetic.main.viewpagerfragment.*

const val LONG_HOLD_VALUE: Long = 700
const val CELL_NOT_FOUND: String = "Cell not found."

private const val ARG_PAGE = "position"
private const val ARG_IS_SMALL_FRAGMENT = "isSmallFragment"
/**
 * Subclase de [Fragment] que se corresponde con cada una de las páginas del [ViewPager].
 * Contiene una GridLayout que guarda la posición de nuestros iconos guardados.
 * Se debe utilizar el método factoría [newInstance] para crear una instancia.
 */
class ScreenSlidePagerFragment : Fragment() {

    companion object {
        fun newInstance(position: Int, isSmallFragment: Int) : ScreenSlidePagerFragment{
            val bundle = Bundle()
            bundle.putInt(ARG_PAGE, position)
            bundle.putInt(ARG_IS_SMALL_FRAGMENT, isSmallFragment)
            val fragment = ScreenSlidePagerFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    private lateinit var onIconAttachedListener: (Int) -> Unit

    private lateinit var viewModel: ScreenSlidePagerViewModel
    //Guardamos una referencia al ViewModel del MainFragment contenedor para comunicar la pagina
    //en la que se ha droppeado un icono u obtener el tamaño de los iconos.
    private lateinit var mainFragmentViewModel: MainFragmentViewModel
    private var iconWidth = 0
    private var iconHeight = 0
    private var page = 0
    private var isSmallFragment = 0
    private var numberOfIcons = 0
    private lateinit var pageGridDataSaved: Array<CustomLinearLayoutState>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = arguments?.getInt("position", page)!!
        isSmallFragment = arguments?.getInt("isSmallFragment", isSmallFragment)!!
        mainFragmentViewModel = ViewModelProviders.of(this.activity!!).get(MainFragmentViewModel::class.java)
        numberOfIcons = mainFragmentViewModel.iconsPerColumn * mainFragmentViewModel.iconsPerRow
        viewModel = ViewModelProviders.of(this, ScreenSlidePagerViewModelFactory(activity!!.application, page)).get(ScreenSlidePagerViewModel::class.java)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View = inflater.inflate(R.layout.viewpagerfragment, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //Según que tamaño tenga el fragmento inicializamos los parámetros del tamñao de los iconos
        when (isSmallFragment) {
            IS_NORMAL -> {
                iconHeight = mainFragmentViewModel.iconHeight
                iconWidth = mainFragmentViewModel.iconWidth
            }
            IS_SMALL -> {
                iconHeight = mainFragmentViewModel.smallIconHeight
                iconWidth = mainFragmentViewModel.smallIconWidth.toInt()
            }
            IS_SMALLER -> {
                iconHeight = mainFragmentViewModel.smallerIconHeight
                iconWidth = mainFragmentViewModel.smallerIconWidth.toInt()
            }
            else -> Log.e("ERROR", "Constante errónea")
        }

        gridlayout.columnCount = mainFragmentViewModel.iconsPerRow
        gridlayout.rowCount = mainFragmentViewModel.iconsPerColumn

        gridConfiguration()

        //Nunca va a haber un cambio en los iconos cuando el fragmento es pequeño (ajustes), por
        //lo que no es necesario observar los cambios. En los otros casos si es necesario, también
        //porque son fragmentos que están siempre activos y necesitan actualizarse con los cambios
            viewModel.stateList.observe(this, observer)


        //Si el fragmento es grande, el fondo es transparente, por el contrario, si no lo es,
        //ponemos un filtro grisáceo translúcido.
        if (isSmallFragment != IS_NORMAL) {
            content.background = ColorDrawable(ContextCompat.getColor(context!!, R.color.blackLowAlpha))
        } else {
            content.background = ColorDrawable(Color.TRANSPARENT)
        }

        //Si el fragmento es pequeño también queremos que cuando clickemos en el grid nos lleve a la
        //página correspondiente y nos devuelva al modo normal
        if (isSmallFragment == IS_SMALLER) {
            view!!.setOnClickListener {
                onPostIconAttached(ON_GRID_CLICK_FROM_SMALLER_MODE)
            }
        }
    }

    override fun onResume() {
        if (isSmallFragment != IS_NORMAL) {
            content.setBackgroundColor(ContextCompat.getColor(context!!, R.color.blackLowAlpha))
        } else {
            content.setBackgroundColor(Color.TRANSPARENT)
        }
        super.onResume()
    }

    //El observer se encarga de observar los datos, compararlos con la anterior muestra de datos
    //que ha quedado guardado, y realizar los cambios necesarios.
    private val observer: Observer<Array<CustomLinearLayoutState>> = Observer {
        //Si no hay datos guardados (primera observación) pintamos todos los iconos, y guardamos
        //los datos, si no, comparamos los datos nuevos con los guardados
        if (::pageGridDataSaved.isInitialized) {
                //Comprobamos primero si hay iconos anteriores que ahora ya no están
                for (i in pageGridDataSaved) {
                    if (!it.contains(i)) {
                        //Si se da el caso, buscamos la celda correspondiente en la vista y la limpiamos
                        //Cuando se crea la celda se le asigna un id arbitrario que depende de página y posición
                        val cell = view!!.findViewById<CustomLinearLayout>((page + 1) * (i.position + 1))
                        if (cell != null && !cell.isEmpty()) {
                            cell.clear()
                        } else {
                            Log.d("ERROR", CELL_NOT_FOUND)
                        }
                    }
                }
                //Comprobamos después si hay iconos nuevos que debemos pintar. Actuamos de manera análoga.
                //Se podría comprobar el tamaño de los datos guardados y de los nuevos para proceder
                //a comprobar solo cuales se han añadido o cuales se han quitado, pero empíricamente
                //se ha comprobado que a veces hay cambios de más de un elemento. Además esto generaba
                //problemas cuando se eliminaba alguna página de en medio y se realojaban los iconos
                //de las páginas correspondientes
                for (i in it) {
                    if (!pageGridDataSaved.contains(i)) {
                        val cell = view!!.findViewById<CustomLinearLayout>((page + 1) * (i.position + 1))
                        //Es necesario comprobar si la celda existe (!= null). Si no tenemos la app
                        //correspondiente guardada o si estamos droppeando un icono en una celda
                        //que ya contiene ese mismo iconos no pintamos nada.
                        if (cell == null) {
                            Log.d("ERROR", CELL_NOT_FOUND)
                        } else if ((cell.isEmpty() || cell.getAppId() != i.appId) && viewModel.appList.value!!.get(i.appId) != null) {
                            cell.setApp(viewModel.appList.value!!.get(i.appId))
                        }
                    }
                }

        } else {
            //Pintamos todos los iconos
            for (i in it) {
                val cell = view!!.findViewById<CustomLinearLayout>((page + 1) * (i.position + 1))
                if (cell == null) {
                    Log.d("ERROR", CELL_NOT_FOUND)
                } else if ((cell.isEmpty() || cell.getAppId() != i.appId) && viewModel.appList.value!!.get(i.appId) != null) {
                    cell.setApp(viewModel.appList.value!!.get(i.appId))
                }
            }
        }
        //Guardamos los datos de esta iteración para comparar con los próximos que lleguen
        pageGridDataSaved = it
    }

    /**
     * Esta función hace una configuración inicial necesaria en la GridLayout, creando las celdas
     * necesarias y pintando los iconos si hay datos guardados al respecto
     */
    private fun gridConfiguration() {
        for (i in 0 until numberOfIcons) {
            //Instanciamos la celda. Si el fragmento no es grande, la celda no necesita
            //implementar longHoldListeners ni los mismos OnClickListeners.
            val linearLayout = if (isSmallFragment == IS_NORMAL) {
                CustomLinearLayout(context, page, i, true)
            } else {
                CustomLinearLayout(context, page, i, false)
            }
            //Callbacks para interactuar con los fragmentos contenedores según sea necseario
            //El primero se utiliza para actualizar la BBDD, y el segundo para varias operaciones,
            //como avisar de un LongClick o de un drop en la celda en cuestión.
            linearLayout.attachListeners({ packageName, page, position -> onIconAttached(packageName, page, position) },
                    { onPostIconAttached(it) })

            linearLayout.id = (page + 1) * (i + 1) //Asignamos un ID arbitrario para identificarla después

            linearLayout.layoutParams = LinearLayout.LayoutParams(iconWidth, iconHeight)


            //Añadimos la celda.
            gridlayout.addView(linearLayout)
        }
    }

    private fun onIconAttached(packageName: String, page: Int, position: Int) {
        //Si el packageName está vacío, significa que hay que borrar el state de la BBDD, por ejemplo,
        //cuando se inicia el drag.
        if (packageName == "") {
            Thread {
                viewModel.emptyState(page, position)
            }.start()
            return
        }
        //Por el contrario si packageName no está vacío, significa que se ha hecho un drop y interac-
        //tuamos con el viewmodel para actualizar la BBDD
        mainFragmentViewModel.page = page
        viewModel.stateOccupied(packageName, page, position)
    }

    /**
     * Se utiliza para interactuar directamente con el [MainFragment] padre
     */
    private fun onPostIconAttached(event: Int) {
        onIconAttachedListener(event)
    }

    /**
     * Método que enlaza los callbacks del [MainFragment] padre
     */
    fun attachListeners(onIconAttachedListener: (Int) -> Unit) {
        this.onIconAttachedListener = onIconAttachedListener
    }

}
