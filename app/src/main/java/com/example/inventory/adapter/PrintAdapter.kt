package com.example.inventory.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventory.PrintRow
import com.example.inventory.R

class PrintAdapter(private val rows: MutableList<PrintRow>) :
    RecyclerView.Adapter<PrintAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.rowCheck)
        val info: TextView = view.findViewById(R.id.rowInfoP)
        val qty: EditText = view.findViewById(R.id.rowQtyP)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_print_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = rows[position]
        holder.info.text = "${row.barcode}  |  ${row.name}"

        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = row.selected
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            row.selected = isChecked
        }

        holder.qty.tag = row.barcode
        if (holder.qty.text.toString() != row.qty.toString()) {
            holder.qty.setText(row.qty.toString())
        }
        holder.qty.removeTextChangedListener(holder.qty.getTag(R.id.text_watcher_tag) as? android.text.TextWatcher)
        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                if (holder.qty.tag == row.barcode) {
                    row.qty = s?.toString()?.toIntOrNull() ?: 1
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        holder.qty.setTag(R.id.text_watcher_tag, watcher)
        holder.qty.addTextChangedListener(watcher)
    }

    override fun getItemCount() = rows.size
}
