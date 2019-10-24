package com.example.basiclauncher.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.example.basiclauncher.R



/**
 * Subclase de [PreferenceFragmentCompat] destinada a mostrar preferencias utilizando la libreria
 * preference
 *
 * Se debe utilizar el método factoría [newInstance] para crear una instancia. La actividad conte-
 */
class SettingsActivityFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
        val seekBar = findPreference<SeekBarPreference>(resources.getString(R.string.icon_size))
        //A la SeekBar se le pone de valor máximo 3. Así va de 0 a 3. Si le sumamos 3 al valor que dea ya tendremos
        //nuestra SeekBar de 3 a 6
        seekBar!!.summary = (seekBar.value + 3).toString() + " iconos por fila"
        seekBar.updatesContinuously = true
        seekBar.setOnPreferenceChangeListener { preference, entry ->
            preference.summary = ((entry as Int) + 3).toString() + " iconos por fila"
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
