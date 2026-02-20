package com.example.myapplication

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class SaleHistoryAdapter(
    private var list: List<Sale>,
    private val onLongClick: (Sale) -> Unit
) : RecyclerView.Adapter<SaleHistoryAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val t: TextView = v.findViewById(R.id.tv1)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_simple, p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        val sale = list[i]
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        h.t.text =
            "Satış ${i + 1} - ${(sale.totalAmountCents / 100.0)} TL\n${sdf.format(Date(sale.timestamp))}"

        h.itemView.setOnLongClickListener {
            onLongClick(sale)
            true
        }
    }

    override fun getItemCount() = list.size

    fun update(n: List<Sale>) {
        list = n
        notifyDataSetChanged()
    }
}