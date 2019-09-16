package com.example.basiclauncher.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.example.basiclauncher.fragments.ScreenSlidePagerFragment
const val IS_NORMAL = 0
const val IS_SMALL = 1
const val IS_SMALLER = 2

class ScreenSlidePagerAdapter constructor(
        private val supportFragmentManager: FragmentManager,
        private val size: Int,
        private val isSmallFragment: Int,
        private val nPages: Int
) : FragmentStatePagerAdapter(supportFragmentManager) {

    override fun getItem(position: Int): Fragment {
        val fragment = ScreenSlidePagerFragment.newInstance()
        val bundle = Bundle()
        bundle.putInt("iconSize", size)
        bundle.putInt("position", position)
        bundle.putInt("isSmallFragment", isSmallFragment)
        fragment.arguments = bundle
        return fragment
    }

    override fun getPageWidth(position: Int): Float {
        return when(isSmallFragment) {
            IS_SMALL -> 0.928f
            IS_SMALLER -> 0.8f
            else -> 1f
        }
    }

    override fun getCount(): Int = nPages


}