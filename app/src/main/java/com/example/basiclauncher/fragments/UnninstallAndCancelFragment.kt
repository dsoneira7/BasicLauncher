package com.example.basiclauncher.fragments


import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.basiclauncher.ON_ANULATE
import com.example.basiclauncher.ON_UNNINSTALL
import com.example.basiclauncher.R
import kotlinx.android.synthetic.main.fragment_unninstall_and_cancel.*

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [UnninstallAndCancelFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [UnninstallAndCancelFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class UnninstallAndCancelFragment : Fragment(), View.OnDragListener {

    private var listener: OnUnninstallAndCancelFragmentInteractionListener? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_unninstall_and_cancel, container, false)
/*        view.findViewById<LinearLayout>(R.id.container_invalidate).setOnDragListener(this)
        view.findViewById<LinearLayout>(R.id.container_unnistall).setOnDragListener(this)*/
        return view
    }

    override fun onStart() {
        container_invalidate.setOnDragListener(this)
        container_unnistall.setOnDragListener(this)
        super.onStart()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnUnninstallAndCancelFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDrag(view: View?, event: DragEvent?): Boolean {
        when (event!!.action) {
            DragEvent.ACTION_DRAG_ENTERED -> {
                view!!.setBackgroundColor(Color.LTGRAY)
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                view!!.setBackgroundColor(Color.TRANSPARENT)
            }
            DragEvent.ACTION_DROP -> {
                view!!.setBackgroundColor(Color.TRANSPARENT)
                if (view.equals(container_invalidate)) {
                    listener!!.onUnninstallAndCancelFragmentInteraction(ON_ANULATE, "")
                } else {
                    listener!!.onUnninstallAndCancelFragmentInteraction(ON_UNNINSTALL, event.clipData.getItemAt(0).text as String)
                }
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                if (view!!.equals(container_invalidate)) {
                    if (!event.result) {
                        Toast.makeText(context, "Has soltado el icono en una zona no habilitada.", Toast.LENGTH_LONG).show()
                        listener!!.onUnninstallAndCancelFragmentInteraction(ON_ANULATE, "")
                    }
                }
            }
        }
        return true
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
    interface OnUnninstallAndCancelFragmentInteractionListener {
        fun onUnninstallAndCancelFragmentInteraction(event: Int, app: String)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment UnninstallAndCancelFragment.
         */
        @JvmStatic
        fun newInstance() =
                UnninstallAndCancelFragment().apply {
                }
    }
}
