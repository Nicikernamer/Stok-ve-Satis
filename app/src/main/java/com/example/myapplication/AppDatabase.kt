package com.example.myapplication

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Product::class, Sale::class, SaleRecord::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun saleRecordDao(): SaleRecordDao
    abstract fun saleDao(): SaleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_db"
                )
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
