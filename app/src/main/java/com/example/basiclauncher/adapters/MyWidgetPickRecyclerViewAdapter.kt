package com.example.basiclauncher.adapters

import android.appwidget.AppWidgetProviderInfo
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.basiclauncher.Helper
import com.example.basiclauncher.R
import com.example.basiclauncher.fragments.WidgetPickFragment
import kotlinx.android.synthetic.main.fragment_widget_pick.view.*
import android.graphics.Color
import android.os.Build
import com.example.basiclauncher.ON_MAIN_MENU_HOLD


class MyWidgetPickRecyclerViewAdapter(
        private val mValues: List<AppWidgetProviderInfo>,
        private val mListener: WidgetPickFragment.OnWidgetPickFragmentInteractionListener?,
        private val context : Context,
        private val screen_width : Int
) : RecyclerView.Adapter<MyWidgetPickRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnLongClickListener = View.OnLongClickListener {
        val item = ClipData.Item(it.tag as CharSequence)
        val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
        val dragData = ClipData(it.tag.toString(), mimeTypes, item)

        val shadow = View.DragShadowBuilder(it)

        mListener!!.onWidgetPickFragmentInteraction()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            it.startDragAndDrop(dragData, shadow, null, 0)
        } else {
            it.startDrag(dragData, shadow, null, 0)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_widget_pick, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        val size = Helper.getGridSizeOfWidget(item,context,screen_width)
        holder.image.setImageDrawable(item.loadPreviewImage(context, DisplayMetrics.DENSITY_MEDIUM))
        holder.text.text = item.loadLabel(context.packageManager) + size[0] + "x" + size[1]
        holder.text.setBackgroundColor(Color.WHITE)
        holder.text.background.alpha = 200
        with(holder.mView) {
            //image.tag = item.
            image.setOnLongClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val image : ImageView = mView.image
        val text: TextView = mView.text
    }
}
