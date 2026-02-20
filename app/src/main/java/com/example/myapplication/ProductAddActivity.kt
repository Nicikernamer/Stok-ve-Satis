package com.example.myapplication

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProductAddActivity : AppCompatActivity() {

    private lateinit var scanner: BarcodeScannerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_add)

        val db = AppDatabase.getDatabase(this)

        val previewView = findViewById<PreviewView>(R.id.previewView)
        val etBarcode = findViewById<EditText>(R.id.etBarcode)
        val etName = findViewById<EditText>(R.id.etName)
        val etQty = findViewById<EditText>(R.id.etQuantity)
        val etPrice = findViewById<EditText>(R.id.etPrice)
        val btnSave = findViewById<Button>(R.id.btnSave)

        scanner = BarcodeScannerHelper(this, this) {
            runOnUiThread { etBarcode.setText(it) }
        }
        scanner.start(previewView)

        btnSave.setOnClickListener {
            val barcode = etBarcode.text.toString()
            val name = etName.text.toString()
            val qty = etQty.text.toString().toIntOrNull()
            val price = etPrice.text.toString().toDoubleOrNull()

            if (barcode.isBlank() || name.isBlank() || qty == null || price == null) {
                Toast.makeText(this, "Tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val priceCents = (price * 100).toLong()

            lifecycleScope.launch {
                db.productDao().insertProduct(
                    Product(barcode, name, qty, priceCents)
                )
                Toast.makeText(this@ProductAddActivity, "Ürün kaydedildi", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanner.stop()
    }
}
