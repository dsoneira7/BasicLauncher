package com.example.basiclauncher.fragments

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.example.basiclauncher.Helper
import com.example.basiclauncher.R
import com.example.basiclauncher.SMALLER_MAIN_FRAGMENT_TAG
import com.example.basiclauncher.SMALL_MAIN_FRAGMENT_TAG
import com.example.basiclauncher.adapters.IS_NORMAL
import com.example.basiclauncher.adapters.IS_SMALL
import com.example.basiclauncher.adapters.IS_SMALLER
import com.example.basiclauncher.adapters.ScreenSlidePagerAdapter
import com.example.basiclauncher.viewmodels.MainFragmentViewModel
import kotlinx.android.synthetic.main.fragment_main.*


// TODO: Poñer constantes en sitio común
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"
private const val ARG_PARAM4 = "param4"
const val APP_DRAWER_FRAGMENT_TAG = "appDrawerFragment"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MainFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MainFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MainFragment : Fragment(), View.OnDragListener, ViewPager.OnPageChangeListener {

    private var iconWidth: Int = 0
    private var iconHeight: Int = 0
    private var numberOfColumns: Int = 0
    private var numberOfRows: Int = 0
    private var listener: OnMainFragmentInteractionListener? = null
    private lateinit var viewModel: MainFragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            iconWidth = it.getInt(ARG_PARAM1)
            iconHeight = it.getInt(ARG_PARAM2)
            numberOfColumns = it.getInt(ARG_PARAM3)
            numberOfRows = it.getInt(ARG_PARAM4)
        }
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)

        if (childFragment is ScreenSlidePagerFragment) {
            childFragment.attachListeners(
                    { listener!!.onMainFragmentInteraction(it) },
                    {
                        pager.setCurrentItem(it, true)
                    }
            )
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        viewModel = ViewModelProviders.of(this.activity!!).get(MainFragmentViewModel::class.java)

        var isSmall = IS_NORMAL
        if (tag.equals(SMALL_MAIN_FRAGMENT_TAG)) {
            isSmall = IS_SMALL
            viewModel.smallIconHeight = iconHeight
            viewModel.smallIconWidth = iconWidth.toFloat()
        } else if (tag.equals(SMALLER_MAIN_FRAGMENT_TAG)) {
            isSmall = IS_SMALLER
            viewModel.smallerIconHeight = iconHeight
            viewModel.smallerIconWidth = iconWidth.toFloat()
        } else {
            viewModel.iconsPerColumn = numberOfRows
            viewModel.iconsPerRow = numberOfColumns
            viewModel.iconWidth = iconWidth
            viewModel.iconHeight = iconHeight
        }

        var nPages = Helper.getFromSharedPreferences(this.activity!!.packageName,
                "nPages", "0", this.activity!!.applicationContext)
        if (nPages == "0") {
            Helper.putInSharedPreferences(this.activity!!.packageName, "nPages", "1", this.activity!!.applicationContext)
            nPages = "1"
        }
        val pagerAdapter = if (tag.equals(SMALL_MAIN_FRAGMENT_TAG)) {
            ScreenSlidePagerAdapter(childFragmentManager, iconWidth, isSmall, nPages!!.toInt() + 1)
        } else {
            ScreenSlidePagerAdapter(childFragmentManager, iconWidth, isSmall, nPages!!.toInt())
        }
        val pager = view.findViewById<ViewPager>(R.id.pager)
        pager.offscreenPageLimit = nPages.toInt()
        pager.adapter = pagerAdapter
        if (this.tag.equals(SMALL_MAIN_FRAGMENT_TAG)) {
            pager.clipToPadding = false
            pager.pageMargin = 12

        } else if (tag.equals(SMALLER_MAIN_FRAGMENT_TAG)) {
            pager.setPadding(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70f, resources.displayMetrics).toInt(),
                    0,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35f, resources.displayMetrics).toInt(),
                    0)
            pager.clipToPadding = false
            pager.pageMargin = 35

        } else {
            pager.setPadding(0, 0, 0, 0)
        }
        pager.addOnPageChangeListener(this)
        pager.currentItem = viewModel.page
        return view
    }


    override fun onDrag(p0: View?, p1: DragEvent?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnMainFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }


    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onPageScrollStateChanged(state: Int) {}

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        viewModel.page = position
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
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MainFragment.
         */
        @JvmStatic
        fun newInstance(param1: Int, iconHeight: Int, numberOfColumns: Int, numberOfRows: Int) =
                MainFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_PARAM1, param1)
                        putInt(ARG_PARAM2, iconHeight)
                        putInt(ARG_PARAM3, numberOfColumns)
                        putInt(ARG_PARAM4, numberOfRows)
                    }
                }
    }
}
