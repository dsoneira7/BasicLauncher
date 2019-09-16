package com.example.basiclauncher


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat




/**
 * A simple [Fragment] subclass.
 * Use the [SettingsActivityFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class SettingsActivityFragment :  PreferenceFragmentCompat() {

    /*override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings_activity, container, false)
    }*/

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
        val dropDown = findPreference<DropDownPreference>("dropdown_size")
        dropDown!!.summary = dropDown.entry.toString() + " iconos por fila"
        dropDown.setOnPreferenceChangeListener {preference, entry ->
                preference.summary = entry.toString() + " iconos por fila"
                true
        }
    }



    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment SettingsActivityFragment.
         */
        @JvmStatic
        fun newInstance() = SettingsActivityFragment()
    }
}
