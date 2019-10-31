package com.example.basiclauncher.adapters

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.basiclauncher.R
import com.example.basiclauncher.classes.AppIcon
import java.util.*
import kotlin.Comparator

/**
 * Subclase de [BaseAdapter] que se utiliza para introducir los datos de los aplicaciones
 * disponibles en la [GridView] del App Drawer
 *
 * @param mContext: El contexto de la aplicación
 * @param data: [ArrayList] de clases [AppIcon] que contiene las aplicaciones para mostrar
 * @param onHoldListener: Método pasado por parámetros dedicado a comunicarle al fragmento contenedor el drag de un icono.
 *
 */
class AppDrawerAdapter(
        private val mContext: Context,
        var data: Array<AppIcon>,
        var onHoldListener: () -> Unit
) : BaseAdapter() {

    //Comparator utilizado para ordenar por orden alfabético según el nombre que se vaya a mostrar.
    //Prioritariamente se muestra el label de la aplicación, pero algunas no tienen.

    override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
        var view2 = view
        if (view2 == null) {
            val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view2 = inflater.inflate(R.layout.item, parent, false)
        }

        //Setteamos icono y nombre
        view2!!.tag = data[position].packageName
        view2.findViewById<ImageView>(R.id.app_image)?.setImageDrawable(BitmapDrawable(mContext.resources, data[position].icon))
        val name = data[position].appName
        view2.findViewById<TextView>(R.id.app_name)?.text = name

        //Si clickamos lanzamos intent de la aplicación
        view2.setOnClickListener {
            val launchIntent: Intent? = mContext.packageManager?.getLaunchIntentForPackage(data[position].packageName)
            if (launchIntent != null) {
                ContextCompat.startActivity(mContext, launchIntent, null)
            }
        }

        view2.setOnLongClickListener {
            onLongClick(it)
        }

        return view2
    }

    override fun getItem(p0: Int): Any {
        return data[p0]
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return data.size
    }

    /**
     * Método destinado a iniciar el drag con los parámetros necesarios (packageName e icono para la
     * sombra de draggeo. También se encarga de avisar al fragment contenedor para llevar a cabo las
     * fragmentTransactions necesarias
     */
    private fun onLongClick(v: View): Boolean {
        val item = ClipData.Item(v.tag as CharSequence)
        val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
        val dragData = ClipData(v.tag.toString(), mimeTypes, item)
        val shadow = View.DragShadowBuilder(v.findViewById(R.id.app_image))
        onHoldListener()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            v.startDragAndDrop(dragData, shadow, null, 0)
        } else {
            v.startDrag(dragData, shadow, null, 0)
        }
        return true
    }


}
