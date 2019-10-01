package com.example.basiclauncher.adapters

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
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
import kotlin.collections.ArrayList


class AppDrawerAdapter(
        val mContext: Context,
        var data: ArrayList<AppIcon>,
        var onHoldListener: () -> Unit
) : BaseAdapter() {

    private val comparator = Comparator<AppIcon> { a, b ->
        var aNameToCompare: String = if (a.appName == "") {
            a.packageName
        } else {
            a.appName
        }
        var bNameToCompare: String = if (b.appName == "") {
            b.packageName
        } else {
            b.appName
        }
        aNameToCompare.compareTo(bNameToCompare,true)
    }

    init{
        Collections.sort(data, comparator)
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
        var view2 = view
        if (view2 == null) {
            val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view2 = inflater.inflate(R.layout.item, null)
        }
        view2!!.tag = data[position].packageName
        view2.findViewById<ImageView>(R.id.app_image)?.setImageDrawable(BitmapDrawable(mContext.resources, data[position].icon))
        var name = data[position].appName
        if (name.length > 16) {
            name = name.substring(0, 13) + "..."
        }

        view2.findViewById<TextView>(R.id.app_name)?.text = name
        Log.d("debug", data[position].packageName)

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
