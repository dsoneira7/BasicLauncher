package com.example.basiclauncher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.RippleDrawable;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import android.graphics.Color;
import android.graphics.drawable.LayerDrawable;
import com.example.basiclauncher.classes.AppIcon;

import java.lang.reflect.Array;
import java.util.*;

public class SerializeManagerJava {
    private static final SerializeManagerJava ourInstance = new SerializeManagerJava();

    public static SerializeManagerJava getInstance() {
        return ourInstance;
    }

    private SerializeManagerJava() {
    }

    public static void saveSerializable(Context context, GridLayout objectToSave, String fileName){
        Kryo kryo = new Kryo();

        kryo.register(Button.class);

        for(Class<?> i : new View(context).getClass().getDeclaredClasses()){
            if(i.getCanonicalName() != null && i.getCanonicalName().contains("AttachInfo")){
                kryo.register(i);
            }
        }

        kryo.register(GridLayout.class);
        kryo.register(Rect.class);
        kryo.register(Display.class);
        kryo.register(String[].class);
        kryo.register(Configuration.class);
        kryo.register(Locale.class);
        kryo.register(int[].class);
        kryo.register(SparseArray.class);
        kryo.register(Object[].class);
        kryo.register(ArrayList.class);
        kryo.register(Looper.class);
        kryo.register(MessageQueue.class);
        kryo.register(Message.class);

        try {
            kryo.register(Class.forName("android.view.DisplayInfo"));
            kryo.register(Class.forName("android.view.DisplayAdjustments"));
            kryo.register(Class.forName("android.hardware.display.DisplayManagerGlobal"));

            for(Class<?> i : Class.forName("android.hardware.display.DisplayManagerGlobal").getDeclaredClasses()){
                    kryo.register(i);
            }
            for(Class<?> i : Class.forName("android.view.ViewRootImpl").getDeclaredClasses()){
                kryo.register(i);
            }
            for (Class<?> i : Class.forName("android.content.res.CompatibilityInfo").getDeclaredClasses()) {
                kryo.register(i);
            }

            for (Class<?> i : Class.forName("android.app.ActivityThread").getDeclaredClasses()) {
                if(i.getCanonicalName() != null && i.getCanonicalName().contains("Idler")){
                    kryo.register(i);
                }
            }

            for (Class<?> i : Class.forName("androidx.core.content.res.ResourcesCompat$FontCallback").getDeclaredClasses()) {
                Log.d("reflection", i.getCanonicalName());
//                if(i.getCanonicalName() != null && i.getCanonicalName().contains("FontCallback")){
                        kryo.register(i);
                        /*for(Class<?> j : i.getDeclaredClasses()){
                            Log.d("reflection2", j.getCanonicalName());
                            kryo.register(j);
                    }
                }*/
            }

            for (Class<?> i : Class.forName("android.view.Display").getDeclaredClasses()) {
                    kryo.register(Array.newInstance(i, 1).getClass());
                    kryo.register(i);
                    //kryo.register(java.lang.reflect.Array.newInstance(i)::class.java)

            }
        }
        catch(ClassNotFoundException e ){
            e.printStackTrace();
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
        try {
            FileOutputStream fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            Output output = new Output(fileOutputStream);
            kryo.writeObject(output, objectToSave);
            output.close();
            fileOutputStream.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static GridLayout readSerializable(Context context,String  fileName){
        Kryo kryo = new Kryo();
        GridLayout objectToReturn = new GridLayout(context);
        try{FileInputStream fileInputStream = context.openFileInput(fileName);
        Input input = new Input(fileInputStream);
        objectToReturn = kryo.readObject(input, GridLayout.class);

        input.close();
        fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return objectToReturn;
    }
}
