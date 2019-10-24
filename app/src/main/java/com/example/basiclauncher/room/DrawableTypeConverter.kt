package com.example.basiclauncher.room

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream

class DrawableTypeConverter {
    companion object {
        private val byteArrayOutputStream = ByteArrayOutputStream()
        @TypeConverter
        @JvmStatic
        fun fromBitmap(value: Bitmap): String {
            value.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val string = byteArrayOutputStream.toByteArray()
            byteArrayOutputStream.reset()
            return Base64.encodeToString(string, Base64.DEFAULT)
        }

        @TypeConverter
        @JvmStatic
        fun stringToBitmap(value: String): Bitmap {
            val bytes = Base64.decode(value, 0)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
}
