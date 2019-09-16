package com.example.basiclauncher

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.RippleDrawable
import android.util.Log
import android.view.Display
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.ref.WeakReference
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import com.example.basiclauncher.classes.AppIcon
import java.util.*

object SerializeManager {

    fun saveSerializable(context: Context, objectToSave: GridLayout, fileName: String){
        val kryo = Kryo()

        kryo.register(Button::class.java)

        for(i in View(context).javaClass.declaredClasses){
            if(i.canonicalName != null && i.canonicalName!!.contains("AttachInfo")){
                kryo.register(i)
            }
        }

        kryo.register(GridLayout::class.java)
        kryo.register(Rect::class.java)
        kryo.register(Display::class.java)
        kryo.register(Array<String>::class.java)
        kryo.register(Configuration::class.java)
        kryo.register(Locale::class.java)
        kryo.register(Class.forName("android.view.DisplayInfo"))
        kryo.register(Class.forName("android.view.DisplayAdjustments"))

        for( i in Class.forName("android.content.res.CompatibilityInfo").declaredClasses){
            kryo.register(i)
        }
        for(i in Class.forName("android.view.Display").declaredClasses){
            if(i.canonicalName != null && i.canonicalName!!.contains("ColorTransform")){
                Log.d("debug", "Reflection: "+i.canonicalName)

                //kryo.register(java.lang.reflect.Array.newInstance(i)::class.java)
            }
        }

        /*kryo.register(RippleDrawable::class.java)
        kryo.register(WeakReference::class.java)

        for(i in RippleDrawable(ColorStateList.valueOf(Color.RED), null, null).javaClass.declaredClasses){
            if(i.canonicalName != null && i.canonicalName!!.contains("RippleState")){
                kryo.register(i)
            }
        }
        for(i in LayerDrawable(emptyArray()).javaClass.declaredClasses){
            if(i.canonicalName != null && i.canonicalName!!.contains("ChildDrawable")){
                val a = Array
                kryo.register()
            }
        }*/

        val fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
        val output = Output(fileOutputStream)
        kryo.writeObject(output, objectToSave)
        output.close()
        fileOutputStream.close()
    }

    fun readSerializable(context:Context, fileName: String): GridLayout{
        val kryo = Kryo()
        kryo.register(GridLayout::class.java)
        kryo.register(View::class.java)
        val fileInputStream = context.openFileInput(fileName)
        val input = Input(fileInputStream)
        val objectToReturn = kryo.readObject(input, GridLayout::class.java)

        input.close()
        fileInputStream.close()
        return objectToReturn
    }
}