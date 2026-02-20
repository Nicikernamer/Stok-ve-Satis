package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter

class ProductListActivity : AppCompatActivity() {

    private lateinit var adapter: ProductAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }

    private val createFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { exportToJson(it) }
        }

    private val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { importFromJson(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)

        val recycler = findViewById<RecyclerView>(R.id.recyclerProducts)
        recycler.layoutManager = LinearLayoutManager(this)

        adapter = ProductAdapter(
            onEdit = { showEditDialog(it) },
            onDelete = { deleteProduct(it) }
        )
        recycler.adapter = adapter

        val searchEditText = EditText(this).apply {
            hint = "Ürün adı veya barkod ara"
        }

        (recycler.parent as? android.widget.LinearLayout)?.addView(searchEditText, 0)

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                lifecycleScope.launch {
                    val list = if (query.isEmpty())
                        db.productDao().getAllProducts()
                    else
                        db.productDao().searchProducts(query)

                    adapter.submitList(list)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        refreshList()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menu.add("Envanteri Dışa Aktar")
        menu.add("Envanteri İçe Aktar")
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        when (item.title) {
            "Envanteri Dışa Aktar" -> createFileLauncher.launch("envanter_yedek.json")
            "Envanteri İçe Aktar" -> openFileLauncher.launch(arrayOf("application/json"))
        }
        return true
    }

    private fun refreshList() {
        lifecycleScope.launch {
            adapter.submitList(db.productDao().getAllProducts())
        }
    }

    private fun exportToJson(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val products = db.productDao().getAllProducts()
            val jsonArray = JSONArray()

            for (p in products) {
                jsonArray.put(
                    JSONObject().apply {
                        put("name", p.name)
                        put("barcode", p.barcode)
                        put("quantity", p.quantity)
                        put("priceCents", p.priceCents)
                    }
                )
            }

            contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream).use {
                    it.write(jsonArray.toString())
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ProductListActivity,
                    "Envanter dışa aktarıldı",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun importFromJson(uri: Uri) {
        lifecycleScope.launch {
            try {
                val jsonText = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: throw Exception()
                }

                val jsonArray = JSONArray(jsonText)

                withContext(Dispatchers.IO) {
                    db.withTransaction {
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)

                            val barcode = obj.getString("barcode")
                            val name = obj.getString("name")
                            val quantity = obj.getInt("quantity")

                            // 🔥 UYUMLULUK BURADA
                            val priceCents = when {
                                obj.has("priceCents") ->
                                    obj.getLong("priceCents")

                                obj.has("price") ->
                                    (obj.getDouble("price") * 100).toLong()

                                else -> 0L
                            }

                            val existing =
                                db.productDao().getProductByBarcode(barcode)

                            if (existing != null) {
                                db.productDao().updateProduct(
                                    existing.copy(
                                        name = name,
                                        quantity = quantity,
                                        priceCents = priceCents
                                    )
                                )
                            } else {
                                db.productDao().insertProduct(
                                    Product(
                                        barcode = barcode,
                                        name = name,
                                        quantity = quantity,
                                        priceCents = priceCents
                                    )
                                )
                            }
                        }
                    }
                }

                Toast.makeText(
                    this@ProductListActivity,
                    "Envanter içe aktarıldı",
                    Toast.LENGTH_LONG
                ).show()

                refreshList()

            } catch (e: Exception) {
                Toast.makeText(
                    this@ProductListActivity,
                    "Dosya okunamadı!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showEditDialog(product: Product) {
        val view = layoutInflater.inflate(R.layout.item_edit_product, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etQty = view.findViewById<EditText>(R.id.etQty)
        val etPrice = view.findViewById<EditText>(R.id.etPrice)

        etName.setText(product.name)
        etQty.setText(product.quantity.toString())
        etPrice.setText((product.priceCents / 100.0).toString())

        AlertDialog.Builder(this)
            .setTitle("Ürünü Düzenle")
            .setView(view)
            .setPositiveButton("Kaydet") { dialog, _ ->
                lifecycleScope.launch {
                    val priceCents =
                        ((etPrice.text.toString().toDoubleOrNull() ?: 0.0) * 100).toLong()

                    db.productDao().updateProduct(
                        product.copy(
                            name = etName.text.toString(),
                            quantity = etQty.text.toString().toIntOrNull() ?: product.quantity,
                            priceCents = priceCents
                        )
                    )
                    refreshList()
                }
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun deleteProduct(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Ürünü Sil")
            .setMessage("${product.name} silinsin mi?")
            .setPositiveButton("Evet") { dialog, _ ->
                lifecycleScope.launch {
                    db.productDao().deleteProduct(product)
                    refreshList()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }
}