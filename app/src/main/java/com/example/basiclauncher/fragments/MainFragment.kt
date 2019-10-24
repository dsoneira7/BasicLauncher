package com.example.basiclauncher.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.example.basiclauncher.*
import com.example.basiclauncher.adapters.IS_NORMAL
import com.example.basiclauncher.adapters.IS_SMALL
import com.example.basiclauncher.adapters.IS_SMALLER
import com.example.basiclauncher.adapters.ScreenSlidePagerAdapter
import com.example.basiclauncher.viewmodels.MainFragmentViewModel
import kotlinx.android.synthetic.main.fragment_main.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"
private const val ARG_PARAM4 = "param4"
const val CHANGER_ANIMATION_DURATION = 200L

/**
 * Subclase de [Fragment] que contiene un [ViewPager], y otros componentes dependiendo de su función
 * en el momento. Según su tamaño se utilizará para:
 *  - La actividad y visualización normal del Launcher (si es normal)
 *  - Contenedor para el draggeo de aplicaciones o widgets (si es medio)
 *  - Muestra de las páginas en pequeño para dar una visión más general en el modo de ajustes (si es pequeño)
 *
 * Se debe utilizar el método factoría [newInstance] para crear una instancia. La actividad conte-
 * nedora debe implementar [OnMainFragmentInteractionListener] para la comunicación.
 */
class MainFragment : Fragment(), ViewPager.OnPageChangeListener {

    private var iconWidth: Int = 0
    private var iconHeight: Int = 0
    private var numberOfColumns: Int = 0
    private var numberOfRows: Int = 0
    private var listener: OnMainFragmentInteractionListener? = null
    private lateinit var viewModel: MainFragmentViewModel
    private var threadInterrupted = true
    private var nPages: Int = 0
    private lateinit var leftChangeListenerLayout: LinearLayout
    private lateinit var rightChangeListenerLayout: LinearLayout
    private lateinit var pagerContainer: LinearLayout
    private var onEdgeAnimationsEnabled: Boolean = true
    private lateinit var pagerAdapter: ScreenSlidePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            iconWidth = it.getInt(ARG_PARAM1)
            iconHeight = it.getInt(ARG_PARAM2)
            numberOfColumns = it.getInt(ARG_PARAM3)
            numberOfRows = it.getInt(ARG_PARAM4)
        }
    }

    /**
     * Cuando se enlaza un fragmento hijo (cada una de las páginas) le pasamos un callback para poder
     * comunicarse con la actividad contenedora cuando sea necesario hacer operaciones de fragmentos.
     */
    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)

        if (childFragment is ScreenSlidePagerFragment) {
            childFragment.attachListeners { listener!!.onMainFragmentInteraction(it) }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        viewModel = ViewModelProviders.of(this.activity!!).get(MainFragmentViewModel::class.java)

        var isSmall = IS_NORMAL

        //Guardamos en el viewmodel cada uno de los tamaños en pixeles de los iconos que se corres-
        //ponden con el tipo de fragmento en cuestión
        when (tag) {
            SMALL_MAIN_FRAGMENT_TAG -> {
                //Si el fragmento es medio (contenedor de draggeo), también necesitamos operar con
                //los layouts utilizados para cambiar de página en los laterales
                rightChangeListenerLayout = view.findViewById(R.id.right_change_listener)
                leftChangeListenerLayout = view.findViewById(R.id.left_change_listener)
                isSmall = IS_SMALL
                viewModel.smallIconHeight = iconHeight
                viewModel.smallIconWidth = iconWidth.toFloat()
            }
            SMALLER_MAIN_FRAGMENT_TAG -> {
                isSmall = IS_SMALLER
                viewModel.smallerIconHeight = iconHeight
                viewModel.smallerIconWidth = iconWidth.toFloat()
            }
            else -> {
                viewModel.iconsPerColumn = numberOfRows
                viewModel.iconsPerRow = numberOfColumns
                viewModel.iconWidth = iconWidth
                viewModel.iconHeight = iconHeight
            }
        }

        nPages = Integer.parseInt(Helper.getFromSharedPreferences(this.activity!!.packageName,
                "nPages", "0", this.activity!!.applicationContext)!!)
        if (nPages == 0) {
            //Si el número de páginas no está en las SharedPreferences (primera vez que se abre la app
            //lo inicializamos a 1
            Helper.putInSharedPreferences(this.activity!!.packageName, "nPages", "1", this.activity!!.applicationContext)
            nPages = 1
        }
        pagerAdapter = if (tag.equals(SMALL_MAIN_FRAGMENT_TAG)) {
            ScreenSlidePagerAdapter(childFragmentManager, isSmall, nPages + 1)
        } else {
            ScreenSlidePagerAdapter(childFragmentManager, isSmall, nPages)
        }
        val pager = view.findViewById<ViewPager>(R.id.pager)
        pager.offscreenPageLimit = nPages //Necesitamos que estén todas las páginas cargadas en memoria o el drag no funcionará bien cuando arrastremos fuera del offscreenLimit
        pager.adapter = pagerAdapter
        pagerContainer = view.findViewById(R.id.pager_container)

        //Configuramos los pageChangers (los layouts que se utilizan para cambiar de página en el draggeo)
        //de ser necesario, así como las animaciones.
        if (tag.equals(SMALL_MAIN_FRAGMENT_TAG)) {
            configurePageChangeListenerLayout(
                    leftChangeListenerLayout,
                    (resources.getDimension(R.dimen.changer_width)).toInt(),
                    rightChangeListenerLayout,
                    (resources.getDimension(R.dimen.changer_width)).toInt()
            )
            rightChangeListenerLayout.setOnDragListener { v, dragEvent -> customOnDragListener(v, dragEvent) }
            leftChangeListenerLayout.setOnDragListener { v, dragEvent -> customOnDragListener(v, dragEvent) }
            if (viewModel.page == 0) {
                setChangerAnimator(rightChangeListenerLayout, leftChangeListenerLayout, 2f, -resources.getDimension(R.dimen.changer_width))
                onEdgeAnimationsEnabled = false
            } else if (viewModel.page == nPages) {
                setChangerAnimator(leftChangeListenerLayout, rightChangeListenerLayout, 2f, resources.getDimension(R.dimen.changer_width))
                onEdgeAnimationsEnabled = false
            }
        }

        //Si se trata del mod ajustes cambiamos el padding, margin para que se vean varias páginas
        //en pantalla
        if (tag.equals(SMALLER_MAIN_FRAGMENT_TAG)) {
            pager.setPadding(resources.getDimension(R.dimen.viewpager_paddingLeft).toInt(),
                    0,
                    resources.getDimension(R.dimen.viewpager_paddingRight).toInt(),
                    0)
            pager.clipToPadding = false
            pager.pageMargin = resources.getDimension(R.dimen.viewpager_margin).toInt()
        }

        //En el viewmodel guardamos la página en la que estaba el anterior fragmento cuano hacemos
        //operaciones de fragmentos. (Por ejemplo, si hacemos un drag desde la página 3, debemos
        //hacer que el fragmento contenedor esté en la página 3 también).
        pager.addOnPageChangeListener(this)
        pager.currentItem = viewModel.page


        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnMainFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    /**
     * Cuando el fragmento se muestra despues de estar escondido, es necesario hacer varias tareas
     * de actualización. Desde añadir páginas al adaptador, a moverse a una página en la que debería
     * estar, o establecer animaciones si se trata del fragmento contenedor del drag.
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            //Si se ha modificado el número de páginas (se ha añadido o quitado alguna) mientras el
            //fragmento estaba escondido y no se corresponde con el valor guardado, añadimos o
            //o quitamos una página
            val newNPages = Integer.parseInt(Helper.getFromSharedPreferences(this.activity!!.packageName,
                    "nPages", "0", this.activity!!.applicationContext)!!)
            Log.d(this.toString(), "OldPages: $nPages   newPages: $newNPages")
            if (nPages > newNPages) {
                pagerAdapter.deleteItem()
                Log.d(this.toString(), "Page Deleted")
                view!!.invalidate()
                nPages = newNPages
                pager.offscreenPageLimit = nPages
            } else if (nPages < newNPages) {
                pagerAdapter.addNewItem()
                Log.d(this.toString(), "Page Added")
                nPages = newNPages
                pager.offscreenPageLimit = nPages
            }
            if (tag == SMALL_MAIN_FRAGMENT_TAG) {
                //Si el fragmento está en uno de los extremos tenemos que animar lso changers como sea
                //necesario. Si no puede avanzar hacia uno de los lados escondemos ese changer y
                //desactivamos las animaciones
                if (viewModel.page == 0) {
                    setChangerAnimator(rightChangeListenerLayout, leftChangeListenerLayout, 2f, -resources.getDimension(R.dimen.changer_width))
                    onEdgeAnimationsEnabled = false
                } else if (viewModel.page == pager.adapter!!.count - 1 ) {
                    setChangerAnimator(leftChangeListenerLayout, rightChangeListenerLayout, 2f, resources.getDimension(R.dimen.changer_width))
                    onEdgeAnimationsEnabled = false
                }
                else{
                    onEdgeAnimationsEnabled = true
                }
            }
            //Nos movemos a la página correspondiente
            pager.setCurrentItem(viewModel.page, false)
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onPageScrollStateChanged(state: Int) {
        //Solamente es necesario el método OnPageSelected
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        //Solamente es necesario el método OnPageSelected
    }

    /**
     * Cuando se cambia de página actualizamos el valor guardado en el viewModel
     */
    override fun onPageSelected(position: Int) {
        if(position >viewModel.page-2 && position < viewModel.page+2){
            viewModel.page = position
        }
    }


    private fun customOnDragListener(v: View, event: DragEvent): Boolean {

        //Thread que se utiliza para cambiar de página. Cuando entramos en uno de los pageChangers
        //durante el drag iniciamos el thread, que hace un sleep durante un tiempo dado.
        //Cuando acabe el sleep, si seguimos dentro del pageChanger hace un cambio de página, si no,
        //no hace nada.
        val longHoldThread = Thread {
            Thread.sleep(LONG_HOLD_VALUE)
            if (!threadInterrupted) {
                v.post {
                    //Si no se ha interrumpido activamos las animaciones si necesario y ejecutamos
                    //el cambio de página que corresponda
                    onEdgeAnimationsEnabled = true
                    when (v.id) {
                        R.id.left_change_listener -> {
                            pager.setCurrentItem(pager.currentItem - 1, true)
                        }

                        R.id.right_change_listener -> {
                            pager.setCurrentItem(pager.currentItem + 1, true)
                        }
                    }
                }
            }
        }

        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED -> {
                //Iniciamos el thread siempre y cuando no estemos intentando cambiar a una página
                //no existente (por ejemplo, en la página 0 queriendo avanzar a la izquierda)
                if (!((v.id == R.id.left_change_listener && pager.currentItem == 0)
                                || (v.id == R.id.right_change_listener && pager.currentItem == pager.adapter!!.count - 1))) {
                    longHoldThread.start() //Iniciamos el hilo y ejecutamso las animaciones
                    threadInterrupted = false
                    v.startAnimation(AnimationUtils.loadAnimation(context!!.applicationContext, R.anim.on_drag_select))
                    when (v.id) {
                        R.id.right_change_listener -> {
                            setChangerAnimator(rightChangeListenerLayout, leftChangeListenerLayout, 2f, 0 - resources.getDimension(R.dimen.changer_width))
                        }
                        R.id.left_change_listener -> {
                            setChangerAnimator(leftChangeListenerLayout, rightChangeListenerLayout, 2f, +resources.getDimension(R.dimen.changer_width))
                        }
                    }
                }
            }

            //Si se sale del pageChanger o se hace un drop en uno de ellos, ejecutamos las anima-
            //ciones de salida e interrumpimos el hilo. Si hacemos el drop en uno de ellos, el drop
            //no es válido y revertimos el drag.
            DragEvent.ACTION_DRAG_EXITED -> {
                threadInterrupted = true
                launchExitAnimations(v)
            }

            DragEvent.ACTION_DROP -> {
                threadInterrupted = true
                Thread{
                    Repository.newInstance(context!!.applicationContext)!!.revertLastDrag()
                }
                launchExitAnimations(v)
                Toast.makeText(context, "Has soltado el icono en una zona no válida", Toast.LENGTH_LONG).show()
                //Nos comunicamos con la actividad contenedora para ejecutar las operaciones necesarias
                //si se hace un drop en uno de los pageChangers
                listener!!.onMainFragmentInteraction(ON_ICON_ATTACHED)
            }

            //Detectamos también cuando termina el drag. Si está en uno de los extremos es necesario
            //animar los pageChangers a su posición "inicial". Si no cuando se vuelvan a cargar, la
            //posición de los mismos estará descoordinada.
            DragEvent.ACTION_DRAG_ENDED -> {
                threadInterrupted = true
                if (viewModel.page == nPages) {
                    setChangerAnimator(leftChangeListenerLayout, rightChangeListenerLayout, 1f, 0f)
                } else if (viewModel.page == 0) {
                    setChangerAnimator(rightChangeListenerLayout, leftChangeListenerLayout, 1f, 0f)
                }
            }

        }
        return true
    }

    private fun launchExitAnimations(v: View) {
        v.startAnimation(AnimationUtils.loadAnimation(context!!.applicationContext, R.anim.on_drag_unselect))
        if (onEdgeAnimationsEnabled) {
            when (v.id) {
                R.id.left_change_listener -> {
                    setChangerAnimator(leftChangeListenerLayout, rightChangeListenerLayout, 1f, 0f)
                    if (viewModel.page == 0) { //Si está en el extremo inicial escondemos el changer izquierdo y desactivamos las animaciones
                        setChangerAnimator(rightChangeListenerLayout, leftChangeListenerLayout, 2f, -resources.getDimension(R.dimen.changer_width))
                        onEdgeAnimationsEnabled = false
                    }
                }
                R.id.right_change_listener -> {
                    setChangerAnimator(rightChangeListenerLayout, leftChangeListenerLayout, 1f, 0f)
                    if (viewModel.page == pager.adapter!!.count - 1) { //Ídem para este lado
                        setChangerAnimator(leftChangeListenerLayout, rightChangeListenerLayout, 2f, resources.getDimension(R.dimen.changer_width))
                        onEdgeAnimationsEnabled = false
                    }
                }
            }
        }
    }

    /**
     * Configura los parámetros necesarios para visualizar los pageChangers
     */
    private fun configurePageChangeListenerLayout(leftChangeListenerLayout: LinearLayout, leftWidth: Int, rightChangeListenerLayout: LinearLayout, rightWidth: Int) {

        val rightLayoutParams = ConstraintLayout.LayoutParams(rightWidth, ConstraintLayout.LayoutParams.MATCH_PARENT)
        rightLayoutParams.startToEnd = R.id.pager_container
        rightLayoutParams.bottomToBottom = R.id.principal
        rightLayoutParams.endToEnd = R.id.principal
        rightLayoutParams.leftMargin = resources.getDimension(R.dimen.changer_margin).toInt()
        rightChangeListenerLayout.layoutParams = rightLayoutParams
        rightChangeListenerLayout.setBackgroundColor(ContextCompat.getColor(context!!, R.color.blackHighAlpha))

        val leftLayoutParams = ConstraintLayout.LayoutParams(leftWidth, ConstraintLayout.LayoutParams.MATCH_PARENT)
        leftLayoutParams.endToStart = R.id.pager_container
        leftLayoutParams.bottomToBottom = R.id.principal
        leftLayoutParams.startToStart = R.id.principal
        leftLayoutParams.rightMargin = resources.getDimension(R.dimen.changer_margin).toInt()
        leftChangeListenerLayout.layoutParams = leftLayoutParams
        leftChangeListenerLayout.setBackgroundColor(ContextCompat.getColor(context!!, R.color.blackHighAlpha))
    }

    /**
     * Configura tres [Animator] para los pageChangers y el [ViewPager]
     */
    private fun setChangerAnimator(scaledView: View, movedView: View, scale: Float, translation: Float) {
        //Agrandamos o reducimos uno de los changers
        val scaledAnimator = if (scale == 0.5f) {
            ObjectAnimator.ofFloat(scaledView, "translationX", translation)
        } else {
            ObjectAnimator.ofFloat(scaledView, "scaleX", scale)
        }
        val pagerAnimator = ObjectAnimator.ofFloat(pagerContainer, "translationX", translation)
        val movedAnimator = ObjectAnimator.ofFloat(movedView, "translationX", translation) //Movemos el otro changer y el pager
        scaledAnimator.duration = CHANGER_ANIMATION_DURATION
        pagerAnimator.duration = CHANGER_ANIMATION_DURATION
        movedAnimator.duration = CHANGER_ANIMATION_DURATION
        scaledAnimator.interpolator = AccelerateInterpolator()
        pagerAnimator.interpolator = AccelerateInterpolator()
        movedAnimator.interpolator = AccelerateInterpolator()
        val animator = AnimatorSet()
        animator.playTogether(scaledAnimator, pagerAnimator, movedAnimator)
        animator.start()
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnMainFragmentInteractionListener {
        fun onMainFragmentInteraction(action: Int)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param iconWidth The width on pixels of each cell.
         * @param iconHeight The height on pixels of each cell.
         * @param numberOfColumns The number of columns selected.
         * @param numberOfRows The number of columns in function of the cell size and the screen height
         * @return A new instance of fragment MainFragment.
         */
        @JvmStatic
        fun newInstance(iconWidth: Int, iconHeight: Int, numberOfColumns: Int, numberOfRows: Int) =
                MainFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_PARAM1, iconWidth)
                        putInt(ARG_PARAM2, iconHeight)
                        putInt(ARG_PARAM3, numberOfColumns)
                        putInt(ARG_PARAM4, numberOfRows)
                    }
                }
    }

}
