package com.example.basiclauncher.fragments

import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.basiclauncher.*
import com.example.basiclauncher.adapters.IS_NORMAL
import com.example.basiclauncher.adapters.IS_SMALL
import com.example.basiclauncher.adapters.IS_SMALLER
import com.example.basiclauncher.classes.CustomLinearLayoutState
import com.example.basiclauncher.viewmodels.MainFragmentViewModel
import com.example.basiclauncher.viewmodels.ScreenSlidePagerViewModel
import com.example.basiclauncher.viewmodels.ScreenSlidePagerViewModelFactory
import kotlinx.android.synthetic.main.viewpagerfragment.*

const val LONG_HOLD_VALUE: Long = 1000

class ScreenSlidePagerFragment : Fragment() {

    companion object {
        fun newInstance() = ScreenSlidePagerFragment()
    }

    private lateinit var onIconAttachedListener: (Int) -> Unit
    private lateinit var onPageChangeListener: (Int) -> Unit

    private lateinit var viewModel: ScreenSlidePagerViewModel
    private lateinit var mainFragmentViewModel: MainFragmentViewModel
    private var iconWidth = 0
    private var iconHeight = 0
    private var page = 0
    private var isSmallFragment = 0
    private var nIcons = 0
    private var interrupted = false

    private lateinit var pageGridData: MutableLiveData<Array<CustomLinearLayoutState>>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iconWidth = arguments?.getInt("iconSize", iconWidth)!!  //todo:innecesario
        page = arguments?.getInt("position", page)!!
        isSmallFragment = arguments?.getInt("isSmallFragment", isSmallFragment)!! //todo: Optimize
        mainFragmentViewModel = ViewModelProviders.of(this.activity!!).get(MainFragmentViewModel::class.java)
        viewModel = ViewModelProviders.of(this, ScreenSlidePagerViewModelFactory(activity!!.application, page)).get(ScreenSlidePagerViewModel::class.java)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.viewpagerfragment, container, false)


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        iconHeight = when (isSmallFragment) {
            IS_NORMAL -> mainFragmentViewModel.iconHeight
            IS_SMALL -> mainFragmentViewModel.smallIconHeight
            IS_SMALLER -> mainFragmentViewModel.smallerIconHeight
            else -> Log.d("debug", "ERROR") //todo
        }

        nIcons = mainFragmentViewModel.iconsPerColumn * mainFragmentViewModel.iconsPerRow
        gridlayout.columnCount = mainFragmentViewModel.iconsPerRow
        gridlayout.rowCount = mainFragmentViewModel.iconsPerColumn

        gridConfiguration()

        gridlayout.setOnDragListener { view, dragEvent -> (customOnDragListener(view, dragEvent)) }
        if (isSmallFragment == IS_NORMAL) {
            viewModel.stateList.observe(this, Observer {
                Log.d("debug", "Observado un cambio en pagina $page")
                for (i in it) {
                    Log.d("ScreenSlidePagerFragmen", "States: " + i.page + " position: " + i.position)
                    val cell = view!!.findViewById<CustomLinearLayout>((page + 1) * (i.position + 1))
                    if (cell == null) {
                        Log.d("ERROR", "Cell not found")
                    } else if ((cell.isEmpty() || cell.getAppId() != i.appId) && viewModel.appList.value!!.get(i.appId) != null) {
                        cell.setApp(viewModel.appList.value!!.get(i.appId))
                    }
                }
            })
        }
    }

    private fun gridConfiguration() {
        val arrayOfStates = arrayOfNulls<CustomLinearLayoutState>(nIcons)
        Log.d("SCRENSLIDEPAGEFRAGMENT", "arrayOfNulls Done")
        if (viewModel.stateList.value != null) {
            for (i in viewModel.stateList.value!!) {
                if (i.position >= arrayOfStates.size) {
                    Thread {
                        viewModel.emptyState(i.page, i.position)
                    }.start()
                } else {
                    arrayOfStates[i.position] = i
                }
            }
        } else {
            Log.d("debug", "value of LiveData null")
        }
        for (i in 0 until nIcons) {
            val linearLayout = CustomLinearLayout(context, page, i)
            linearLayout.id = (page + 1) * (i + 1) //Elaboramos un appId para cada celda así para que no coincida con ningún otro
            if (isSmallFragment != IS_SMALLER) {
                linearLayout.attachListeners({ p1, p2, p3 -> onIconAttached(p1, p2, p3) },
                        { onPostIconAttached(it) })
            }
            linearLayout.layoutParams = LinearLayout.LayoutParams(
                    iconWidth,
                    iconHeight
            )
            var state: CustomLinearLayoutState? = null
            if (arrayOfStates[i] != null) {
                try {
                    state = arrayOfStates[i]
                } catch (e: IndexOutOfBoundsException) {
                    e.printStackTrace()
                }
            }

            if (state != null && state.appId != -1) { //todo: Optimizar para que solo se cambien los iconos que han cambiado
                linearLayout.setApp(viewModel.appList.value!!.get(state.appId))
            }
            //linearLayout.attachListeners{onIconMoving(it)}
            gridlayout.addView(linearLayout)
        }
        if (isSmallFragment != IS_NORMAL) {
            content.background.alpha = 100
        } else {
            content.background.alpha = 0
        }
        if (isSmallFragment == IS_SMALLER) {
            gridlayout.setOnClickListener {
                onPostIconAttached(ON_GRID_CLICK_FROM_SMALLER_MODE)
            }
        }
    }

    override fun onResume() {
        if (isSmallFragment != IS_NORMAL) {
            content.background.alpha = 100
        } else {
            content.background.alpha = 0
        }
        super.onResume()
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
        if ((page + 1) > Helper.getFromSharedPreferences(this.activity!!.packageName,
                        "nPages", "0", this.activity!!.applicationContext)!!.toInt()) {
            Helper.putInSharedPreferences(this.activity!!.packageName,
                    "nPages", (page + 1).toString(), this.activity!!.applicationContext)
            onIconAttachedListener(PLUS_ONE_PAGE)
        }
        val states = viewModel.stateList.value!!
    }

    private fun onPostIconAttached(event: Int) {
        onIconAttachedListener(event)
    }

    fun attachListeners(onIconAttachedListener: (Int) -> Unit, onPageChangeListener: (Int) -> Unit) {
        this.onIconAttachedListener = onIconAttachedListener
        this.onPageChangeListener = onPageChangeListener
    }

    private fun customOnDragListener(v: View, event: DragEvent): Boolean {
        val longHoldThread = Thread {
            try {
                Thread.sleep(LONG_HOLD_VALUE)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if (!interrupted) {
                v.post {
                    onPageChangeListener(page)
                }
                mainFragmentViewModel.page = page
            }
        }

        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED -> {
                if (mainFragmentViewModel.page != page) {
                    interrupted = false
                    longHoldThread.start()
                }
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                longHoldThread.interrupt()
                interrupted = true
            }

        }
        return true
    }

}
