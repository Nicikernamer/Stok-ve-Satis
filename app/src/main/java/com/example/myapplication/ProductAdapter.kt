package com.example.myapplication

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ProductAdapter(
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.VH>(DIFF) {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val t1: TextView = v.findViewById(R.id.tv1)
        val t2: TextView = v.findViewById(R.id.tv2)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_simple, p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        val p = getItem(i)

        val priceText = String.format(
            Locale.getDefault(),
            "%.2f",
            p.priceCents / 100.0
        )

        h.t1.text = "${p.name} (Barkod: ${p.barcode})"
        h.t2.text = "Stok: ${p.quantity} | Fiyat: $priceText ₺"

        h.itemView.setOnLongClickListener {
            val options = arrayOf("Düzenle", "Sil")
            AlertDialog.Builder(h.itemView.context)
                .setTitle("Ürün Seçimi")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> onEdit(p)
                        1 -> onDelete(p)
                    }
                }
                .show()
            true
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean =
                oldItem.barcode == newItem.barcode

            override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean =
                oldItem == newItem
        }
    }
}
