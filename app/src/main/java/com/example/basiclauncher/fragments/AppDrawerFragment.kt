package com.example.basiclauncher.fragments

import android.content.Context
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.example.basiclauncher.adapters.AppDrawerAdapter

import com.example.basiclauncher.R
import com.example.basiclauncher.classes.AppIcon
import com.example.basiclauncher.viewmodels.AppDrawerViewModel
import kotlinx.android.synthetic.main.app_drawer_fragment.*

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
        if(viewModel.appArray.value != null){
            gridview.adapter = AppDrawerAdapter(context!!, viewModel.appArray.value!!) {onHoldAppIcon()}
        }
        viewModel.appArray.observe(this, Observer<ArrayList<AppIcon>>{
            gridview.adapter = AppDrawerAdapter(context!!, viewModel.appArray.value!!) {onHoldAppIcon()}
        })//todo: optimizar con DiffUtil
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

    fun onHoldAppIcon(){
            listener!!.onFragmentInteraction()
    }



}
