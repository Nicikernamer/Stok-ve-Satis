package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class SaleHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale_history)

        val recycler = findViewById<RecyclerView>(R.id.recyclerSales)
        recycler.layoutManager = LinearLayoutManager(this)

        val adapter = SaleHistoryAdapter(emptyList()) { sale ->
            showSaleDetail(sale)
        }

        recycler.adapter = adapter

        lifecycleScope.launch {
            adapter.update(
                AppDatabase.getDatabase(this@SaleHistoryActivity)
                    .saleDao()
                    .getAll()
            )
        }
    }

    private fun showSaleDetail(sale: Sale) {
        lifecycleScope.launch {
            val records =
                AppDatabase.getDatabase(this@SaleHistoryActivity)
                    .saleRecordDao()
                    .getBySaleId(sale.id)

            if (records.isEmpty()) {
                AlertDialog.Builder(this@SaleHistoryActivity)
                    .setTitle("Satış Detayı")
                    .setMessage("Bu satışa ait detay bulunamadı.")
                    .setPositiveButton("Kapat", null)
                    .show()
                return@launch
            }

            val sb = StringBuilder()
            var total = 0L

            records.forEach {
                val unitPrice = it.totalPriceCents / it.quantitySold
                total += it.totalPriceCents

                sb.append(
                    "${it.productName}\n" +
                    "${it.quantitySold} × ${(unitPrice / 100.0)} TL = ${(it.totalPriceCents / 100.0)} TL\n\n"
                )
            }

            sb.append("TOPLAM: ${(total / 100.0)} TL")

            AlertDialog.Builder(this@SaleHistoryActivity)
                .setTitle("Satış Detayı")
                .setMessage(sb.toString())
                .setPositiveButton("Kapat", null)
                .show()
        }
    }
}