package com.example.basiclauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * [BroadcastReceiver] que recibe eventos de instalación y desinstalación de paquetes. Su función
 * es avisar a la clase [Repository] de que actualice la BBDD según sea necesario.
 */
class InstallEventBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val repository = Repository.getInstance(context!!.applicationContext)
        repository!!.updateAppList(intent)
    }
}