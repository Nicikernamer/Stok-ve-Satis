package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface SaleRecordDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: SaleRecord)

    @Query("SELECT * FROM SaleRecord ORDER BY timestamp DESC")
    suspend fun getAll(): List<SaleRecord>

    @Query("SELECT * FROM SaleRecord WHERE saleId = :saleId")
    suspend fun getBySaleId(saleId: Int): List<SaleRecord>

    @Transaction
    suspend fun insertRecordAtomic(record: SaleRecord) {
        insert(record)
    }
}