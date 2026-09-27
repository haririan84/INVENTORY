package com.example.inventory

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventory.adapter.ItemAdapter
import com.example.inventory.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val items = LinkedHashMap<String, Item>()
    private lateinit var adapter: ItemAdapter
    private val orderedList = mutableListOf<Item>()

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService? = null
    private var scanning = false
    private var lastCode: String? = null
    private var lastTime = 0L
    private var currentFoundBarcode: String? = null

    private val stateFile: File by lazy { File(filesDir, "inventory_state.json") }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else toast("دسترسی دوربین رد شد")
        }

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importCsv(uri)
        }

    private val exportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri != null) exportCsv(uri)
        }

    private val sampleSaveLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri != null) writeSampleCsv(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ItemAdapter(orderedList) { item, newQty ->
            item.qty = newQty.coerceAtLeast(0)
            saveState()
            renderTotals()
        }
        binding.recyclerList.layoutManager = LinearLayoutManager(this)
        binding.recyclerList.adapter = adapter

        binding.btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("text/*", "text/comma-separated-values", "*/*"))
        }
        binding.btnExport.setOnClickListener {
            exportLauncher.launch("نتیجه-انبارگردانی.csv")
        }
        binding.btnSample.setOnClickListener {
            sampleSaveLauncher.launch("نمونه-فایل-قطعات.csv")
        }
        binding.btnScan.setOnClickListener { toggleScan() }
        binding.btnAddQty.setOnClickListener { confirmAddQty() }
        binding.btnPrintScreen.setOnClickListener {
            startActivity(android.content.Intent(this, PrintActivity::class.java))
        }

        loadState()
        refreshList()
    }

    // ---------------- STATE PERSISTENCE ----------------

    private fun saveState() {
        val arr = JSONArray()
        items.values.forEach { i ->
            val o = JSONObject()
            o.put("barcode", i.barcode)
            o.put("name", i.name)
            o.put("price", i.price)
            o.put("qty", i.qty)
            arr.put(o)
        }
        stateFile.writeText(arr.toString())
    }
