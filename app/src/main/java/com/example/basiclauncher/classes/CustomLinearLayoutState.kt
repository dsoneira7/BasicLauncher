package com.example.basiclauncher.classes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE

/**
 * Clase que contiene la información sobre el estado de cada una de las celdas.
 *
 * @param page: Página en la que se encuentra la celda a la que hace referencia.
 * @param position: Posición en dicha página en la que se encuentra la celda.
 * @param appId: Identificador que hace referencia a la aplicación que contiene esta celda.
 *               Si la celda está vacía entonces será -1.
 */
@Entity(tableName = "states",
        primaryKeys = ["page", "position"],
        foreignKeys = [ForeignKey(
                entity = AppIcon::class,
                childColumns = ["appId"],
                parentColumns = ["id"],
                onDelete = CASCADE
        )]
)
data class CustomLinearLayoutState(
        @ColumnInfo(name = "page") var page: Int,
        @ColumnInfo(name = "position") var position: Int,
        var appId: Int = -1
)