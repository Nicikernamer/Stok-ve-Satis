package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface SaleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(sale: Sale): Long

    @Query("SELECT * FROM Sale ORDER BY timestamp DESC")
    suspend fun getAll(): List<Sale>

    @Transaction
    suspend fun insertSaleAtomic(sale: Sale): Long {
        return insert(sale)
    }
}
