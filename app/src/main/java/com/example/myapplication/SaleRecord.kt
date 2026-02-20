package com.example.myapplication

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["saleId"])]
)
data class SaleRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val saleId: Int,
    val barcode: String,
    val productName: String,
    val quantitySold: Int,
    val totalPriceCents: Long,
    val timestamp: Long = System.currentTimeMillis()
)
