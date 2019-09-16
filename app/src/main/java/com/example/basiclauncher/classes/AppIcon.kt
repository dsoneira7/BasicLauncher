package com.example.basiclauncher.classes

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.basiclauncher.Helper

@Entity(tableName = "apps")
data class AppIcon(
        var packageName : String,
        var appName : String,
        var icon : Bitmap,
        @PrimaryKey(autoGenerate = true)
        var id: Int? = null
)