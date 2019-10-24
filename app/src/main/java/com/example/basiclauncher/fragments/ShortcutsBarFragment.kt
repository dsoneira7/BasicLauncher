package com.example.basiclauncher.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.basiclauncher.CustomLinearLayout
import com.example.basiclauncher.OPEN_APP_DRAWER
import com.example.basiclauncher.R
import com.example.basiclauncher.classes.CustomLinearLayoutState
import com.example.basiclauncher.viewmodels.ScreenSlidePagerViewModel
import com.example.basiclauncher.viewmodels.ScreenSlidePagerViewModelFactory
import kotlinx.android.synthetic.main.fragment_shortcuts_bar.*


/**
 * Subclase de [Fragment] que se corresponde con la barra de accesos rápidos que se encuentra en la
 * parte inferior de la pantalla. Su estructura y funcionamiento son muy similares a los de un
 * [ScreenSlidePagerFragment]. Adicionalmente esta barra tiene la función de lanzar el app drawer,
 * si se arrastra desde ella hacia arriba.
 * Se debe utilizar el método factoría [newInstance] para crear una instancia. La actividad conte-
 * nedora debe implementar [OnFragmentInteractionListener] para la comunicación.
 */

//La barra de accesos directos se trata como si fuera una página en la posición -1.
const val SHORTCUTS_BAR_PAGE = -1

class ShortcutsBarFragment : Fragment() {

    private var listener: OnShortcutsBarFragmentInteractionListener? = null

    //Los datos de la barra de accesos rápidos
    //son tratados como si fuesen cualquier
    //otra página del viewPager
    private lateinit var viewModel: ScreenSlidePagerViewModel

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_shortcuts_bar, container, false)
        //El ViewModel que utiliza es un ScreenSlidePagerViewModel
        viewModel = ViewModelProviders.of(this, ScreenSlidePagerViewModelFactory(activity!!.application, SHORTCUTS_BAR_PAGE)).get(ScreenSlidePagerViewModel::class.java)
        view.setOnTouchListener { _, motionEvent -> dragDrawer(motionEvent) }
        viewModel.stateList.observe(this, observer)

        //El funcionamiento es totalmente análogo al de ScreenSlidePagerFragment. Solo que en vez
        //de tratar con un número de iconos dinámico trata siempre con 4 que están siempre visibles.
        for (i in 0 until 4) {
            val frameLayout = when (i) {
                0 -> view.findViewById<FrameLayout>(R.id.container_shortcut_1)
                1 -> view.findViewById(R.id.container_shortcut_2)
                2 -> view.findViewById(R.id.container_shortcut_3)
                else -> view.findViewById(R.id.container_shortcut_4)
            }
            val linearLayout = CustomLinearLayout(context, SHORTCUTS_BAR_PAGE, i, true)
            linearLayout.id = (i + 1)
            if (viewModel.stateList.value != null) {
                for (j in viewModel.stateList.value!!) {
                    if (j.position == i && viewModel.appList.value!!.get(j.appId) != null) {
                        linearLayout.setApp(viewModel.appList.value!!.get(j.appId))
                        linearLayout.clearText()
                        break
                    }
                }
            }
            linearLayout.layoutParams = FrameLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            )
            linearLayout.attachListeners({ p1, p2, p3 -> onIconAttached(p1, p2, p3) },
                    { listener!!.onShortcutsBarFragmentInteraction(it) })

            frameLayout.addView(linearLayout)
        }
        return view
    }

    private val observer = Observer<Array<CustomLinearLayoutState>> {
        var contador = 0
        for (i in 0..4) {
            if (contador < it.size && i == it[contador].position) {

                val cell = view!!.findViewById<CustomLinearLayout>((it[contador].position + 1))
                if (cell == null) {
                    Log.d("ERROR", "Cell not found")
                } else if ((cell.isEmpty() || cell.getAppId() != it[contador].appId) && viewModel.appList.value!!.get(it[contador].appId) != null) {
                    cell.setApp(viewModel.appList.value!!.get(it[contador].appId))
                    cell.clearText()
                }
                contador++
            } else {
                val cell = view!!.findViewById<CustomLinearLayout>(i + 1)
                if (cell != null && !cell.isEmpty()) {
                    cell.clear()
                } else {
                    Log.d("ERROR", "Cell not found when trying to clean")
                }
            }
        }
    }

    /**
     * Si arrastramos desde la barra de accesos directos a cualquier lugar que se encuentre más
     * arriba que ésta, lanzamos el drawer de aplicaciones.
     */
    private fun dragDrawer(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP &&
                event.y < frame_layout_shortcuts_bar.y) {
            listener!!.onShortcutsBarFragmentInteraction(OPEN_APP_DRAWER)
        }
        return true
    }



    private fun onIconAttached(packageName: String, page: Int, position: Int) {
        if (packageName == "") {
            Thread {
                viewModel.emptyState(page, position)
            }.start()
            return
        }
        Thread {
            viewModel.stateOccupied(packageName, page, position)
        }.start()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnShortcutsBarFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
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
    interface OnShortcutsBarFragmentInteractionListener {
        fun onShortcutsBarFragmentInteraction(event: Int)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment UnninstallAndCancelFragment.
         */
        @JvmStatic
        fun newInstance() = ShortcutsBarFragment()
    }
}