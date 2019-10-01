package com.example.basiclauncher.classes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE

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