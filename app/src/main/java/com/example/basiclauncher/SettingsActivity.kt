package com.example.basiclauncher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

const val SETTINGS_ACTIVITY_FRAGMENT_TAG = "SettingsActivityFragment"
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.show() //todo: customize
        supportFragmentManager
                .beginTransaction()
                .add(
                        R.id.constraint_layout_settings,
                        SettingsActivityFragment.newInstance(),
                        SETTINGS_ACTIVITY_FRAGMENT_TAG
                ).commit()
    }

}
