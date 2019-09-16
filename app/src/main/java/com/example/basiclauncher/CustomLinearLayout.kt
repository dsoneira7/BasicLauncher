package com.example.basiclauncher

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat.startActivity
import com.example.basiclauncher.classes.AppIcon

class CustomLinearLayout(private val mContext: Context?, private val page: Int, private val position: Int) : LinearLayout(mContext), View.OnDragListener {

    private var empty = true
    private var touched = false
    private lateinit var onIconAttachedListener: (String, Int, Int) -> Unit
    private lateinit var onPostIconAttachedListener: (Int) -> Unit
    private lateinit var app: String
    var appId = -1
    private var icon: ImageView? = null

    init {
        setOnLongClickListener { onLongClick(it) }
        setOnClickListener { onClick() }
        setOnDragListener(this)
    }

    fun isEmpty(): Boolean = empty


    fun attachListeners(onIconAttached: (String, Int, Int) -> Unit, onPostIconAttachedListener: (Int) -> Unit) {
        onIconAttachedListener = onIconAttached
        this.onPostIconAttachedListener = onPostIconAttachedListener
    }

    fun setApp(app: AppIcon){
        appId = app.id!!
        setApp(app.packageName)
    }

    fun setApp(app: String) { //todo: usar icon almacenado en bbdd si se tiene
        this.app = app
        empty = false
        if (icon == null) {
            icon = ImageView(mContext)
            //icon?.id = (page + 1) * (position + 1)
            icon?.tag = app
            icon?.layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            val drawable = Helper.getActivityIcon(mContext!!, app)
            icon?.setImageDrawable(drawable)
            this.addView(icon)
        } else {
        }
    }

    override fun onDrag(view: View?, event: DragEvent?): Boolean {
        when (event?.action) {

            DragEvent.ACTION_DRAG_ENTERED -> {
                Log.d("debug", "entered")
                view?.setBackgroundColor(Color.GREEN)
                view?.background!!.alpha = 140
                view.invalidate()
                return true
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                Log.d("debug", "exited")
                view?.background!!.alpha=0
                view.setBackgroundColor(Color.TRANSPARENT)
                view.invalidate()
                return true
            }

            DragEvent.ACTION_DROP -> { //todo: thread secundario
                Log.d("debug", "drop")
                val item = event.clipData.getItemAt(0)
                val dragData = item.text as String
                //setApp(dragData)
                app = dragData
                view!!.background.alpha = 0
                view.setBackgroundColor(Color.TRANSPARENT)
                post {
                    onIconAttachedListener(app, page, position)
                }
                post {
                    onPostIconAttachedListener(ON_ICON_ATTACHED)
                }
                view.invalidate()
                return true
            }
        }

        return true

    }

    private fun onClick(): Boolean {
        if (empty) {
            return true
        }
        val launchIntent: Intent? = mContext!!.packageManager.getLaunchIntentForPackage(app)
        if (launchIntent != null) {
            startActivity(mContext, launchIntent, null)
        } else {
            Log.d("debug", "intent null ")
        }
        return true
    }

    private fun onLongClick(v: View): Boolean {
        if (!empty) {
            val item = ClipData.Item(icon!!.tag as CharSequence)
            val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
            val dragData = ClipData(icon!!.tag.toString(), mimeTypes, item)

            val shadow = DragShadowBuilder(icon)
            onIconAttachedListener("", page, position) //old listener, gotta change it
            onPostIconAttachedListener(ON_MAIN_MENU_HOLD) //todo: cambiar esto

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                v.startDragAndDrop(dragData, shadow, null, 0)
            } else {
                v.startDrag(dragData, shadow, null, 0)
            }

        } else {
            onPostIconAttachedListener(ON_EMPTY_CLICK)
        }
        return true
    }

}