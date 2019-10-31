package com.example.basiclauncher.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.example.basiclauncher.fragments.ScreenSlidePagerFragment

const val IS_NORMAL = 0
const val IS_SMALL = 1
const val IS_SMALLER = 2

/**
 * Adaptador utilizado para enlazar los datos de cada una de las páginas del [ViewPager], compuestas
 * cada una por un [ScreenSlidePagerFragment]
 *
 * @param supportFragmentManager: El [FragmentManager], utilizado para operar con los fragmentos.
 * @param isSmallFragment: Parámetro que indica si este [ViewPager] es normal, medio o pequeño,
 *      correspondiéndose con las constantes declaradas en esta misma clase (IS_NORMAL, IS_SMALL,
 *      IS_SMALLER)
 * @param nPages: El número de páginas que tiene el [ViewPager]
 */
class ScreenSlidePagerAdapter constructor(
        supportFragmentManager: FragmentManager,
        private val isSmallFragment: Int,
        private var nPages: Int
) : FragmentStatePagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    /**
     * Crea una nueva instancia de [ScreenSlidePagerFragment] para cada página y le pasa el número
     * de la página con la que se corresponde y la constante indicadora de si el fragment es grande
     * o no.
     */
    override fun getItem(position: Int): Fragment {
        val fragment = ScreenSlidePagerFragment.newInstance(position, isSmallFragment)
        return fragment
    }

    /**
     * Añade una nueva página
     */
    fun addNewItem(){
        nPages++
        notifyDataSetChanged()
    }

    /**
     * Borra la última página
     */
    fun deleteItem(){
        nPages--
        notifyDataSetChanged()
    }


    override fun getPageWidth(position: Int): Float = 1f

    override fun getCount(): Int = nPages


}

