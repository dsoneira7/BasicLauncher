package com.example.basiclauncher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.basiclauncher.fragments.SettingsActivityFragment

const val SETTINGS_ACTIVITY_FRAGMENT_TAG = "SettingsActivityFragment"

/**
 * Actividad de ajustes de la aplicación
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.show()
        supportFragmentManager
                .beginTransaction()
                .add(
                        R.id.constraint_layout_settings,
                        SettingsActivityFragment.newInstance(),
                        SETTINGS_ACTIVITY_FRAGMENT_TAG
                ).commit()
    }

    /**
     * Cuando se presiona el botón atrás se retorna a la actividad principal con el resultado OK,
     * para que se lleven a cabo las operaciones necesarias según los cambios que se hayan hecho
     */
    override fun onBackPressed() {
        setResult(RESULT_OK)
        finish()
    }
}
