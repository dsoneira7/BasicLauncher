package com.example.basiclauncher

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.text.InputFilter
import android.text.TextUtils
import android.util.Log
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.contains
import androidx.core.widget.TextViewCompat
import com.example.basiclauncher.classes.AppIcon
import com.example.basiclauncher.viewmodels.MainFragmentViewModel

/**
 * Subclase de [LinearLayout] que se corresponde con cada una de las celdas que va a contener el
 * launcher.
 */
class CustomLinearLayout(private val mContext: Context?) : LinearLayout(mContext), View.OnDragListener {

    //Es necesario asignarle un valor en caso de que se utilice el constructor principal
    private var page: Int = -2
    private var position: Int = -2
    private var viewModel: MainFragmentViewModel? = null
    private var isNormalFragment: Boolean = false

    constructor(mContext: Context?, page: Int, position: Int) : this(mContext) {
        this.page = page
        this.position = position
    }

    constructor(mContext: Context?, page: Int, position: Int, isNormalFragment: Boolean) : this(mContext, page, position) {
        this.isNormalFragment = isNormalFragment
    }

    constructor(mContext: Context?, page: Int, position: Int, isNormalFragment: Boolean, viewModel: MainFragmentViewModel) : this(mContext, page, position, isNormalFragment) {
        this.viewModel = viewModel
    }

    private var colorFrom: Int //colores en ARGB para las animaciones
    private var colorTo: Int
    private var empty = true
    private lateinit var iconOperationListener: (String, Int, Int) -> Unit
    private lateinit var cellInteractionListener: (Int) -> Unit
    private var packageName: String = ""
    private var appId = -1
    private var icon: ImageView? = null
    private var appName: AppCompatTextView? = null

    init {
        setOnLongClickListener { onLongClick(it) }
        setOnClickListener { onClick() }
        setOnDragListener(this)
        colorFrom = ContextCompat.getColor(mContext!!, R.color.blackHighAlpha)
        colorTo = ContextCompat.getColor(mContext, R.color.blackLowAlpha)
        this.orientation = VERTICAL
    }

    /**
     * Devuelve un booleano que indica si la celda está vacía o no
     */
    fun isEmpty(): Boolean = empty

    /**
     * Devuelve el appId de la app asociada a esta celda
     */
    fun getAppId(): Int = appId

    /**
     * Se utiliza para adjuntar los callbacks del layout
     */
    fun attachListeners(onIconAttached: (String, Int, Int) -> Unit, onPostIconAttachedListener: (Int) -> Unit) {
        iconOperationListener = onIconAttached
        this.cellInteractionListener = onPostIconAttachedListener
    }

    /**
     * Esta función settea una nueva app en una celda.
     */
    fun setApp(app: AppIcon) {
        this.packageName = app.packageName
        this.appId = app.id
        empty = false

        if(app.appName.length > 10){//Recortamos el textview de ser necesario
            app.appName = app.appName.substring(0,10) + "..."
        }

        if (icon != null) {
            //Si la celda ya se ha actualizado alguna vez en esta sesión, simplemente actualizamos
            //el icono y el nombre y añadimos las vistas de ser necesario
            icon?.setImageDrawable(BitmapDrawable(context.resources, app.icon))
            appName?.text = app.appName
            if (!this.contains(icon!!)) {
                this.addView(icon)
                this.addView(appName)
            }
        } else {
            //Si la celda no se ha actualizado ajustamos los parámetros encesariso para su correcto funcioamiento
            appName = AppCompatTextView(mContext)
            appName?.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 3.5f)

            //Autosize uniforme automático del textview
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appName?.setAutoSizeTextTypeUniformWithConfiguration(2, 100, 2, TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            } else {
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(appName!!, 2, 100, 2, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            }
            icon = ImageView(mContext)
            icon?.tag = app.packageName
            icon?.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.2f)
            val drawable = BitmapDrawable(context.resources, app.icon)
            icon?.setImageDrawable(drawable)
            this.addView(icon)
            appName?.text = app.appName
            appName?.gravity = Gravity.CENTER_HORIZONTAL
            appName?.maxLines = 1
            this.addView(appName)
        }
    }

    /**
     * Esta función se llama para interactuar con los eventos de Drag&Drop
     */
    override fun onDrag(view: View?, event: DragEvent?): Boolean {
        when (event?.action) {

            DragEvent.ACTION_DRAG_ENTERED -> {
                setAnimator(Color.TRANSPARENT, colorTo, 200)
                return true
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                setAnimator(colorTo, Color.TRANSPARENT, 200)
                return true
            }

            DragEvent.ACTION_DROP -> {
                //Si se hace un drop en una celda desempaquetamos la información
                val item = event.clipData.getItemAt(0)
                val dragData = item.text as String
                val packageName = dragData.substringBefore(";")
                post {
                    //Esta llamada hace que el fragmento contenedor le indique al repositorio que es
                    //lo que tiene que meter en la BBDD
                    iconOperationListener(packageName, page, position)
                }
                post {
                    //Esta llamada va destinada a advertir que se está preparado para el cambio
                    //de fragmentos.
                    cellInteractionListener(ON_ICON_ATTACHED)
                }
                setAnimator(colorTo, Color.TRANSPARENT, 400)
                return true
            }
        }

        return true

    }

    private fun onClick(): Boolean {
        if (!isNormalFragment) {
            //Si no estamos en modo normal (modo ajustes) un click en el grid nos lleva al mdoo normal
            cellInteractionListener(ON_GRID_CLICK_FROM_SMALLER_MODE)
            return true
        }
        if (empty) {
            //Si la celda está vacía no hacemos nada
            return true
        }
        //Recreamos el intent de la aplicació en cuestión y lo lanzamos.
        val launchIntent: Intent? = mContext!!.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(mContext, launchIntent, null)
        } else {
            Log.e("ERROR", "intent null")
        }
        return true
    }

    private fun onLongClick(v: View): Boolean {
        if (!isNormalFragment) {
            return true
        }
        if (!empty) {
            //Si hacemos un long click, empaquetamos los datos de la app si la celda no está vacía
            //e iniciamos el drag
            val item = ClipData.Item((packageName + ";" + page + ";" + position) as CharSequence)
            val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
            val dragData = ClipData(packageName + ";" + page + ";" + position, mimeTypes, item)
            Log.d("LinearLayout", "StartDrag $packageName $page $position")
            val shadow = DragShadowBuilder(icon)
            //Esta llamada indicará al fragmento contenedor que debe borrar esta app de la BBDD
            iconOperationListener("", page, position)
            cellInteractionListener(ON_MAIN_MENU_HOLD)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                v.startDragAndDrop(dragData, shadow, null, 0)
            } else {
                v.startDrag(dragData, shadow, null, 0)
            }
        } else {
            //Si la celda está vacía enviamos un callback para entrar en modo ajustes
            cellInteractionListener(ON_EMPTY_CLICK)
        }
        return true
    }

    /**
     * Método dedicado a limpiar la vista de la celda
     */
    fun clear() {
        Log.d("LinearLayout", "Cell cleared: $page $position $packageName")
        packageName = ""
        this.removeView(icon)
        this.removeView(appName)
        empty = true
        invalidate()
    }

    /**
     * Método dedicado a limpiar sólo el texto de la celda
     */
    fun clearText() {
        this.removeView(appName)
    }

    /**
     * Configura un [ValueAnimator] para llevar a cabo animaciones en la celda
     *
     * @param colorFrom: Color inicial en ARGB de la animación
     * @param colorTo: Color final en ARGB de la animación
     * @param duration: Duración de la animación
     */
    private fun setAnimator(colorFrom: Int, colorTo: Int, duration: Long) {

        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = duration
        colorAnimation.addUpdateListener {
            this.setBackgroundColor(it.animatedValue as Int)
        }
        colorAnimation.start()
    }
}