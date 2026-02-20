package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Product(
    @PrimaryKey val barcode: String,
    val name: String,
    val quantity: Int,
    val priceCents: Long
)
