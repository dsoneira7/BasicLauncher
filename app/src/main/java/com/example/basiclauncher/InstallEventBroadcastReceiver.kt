package com.example.basiclauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class InstallEventBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, p1: Intent?) {
        val repository = Repository.newInstance(context!!.applicationContext)
        repository!!.updateAppList()
    }
}