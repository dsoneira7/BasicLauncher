package com.example.basiclauncher.fragments


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.basiclauncher.ON_SETTINGS_CLICK
import com.example.basiclauncher.ON_WALLPAPER_CLICK
import com.example.basiclauncher.ON_WIDGET_CLICK
import com.example.basiclauncher.R
import kotlinx.android.synthetic.main.fragment_settings.*


/**
 * Subclase de [Fragment] que solo contiene unas [ImageView] con callbacks para entrar en ajustes
 * básicos.
 *
 * Se debe utilizar el método factoría [newInstance] para crear una instancia. La actividad conte-
 * nedora debe implementar [OnSettingsFragmentInteractionListener] para la comunicación.
 */
class SettingsFragment : Fragment() {

    private var listener: OnSettingsFragmentInteractionListener? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    /**
     * En el onStart asignamos los callbacks que interactuan con la Actividad contenedora, que hará,
     * las operaciones pertinentes.
     */
    override fun onStart() {
        wallpaper.setOnClickListener { listener!!.onSettingsFragmentInteraction(ON_WALLPAPER_CLICK) }
        settings.setOnClickListener { listener!!.onSettingsFragmentInteraction(ON_SETTINGS_CLICK) }
        widget.setOnClickListener { listener!!.onSettingsFragmentInteraction(ON_WIDGET_CLICK) }
        super.onStart()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnSettingsFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnSettingsFragmentInteractionListener")
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
    interface OnSettingsFragmentInteractionListener {
        fun onSettingsFragmentInteraction(event: Int)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment UnninstallAndCancelFragment.
         */
        @JvmStatic
        fun newInstance() =
                SettingsFragment().apply {
                }
    }
}
