package com.example.basiclauncher.fragments

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.basiclauncher.R
import com.example.basiclauncher.adapters.MyWidgetPickRecyclerViewAdapter

const val SCREEN_WIDTH = "param2"

/**
 * Este fragmento se utilizaba para mostrar la lista de widgets disponibles en el sistema.
 * Simplemente contenía un [RecyclerView] que listaba todos los widgets y permitía seleccionarlos
 * al mantener pulsado.
 */
class WidgetPickFragment : Fragment() {

    private var screenWidth = 0
    private var listener: OnWidgetPickFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenWidth = arguments!!.getInt(SCREEN_WIDTH)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_widget_pick_list, container, false)
        val list = view.findViewById<RecyclerView>(R.id.list)
        list!!.layoutManager = LinearLayoutManager(context)
        list.setBackgroundColor(Color.BLACK)
        list.background.alpha = 127
        list.setHasFixedSize(true)
        val widgetList = AppWidgetManager.getInstance(activity!!.applicationContext).installedProviders
        list.adapter = MyWidgetPickRecyclerViewAdapter(widgetList, listener, activity!!.applicationContext, screenWidth)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnWidgetPickFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnWidgetPickFragmentInteractionListener")
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
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnWidgetPickFragmentInteractionListener {
        fun onWidgetPickFragmentInteraction()
    }

    companion object {
        @JvmStatic
        fun newInstance(screenWidth: Int) = WidgetPickFragment().apply {
            arguments = Bundle().apply {
                this.putInt(SCREEN_WIDTH, screenWidth)
            }
        }
    }
}
