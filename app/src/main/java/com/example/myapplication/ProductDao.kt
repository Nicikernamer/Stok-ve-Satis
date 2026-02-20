package com.example.myapplication

import androidx.room.*

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Query("SELECT * FROM Product ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllProducts(): List<Product>

    @Query("SELECT * FROM Product WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("""
        SELECT * FROM Product
        WHERE name LIKE '%' || :query || '%'
        OR barcode LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE ASC
    """)
    suspend fun searchProducts(query: String): List<Product>

    @Query("""
        UPDATE Product
        SET quantity = quantity - :qty
        WHERE barcode = :barcode
        AND quantity >= :qty
    """)
    suspend fun decreaseStockSafely(barcode: String, qty: Int): Int

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Transaction
    suspend fun tryDecreaseStock(barcode: String, qty: Int): Boolean {
        return decreaseStockSafely(barcode, qty) > 0
    }
}
