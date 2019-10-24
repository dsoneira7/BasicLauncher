package com.example.basiclauncher.room

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.basiclauncher.Helper
import com.example.basiclauncher.classes.AppIcon
import com.example.basiclauncher.classes.CustomLinearLayoutState

@Database(entities = [AppIcon::class, CustomLinearLayoutState::class], version = 1)
@TypeConverters(DrawableTypeConverter::class)
abstract class AbstractAppDatabase : RoomDatabase() {

    abstract fun myDao(): MyDao
    abstract fun stateDao(): StateDao

    companion object {
        private var instance: AbstractAppDatabase? = null

        fun getInstance(context: Context): AbstractAppDatabase? {
            if (instance == null) {
                synchronized(AbstractAppDatabase::class) {
                    instance = Room.databaseBuilder(context.applicationContext, AbstractAppDatabase::class.java,
                            "myDb.db").addCallback(object : Callback() {
                        //Este método se ejecutará cuando se cree por primera vez la BBDD
                        //Llenamos la tabla de AppIcons con info sobre las app isntaladas.
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            //Lanzamos un nuevo thread para poder acceder a getInstance() y al DAO
                            Thread {
                                val mainIntent = Intent(Intent.ACTION_MAIN, null)
                                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                                //Seleccionamos todos los intents de estas categorías. Sería necesa-
                                //rio contemplar también que una app puede tener más de una activity
                                //launcher
                                val apps = context.packageManager.queryIntentActivities(mainIntent, 0)
                                for (i in apps) {
                                    if (i.activityInfo.packageName == context.packageName) {
                                        //No añadimos la actividad launcher de nuestra propia app
                                        continue
                                    }
                                    
                                    getInstance(context)!!.myDao().insertApp(AppIcon(
                                            i.activityInfo.packageName,
                                            Helper.getAppName(context, i.activityInfo.packageName),
                                            Helper.getActivityIcon(context, i.activityInfo.packageName)
                                    ))
                                }
                            }.start()
                        }
                    }).build()
                }
            }


            return instance
        }
    }

    @Dao
    interface MyDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insertApp(obj: AppIcon)

        @Query("select * from apps")
        fun getAppList(): LiveData<Array<AppIcon>>

        @Query("select * from apps where id=:id")
        fun getAppById(id: Int): AppIcon

        @Delete
        fun deleteApp(obj: AppIcon)
    }

    @Dao
    interface StateDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insertState(obj: CustomLinearLayoutState): Long

        @Update
        fun updateState(obj: CustomLinearLayoutState)

        @Query("select * from states where page=:page and position = :position")
        fun getState(page: Int, position: Int): CustomLinearLayoutState

        @Query("select * from states")
        fun getAllStates(): LiveData<Array<CustomLinearLayoutState>>

        @Query("select * from states where page =:page order by position asc")
        fun getAllStatesByPage(page: Int): LiveData<Array<CustomLinearLayoutState>>

        @Delete
        fun deleteState(obj: CustomLinearLayoutState)
    }

}