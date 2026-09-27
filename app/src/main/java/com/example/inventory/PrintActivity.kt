package com.example.inventory

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventory.adapter.PrintAdapter
import com.example.inventory.databinding.ActivityPrintBinding
import org.json.JSONArray
import java.io.File
import java.net.Socket
import kotlin.concurrent.thread

class PrintActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrintBinding
    private lateinit var adapter: PrintAdapter
    private val rows = mutableListOf<PrintRow>()
    private val stateFile: File by lazy { File(filesDir, "inventory_state.json") }
    private val prefs by lazy { getSharedPreferences("printer_settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ipInput.setText(prefs.getString("ip", ""))
        binding.portInput.setText(prefs.getString("port", "9100"))

        loadItems()
        adapter = PrintAdapter(rows)
        binding.recyclerPrint.layoutManager = LinearLayoutManager(this)
        binding.recyclerPrint.adapter = adapter

        binding.btnSaveSettings.setOnClickListener {
            prefs.edit()
                .putString("ip", binding.ipInput.text.toString().trim())
                .putString("port", binding.portInput.text.toString().trim())
                .apply()
            toast("تنظیمات ذخیره شد")
        }

        binding.btnPrint.setOnClickListener { printSelected() }
    }

    private fun loadItems() {
        rows.clear()
        if (!stateFile.exists()) return
        try {
            val arr = JSONArray(stateFile.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                rows.add(
                    PrintRow(
                        barcode = o.getString("barcode"),
                        name = o.optString("name", ""),
                        qty = 1,
                        selected = false
                    )
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun buildZpl(): String {
        val sb = StringBuilder()
        rows.filter { it.selected }.forEach { row ->
            val safeQty = row.qty.coerceAtLeast(1)
            sb.append("^XA\n")
            sb.append("^CI28\n")
            sb.append("^FO50,30^A0N,35,35^FD${sanitize(row.name)}^FS\n")
            sb.append("^FO50,80^BY2\n")
            sb.append("^BCN,90,Y,N,N\n")
            sb.append("^FD${row.barcode}^FS\n")
            sb.append("^PQ$safeQty\n")
            sb.append("^XZ\n")
        }
        return sb.toString()
    }

    private fun sanitize(text: String): String =
        text.replace("^", "").replace("~", "").take(40)

    private fun printSelected() {
        val ip = binding.ipInput.text.toString().trim()
        val port = binding.portInput.text.toString().trim().toIntOrNull()
        if (ip.isEmpty() || port == null) {
            toast("آی‌پی و پورت را درست وارد کنید")
            return
        }
        val selectedCount = rows.count { it.selected }
        if (selectedCount == 0) {
            toast("حداقل یک کالا را انتخاب کنید")
            return
        }
        val zpl = buildZpl()
        thread {
            try {
                Socket(ip, port).use { socket ->
                    socket.getOutputStream().use { out ->
                        out.write(zpl.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                }
                runOnUiThread { toast("ارسال شد به چاپگر") }
            } catch (e: Exception) {
                runOnUiThread { toast("خطا در ارسال: ${e.message}") }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

data class PrintRow(
    val barcode: String,
    val name: String,
    var qty: Int,
    var selected: Boolean
)
