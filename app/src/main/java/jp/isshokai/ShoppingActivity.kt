package jp.isshokai

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.io.File

class ShoppingActivity : AppCompatActivity() {
    private lateinit var db: ProductDb
    private lateinit var countText: TextView
    private lateinit var recentList: ListView
    private lateinit var emptyText: TextView

    private val quickCapture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        refresh()
        if (result.resultCode == RESULT_OK && result.data?.getBooleanExtra(QuickCaptureActivity.EXTRA_SCAN_NEXT, false) == true) {
            scanBarcode()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = ProductDb(this)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        root.addView(TextView(this).apply {
            text = "店頭で記録"
            textSize = 28f
        })
        root.addView(TextView(this).apply {
            text = "選ばれた商品だけ、JANと写真を残します。"
            textSize = 14f
            setPadding(0, dp(4), 0, dp(10))
        })

        countText = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 18f
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(countText, fullWidth())

        root.addView(Button(this).apply {
            text = "JANバーコードを読む"
            textSize = 21f
            minHeight = dp(82)
            setOnClickListener { scanBarcode() }
        }, fullWidth())

        root.addView(Button(this).apply {
            text = "JANを手入力する"
            minHeight = dp(54)
            setOnClickListener { showManualBarcodeDialog() }
        }, fullWidth().apply { topMargin = dp(6) })

        root.addView(Button(this).apply {
            text = "バーコードがない商品を写真で追加"
            minHeight = dp(54)
            setOnClickListener { openQuickCapture("") }
        }, fullWidth().apply { topMargin = dp(6) })

        root.addView(TextView(this).apply {
            text = "直近に選んだ商品"
            textSize = 18f
            setPadding(0, dp(18), 0, dp(6))
        })

        emptyText = TextView(this).apply {
            text = "まだ商品を登録していません。"
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(20))
        }
        root.addView(emptyText, fullWidth())

        recentList = ListView(this).apply { dividerHeight = dp(1) }
        root.addView(recentList, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        root.addView(Button(this).apply {
            text = "LINEへ戻る"
            setOnClickListener {
                val line = packageManager.getLaunchIntentForPackage("jp.naver.line.android")
                if (line != null) {
                    try {
                        startActivity(line)
                    } catch (_: android.content.ActivityNotFoundException) {
                        Toast.makeText(this@ShoppingActivity, "LINEを開けませんでした", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ShoppingActivity, "LINEがインストールされていません", Toast.LENGTH_SHORT).show()
                }
            }
        }, fullWidth())

        root.addView(Button(this).apply {
            text = "店頭での記録を終了"
            setOnClickListener { finish() }
        }, fullWidth().apply { topMargin = dp(8) })

        return root
    }

    private fun scanBarcode() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .enableAutoZoom()
            .build()

        GmsBarcodeScanning.getClient(this, options)
            .startScan()
            .addOnSuccessListener { barcode ->
                val value = barcode.rawValue.orEmpty().filter { it.isDigit() }
                if (value.isBlank()) {
                    Toast.makeText(this, "JANを読み取れませんでした", Toast.LENGTH_SHORT).show()
                    showManualBarcodeDialog()
                } else {
                    openQuickCapture(value)
                }
            }
            .addOnCanceledListener { }
            .addOnFailureListener {
                Toast.makeText(this, "バーコードカメラを開けませんでした。手入力も使えます。", Toast.LENGTH_LONG).show()
                showManualBarcodeDialog()
            }
    }

    private fun showManualBarcodeDialog() {
        val edit = EditText(this).apply {
            hint = "JAN（8桁または13桁）"
            inputType = InputType.TYPE_CLASS_NUMBER
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        AlertDialog.Builder(this)
            .setTitle("JANを入力")
            .setView(edit)
            .setNegativeButton("キャンセル", null)
            .setPositiveButton("写真へ") { _, _ ->
                val value = edit.text.toString().filter { it.isDigit() }
                if (value.isNotBlank()) openQuickCapture(value)
                else Toast.makeText(this, "JANを入力してください", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun openQuickCapture(barcode: String) {
        quickCapture.launch(Intent(this, QuickCaptureActivity::class.java).apply {
            putExtra(QuickCaptureActivity.EXTRA_BARCODE, barcode)
        })
    }

    private fun refresh() {
        val items = db.listPending().take(10)
        countText.text = "現在 ${db.countPending()} 商品を記録中"
        emptyText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        recentList.adapter = RecentAdapter(items)
    }

    private inner class RecentAdapter(private val items: List<Product>) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val p = items[position]
            val row = LinearLayout(this@ShoppingActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
                setOnClickListener {
                    startActivity(Intent(this@ShoppingActivity, ProductActivity::class.java).apply {
                        putExtra(ProductActivity.EXTRA_ID, p.id)
                    })
                }
            }
            val image = ImageView(this@ShoppingActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(0xFFECECEC.toInt())
                val bitmap = ImageUtils.decodeSampled(p.photoPath, 180, 180)
                if (bitmap != null) setImageBitmap(bitmap) else setImageResource(android.R.drawable.ic_menu_camera)
            }
            row.addView(image, LinearLayout.LayoutParams(dp(62), dp(62)).apply { marginEnd = dp(10) })

            val text = TextView(this@ShoppingActivity).apply {
                text = "${p.name.ifBlank { "商品名確認待ち" }}\n数量 ${p.quantity}${if (p.barcode.isNotBlank()) "  /  JAN ${p.barcode}" else ""}"
                textSize = 14f
            }
            row.addView(text, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            return row
        }
    }

    private fun fullWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
