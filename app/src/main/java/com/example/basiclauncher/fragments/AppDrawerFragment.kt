package com.example.basiclauncher.fragments

import android.content.Context
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.basiclauncher.R
import com.example.basiclauncher.adapters.AppDrawerAdapter
import com.example.basiclauncher.classes.AppIcon
import com.example.basiclauncher.viewmodels.AppDrawerViewModel
import kotlinx.android.synthetic.main.app_drawer_fragment.*

/**
 * Subclase de [Fragment] que se contiene un [GridView] con las aplicaciones del sistema.
 *
 * Se debe utilizar el método factoría [newInstance] para crear una instancia. La actividad conte-
 * nedora debe implementar [OnFragmentInteractionListener] para la comunicación.
 */
class AppDrawerFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null

    companion object {
        fun newInstance() = AppDrawerFragment()
    }

    private lateinit var viewModel: AppDrawerViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.app_drawer_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this.activity!!).get(AppDrawerViewModel::class.java)
        gridview.setBackgroundColor(ContextCompat.getColor(context!!, R.color.whiteLowAlpha)) //Leve color blanco sombreado de fondo

        //Observamos el listado de aplicaciones del sistema. Si hay algún cambio (instalación o desinstalación) pasamos el adaptador nuevo.
        viewModel.appLiveData.observe(this, Observer<Array<AppIcon>> {
            gridview.adapter = AppDrawerAdapter(context!!, viewModel.appLiveData.value!!) { onHoldAppIcon() }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction()
    }

    /**
     * Método que se pasa por parámetros al adaptador para hacer un Callback a la activity cuando
     * sea necesario hacer un cambio de fragmentos.
     */
    private fun onHoldAppIcon() {
        listener!!.onFragmentInteraction()
    }


}
