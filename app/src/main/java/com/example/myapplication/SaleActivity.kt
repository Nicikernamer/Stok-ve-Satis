package com.example.myapplication

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class CartItem(
    val product: Product,
    var quantity: Int
)

class SaleActivity : AppCompatActivity() {

    private lateinit var scanner: BarcodeScannerHelper
    private val cart = mutableListOf<CartItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale)

        val db = AppDatabase.getDatabase(this)

        val previewView = findViewById<PreviewView>(R.id.previewView)
        val etBarcode = findViewById<EditText>(R.id.etBarcode)
        val etQty = findViewById<EditText>(R.id.etQty)
        val btnSell = findViewById<Button>(R.id.btnSell)

        val btnSearchProduct = Button(this).apply { text = "Ürün Ara" }
        val btnViewCart = Button(this).apply { text = "Sepeti Görüntüle" }
        val btnComplete = Button(this).apply { text = "Satışı Tamamla" }

        (btnSell.parent as LinearLayout).apply {
            addView(btnSearchProduct)
            addView(btnViewCart)
            addView(btnComplete)
        }

        scanner = BarcodeScannerHelper(this, this) { barcode ->
            runOnUiThread { etBarcode.setText(barcode) }
        }
        scanner.start(previewView)

        // MANUEL EKLE
        btnSell.setOnClickListener {
            val qty = etQty.text.toString().toIntOrNull()
            val barcode = etBarcode.text.toString()

            if (qty == null || qty <= 0 || barcode.isBlank()) {
                Toast.makeText(this, "Bilgi hatalı", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val product = db.productDao().getProductByBarcode(barcode)
                if (product == null || product.quantity < qty) {
                    Toast.makeText(
                        this@SaleActivity,
                        "Ürün yok / stok yetersiz",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                addToCart(product, qty)
                etBarcode.text.clear()
                etQty.text.clear()
            }
        }

        // ÜRÜN ARA (İSİM + BARKOD)
        btnSearchProduct.setOnClickListener {
            lifecycleScope.launch {
                val allProducts = db.productDao().getAllProducts().toMutableList()
                val shownProducts = allProducts.toMutableList()

                val root = LinearLayout(this@SaleActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(24, 24, 24, 24)
                }

                val searchEt = EditText(this@SaleActivity).apply {
                    hint = "Ürün adı veya barkod"
                }

                val listView = ListView(this@SaleActivity)

                val adapter = ArrayAdapter(
                    this@SaleActivity,
                    android.R.layout.simple_list_item_1,
                    shownProducts.map { "${it.name} (${it.barcode})" }.toMutableList()
                )

                listView.adapter = adapter

                root.addView(searchEt)
                root.addView(listView)

                val dialog = AlertDialog.Builder(this@SaleActivity)
                    .setTitle("Ürün Seç")
                    .setView(root)
                    .setNegativeButton("Kapat", null)
                    .create()

                searchEt.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val q = s.toString().lowercase(Locale.getDefault())
                        shownProducts.clear()
                        shownProducts.addAll(
                            allProducts.filter {
                                it.name.lowercase(Locale.getDefault()).contains(q)
                                        || it.barcode.contains(q)
                            }
                        )
                        adapter.clear()
                        adapter.addAll(shownProducts.map { "${it.name} (${it.barcode})" })
                        adapter.notifyDataSetChanged()
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })

                listView.setOnItemClickListener { _, _, pos, _ ->
                    val p = shownProducts[pos]

                    val qtyEt = EditText(this@SaleActivity).apply {
                        inputType = InputType.TYPE_CLASS_NUMBER
                        hint = "Adet"
                    }

                    AlertDialog.Builder(this@SaleActivity)
                        .setTitle(p.name)
                        .setView(qtyEt)
                        .setPositiveButton("Ekle") { _, _ ->
                            val q = qtyEt.text.toString().toIntOrNull()
                            if (q != null && q > 0 && q <= p.quantity) {
                                addToCart(p, q)
                                dialog.dismiss()
                            }
                        }
                        .setNegativeButton("İptal", null)
                        .show()
                }

                dialog.show()
            }
        }

        // SEPET
        btnViewCart.setOnClickListener { showCart() }

        // SATIŞI TAMAMLA
        btnComplete.setOnClickListener {
            if (cart.isEmpty()) {
                Toast.makeText(this, "Sepet boş", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val totalCents = cart.fold(0L) { acc, item ->
                    acc + (item.product.priceCents * item.quantity)
                }

                withContext(Dispatchers.IO) {
                    db.withTransaction {
                        val saleId = db.saleDao()
                            .insert(Sale(totalAmountCents = totalCents))
                            .toInt()

                        for (item in cart) {
                            val ok = db.productDao()
                                .tryDecreaseStock(item.product.barcode, item.quantity)
                            if (!ok) error("Stok yetersiz")

                            db.saleRecordDao().insert(
                                SaleRecord(
                                    saleId = saleId,
                                    barcode = item.product.barcode,
                                    productName = item.product.name,
                                    quantitySold = item.quantity,
                                    totalPriceCents = item.product.priceCents * item.quantity
                                )
                            )
                        }
                    }
                }

                AlertDialog.Builder(this@SaleActivity)
                    .setTitle("Toplam")
                    .setMessage(
                        "Toplam: ${
                            String.format(
                                Locale.getDefault(),
                                "%.2f",
                                totalCents / 100.0
                            )
                        } TL"
                    )
                    .setPositiveButton("Tamam", null)
                    .show()

                cart.clear()
            }
        }
    }

    private fun addToCart(product: Product, qty: Int) {
        val existing = cart.find { it.product.barcode == product.barcode }
        if (existing != null) {
            existing.quantity += qty
        } else {
            cart.add(CartItem(product, qty))
        }
        Toast.makeText(this, "${product.name} sepete eklendi", Toast.LENGTH_SHORT).show()
    }

    private fun showCart() {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Sepet boş", Toast.LENGTH_SHORT).show()
            return
        }

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val edits = mutableListOf<Pair<EditText, CartItem>>()

        cart.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tv = TextView(this).apply {
                text =
                    "${item.product.name} - ${"%.2f".format(item.product.priceCents * item.quantity / 100.0)} TL"
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
            }

            val et = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(item.quantity.toString())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
            }

            val btnRemove = Button(this).apply {
                text = "Sil"
            }

            edits.add(et to item)

            btnRemove.setOnClickListener {
                cart.remove(item)
                showCart()
            }

            row.addView(tv)
            row.addView(et)
            row.addView(btnRemove)
            layout.addView(row)
        }

        scroll.addView(layout)

        AlertDialog.Builder(this)
            .setTitle("Sepet")
            .setView(scroll)
            .setPositiveButton("Tamam") { d, _ ->
                edits.forEach { (et, item) ->
                    et.text.toString().toIntOrNull()?.let {
                        if (it > 0) item.quantity = it
                    }
                }
                d.dismiss()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scanner.stop()
    }
}
