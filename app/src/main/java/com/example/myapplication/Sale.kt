package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val totalAmountCents: Long,
    val timestamp: Long = System.currentTimeMillis()
)
