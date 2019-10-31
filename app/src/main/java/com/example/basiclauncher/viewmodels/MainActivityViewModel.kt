package com.example.basiclauncher.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.basiclauncher.R
import com.example.basiclauncher.Repository

class MainActivityViewModel(private val app: Application, private val screenWidth: Int, private val screenHeight: Int): AndroidViewModel(app){

    var iconsPerRow: Int = 0
    var iconsPerColumn: Int = 0

    var iconWidth: Int = 0
    var smallIconWidth: Int = 0
    var smallerIconWidth: Int = 0

    var iconHeight: Int = 0
    var smallIconHeight: Int = 0
    var smallerIconHeight: Int = 0

    private var repository: Repository = Repository.getInstance(app.applicationContext)!!

    init{
        iconSizeDataInitialize()
    }

    fun iconSizeDataInitialize() {
        //Obtenemos el número de iconos configurado de las SharedPreferences
        iconsPerRow = repository.getIconsPerRow()

        //El ancho se obtiene dividiendo el ancho del fragmento entre el número de iconos.
        //En el caso del fragmento principal este ancho es la pantalla. En los otros casos hay que
        //restarle márgenes y paddings que se introducen. En el caso del smallIconWidth tiene un
        //changer y un margin por cada lado.
        smallIconWidth = ((screenWidth - (app.resources.getDimension(R.dimen.changer_width) + app.resources.getDimension(R.dimen.changer_margin)) * 2) / iconsPerRow).toInt()
        smallerIconWidth = ((screenWidth- (app.resources.getDimension(R.dimen.viewpager_paddingLeft) + app.resources.getDimension(R.dimen.viewpager_paddingRight))) / iconsPerRow).toInt()
        iconWidth = screenWidth/ iconsPerRow

        var statusBarHeight = 0
            val resourceId = app.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                statusBarHeight = app.resources.getDimensionPixelSize(resourceId)
            }

        //Ahora obtenemos la altura del MainFragment grande. La altura de la pantalla menos la altura
        //de la barra de notificaciones, de la barra de accesos y directos y del tabLayout (las
        // bolitas que indican en que pagina estamos).
        val mainFragmentHeight = screenHeight -
                app.resources.getDimension(R.dimen.tablayout_height).toInt() -
                app.resources.getDimension(R.dimen.app_drawer_container_height).toInt() -
                statusBarHeight

        // Lo que queremos es que cada celda sea lo más cuadrada posible llenando toodo el espacio
        // disponible. El algoritmo para buscar la altura por icono y el número de iconos por
        // culumna consiste en iterar el número de iconos, comparando la altura que tendría cada
        // icono con la anchura que ya hemos obtenido. Una vez tengamos las alturas que más se apro-
        // ximen por arriba y por abajo a la anchura conocida seleccionamos la más próxima.
        var nearestDown = 0
        var nearestUp = 0
        var nearestNumberOfRowsDown = 0
        var nearestNumberOfRowsUp = 0
        var i = 1

        while (true) {
            if (((mainFragmentHeight / i) - iconWidth) > 0) {
                nearestUp = mainFragmentHeight / i
                nearestNumberOfRowsUp = i
            } else {
                nearestDown = mainFragmentHeight / i
                nearestNumberOfRowsDown = i
                break
            }
            i++
        }

        //Seleccionamos el número de iconos por arriba o por abajo según la altura que más se apro-
        //xime a la anchura. Así conseguimos que la celda sea lo más cuadrada posible
        iconsPerColumn = if ((nearestUp - iconWidth) < (iconWidth - nearestDown)) {
            nearestNumberOfRowsUp
        } else {
            nearestNumberOfRowsDown
        }

        //Ahora configuramos la anchura para los diferentes tamaños de fragmento, dividiendo
        //la altura que tenedría cada fragmento entre el número de iconos por columna ya conocido.
        iconHeight = mainFragmentHeight / iconsPerColumn
        smallIconHeight = (mainFragmentHeight
                - app.resources.getDimension(R.dimen.container_small_margin_bottom).toInt()
                - app.resources.getDimension(R.dimen.container_small_margin_top).toInt()) / iconsPerColumn
        smallerIconHeight = (mainFragmentHeight
                + app.resources.getDimension(R.dimen.app_drawer_container_height).toInt()
                - app.resources.getDimension(R.dimen.container_smaller_margin_top).toInt()
                - app.resources.getDimension(R.dimen.container_smaller_margin_bottom).toInt()) / iconsPerColumn
    }
}