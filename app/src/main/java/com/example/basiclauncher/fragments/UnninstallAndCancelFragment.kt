package com.example.basiclauncher.fragments


import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.basiclauncher.ON_ANULATE
import com.example.basiclauncher.ON_UNNINSTALL
import com.example.basiclauncher.R
import com.example.basiclauncher.Repository
import kotlinx.android.synthetic.main.fragment_unninstall_and_cancel.*


/**
 * Subclase de [Fragment] que contiene un par de contenedores, los que se utilizan para que, al
 * droppear un icono en ellos durante el drag, se desinstale la app correspondiente o se anule la
 * operación.
 *
 * Se debe utilizar el método factoría [newInstance] para crear una instancia. La actividad conte-
 * nedora debe implementar [OnUnninstallAndCancelFragmentInteractionListener] para la comunicación.
 */


class UnninstallAndCancelFragment : Fragment(), View.OnDragListener {

    private var listener: OnUnninstallAndCancelFragmentInteractionListener? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_unninstall_and_cancel, container, false)
        //Metemos un filtro sombreado grisáceo en lso contenedores
        view.findViewById<FrameLayout>(R.id.container_unninstall_background).background = ColorDrawable(ContextCompat.getColor(context!!, R.color.blackHighAlpha))
        view.findViewById<FrameLayout>(R.id.container_invalidate_background).background = ColorDrawable(ContextCompat.getColor(context!!, R.color.blackHighAlpha))
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
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDrag(view: View?, event: DragEvent?): Boolean {
        when (event!!.action) {
            //Si etnramos o salimos de los contenedores realizamos animaciones que le den un poco
            //de gracia e intuitividad.
            DragEvent.ACTION_DRAG_ENTERED -> {
                when (view!!.id) {
                    R.id.container_invalidate -> container_invalidate_background.startAnimation(AnimationUtils.loadAnimation(context!!.applicationContext, R.anim.on_drag_select))
                    R.id.container_unnistall -> container_unninstall_background.startAnimation(AnimationUtils.loadAnimation(context!!.applicationContext, R.anim.on_drag_select))
                }
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                when (view!!.id) {
                    R.id.container_invalidate -> container_invalidate_background.startAnimation(AnimationUtils.loadAnimation(context!!.applicationContext, R.anim.on_drag_unselect))
                    R.id.container_unnistall -> container_unninstall_background.startAnimation(AnimationUtils.loadAnimation(context!!.applicationContext, R.anim.on_drag_unselect))
                }
            }
            //Cuando se droppea se realizan las animaciones pertinentes y se avisa a la actividad
            //contenedora para que haga las transacciones de fragmentos y las operaciones necesarias.
            DragEvent.ACTION_DROP -> {
                when (view!!.id) {
                    R.id.container_invalidate -> container_invalidate_background.startAnimation(AnimationUtils.loadAnimation(context!!.applicationContext, R.anim.on_drag_unselect))
                    R.id.container_unnistall -> container_unninstall_background.startAnimation(AnimationUtils.loadAnimation(context!!.applicationContext, R.anim.on_drag_unselect))
                }
                Thread {
                    //Adicionalmente, si es necesario actualizar numero de paginas, iconos, etc,
                    //se realiza en este momento a traves de la clase Repository
                    Repository.newInstance(context!!.applicationContext)!!.updateIfNecessary()
                }.start()
                if (view == container_invalidate) {
                        listener!!.onUnninstallAndCancelFragmentInteraction(ON_ANULATE, "")
                    } else {
                        listener!!.onUnninstallAndCancelFragmentInteraction(ON_UNNINSTALL, (event.clipData.getItemAt(0).text as String).substringBefore(";"))
                    }

            }
            DragEvent.ACTION_DRAG_ENDED -> {
                //Adicionalmente el contenedor de invalidar, se utiliza también para escuchar si la
                //operación drag ha salido mal. Si el resultado ha sido erróneo o se suelta en una zona
                //no válida, se avisa al usuario y se revierte el drag.
                if (view!!.id == R.id.container_invalidate && !event.result) {
                    Toast.makeText(context, "Has soltado el icono en una zona no habilitada.", Toast.LENGTH_LONG).show()
                    Thread {
                        Repository.newInstance(context!!.applicationContext)!!.revertLastDrag()
                    }.start()
                    listener!!.onUnninstallAndCancelFragmentInteraction(ON_ANULATE, "")
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
         * @return A new instance of fragment UnninstallAndCancelFragment.
         */
        @JvmStatic
        fun newInstance() =
                UnninstallAndCancelFragment().apply {
                }
    }
}
