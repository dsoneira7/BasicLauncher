package com.example.basiclauncher.room

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.basiclauncher.classes.AppIcon
import com.example.basiclauncher.classes.CustomLinearLayoutState

@Database(entities = [AppIcon::class, CustomLinearLayoutState::class], version = 1)
@TypeConverters(DrawableTypeConverter::class)
abstract class AbstractAppDatabase : RoomDatabase() {

    abstract fun myDao(): MyDao
    abstract fun stateDao(): StateDao

    companion object {
        private var instance: AbstractAppDatabase? = null

        fun getInstance(context: Context): AbstractAppDatabase?{
            if(instance == null){
                synchronized(AbstractAppDatabase::class){
                    instance = Room.databaseBuilder(context.applicationContext, AbstractAppDatabase::class.java,
                        "myDb.db").build()
                }
            }

            return instance
        }
    }

    @Dao
    interface MyDao{
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insertApp(obj: AppIcon)

        @Query("select * from apps")
        fun getAppList() : LiveData<Array<AppIcon>>

        @Query("select * from apps where id=:id")
        fun getAppById(id: Int) : AppIcon

        @Query("select id from apps where packageName=:packageName")
        fun getAppIdByPackageName(packageName: String): Int

        @Delete
        fun deleteApp(obj: AppIcon)
    }

    @Dao
    interface StateDao{
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insertState(obj: CustomLinearLayoutState) : Long

        @Update
        fun updateState(obj: CustomLinearLayoutState)

        @Query("select * from states where page=:page and position = :position")
        fun getState(page : Int, position:Int): CustomLinearLayoutState

        @Query("select * from states")
        fun getAllStates(): LiveData<Array<CustomLinearLayoutState>>

        @Query("select * from states where page =:page order by position asc")
        fun getAllStatesByPage(page: Int): LiveData<Array<CustomLinearLayoutState>>

        @Delete
        fun deleteState(obj: CustomLinearLayoutState)
    }

}