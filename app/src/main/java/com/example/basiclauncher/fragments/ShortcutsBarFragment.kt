package com.example.basiclauncher.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.basiclauncher.CustomLinearLayout
import com.example.basiclauncher.OPEN_APP_DRAWER
import com.example.basiclauncher.R
import com.example.basiclauncher.viewmodels.ScreenSlidePagerViewModel
import com.example.basiclauncher.viewmodels.ScreenSlidePagerViewModelFactory
import kotlinx.android.synthetic.main.fragment_shortcuts_bar.*

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ShortcutsBarFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ShortcutsBarFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */

const val SHORTCUTS_BAR_PAGE = -1

class ShortcutsBarFragment : Fragment() {

    private var listener: OnShortcutsBarFragmentInteractionListener? = null

    //the data of the shortcutBarFragment
    //is treated as if it were another page
    //page from the ViewPager
    private lateinit var viewModel: ScreenSlidePagerViewModel
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_shortcuts_bar, container, false)
        viewModel = ViewModelProviders.of(this, ScreenSlidePagerViewModelFactory(activity!!.application, SHORTCUTS_BAR_PAGE)).get(ScreenSlidePagerViewModel::class.java)
        view.setOnTouchListener { view, motionEvent -> dragDrawer(view, motionEvent) }
        for (i in 0 until 4) {
            val frameLayout = when (i) {
                0 -> view.findViewById<FrameLayout>(R.id.container_shortcut_1)
                1 -> view.findViewById(R.id.container_shortcut_2)
                2 -> view.findViewById(R.id.container_shortcut_3)
                else -> view.findViewById(R.id.container_shortcut_4)
            }
            val linearLayout = CustomLinearLayout(context, SHORTCUTS_BAR_PAGE, i)
            if(viewModel.stateList.value != null) {
            try{
                var state = viewModel.stateList.value!![i]

                if (state != null && state.appId != -1) {
                    linearLayout.setApp(viewModel.appList.value!!.get(i).packageName)
                }
            }
            catch(e: ArrayIndexOutOfBoundsException){

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

    fun dragDrawer(view: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP &&
                event.y < frame_layout_shortcuts_bar.y) {
            listener!!.onShortcutsBarFragmentInteraction(OPEN_APP_DRAWER)
        }
        return true
    }

    private fun onIconAttached(packageName: String, page: Int, position: Int) {
        if (packageName == "") {
            Thread {
                viewModel.emptyState(page,position)
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
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /*override fun onDrag(view: View?, event: DragEvent?): Boolean {
        when(event!!.action) {
            DragEvent.ACTION_DRAG_ENTERED -> {
                view!!.setBackgroundColor(Color.LTGRAY)
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                view!!.setBackgroundColor(Color.TRANSPARENT)
            }
            DragEvent.ACTION_DROP ->{
                view!!.setBackgroundColor(Color.TRANSPARENT)
                if(view.equals(container_invalidate)){
                    listener!!.onUnninstallAndCancelFragmentInteraction(ON_ANULATE, "")
                }else {
                    listener!!.onUnninstallAndCancelFragmentInteraction(ON_UNNINSTALL, event.clipData.getItemAt(0).text as String)
                }
            }
        }
        return true
    }*/


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
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment UnninstallAndCancelFragment.
         */
        @JvmStatic
        fun newInstance() = ShortcutsBarFragment()
    }
}