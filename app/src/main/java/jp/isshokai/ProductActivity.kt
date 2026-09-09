package jp.isshokai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class ProductActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ID = "id"
    }

    private lateinit var db: ProductDb
    private var productId = 0L
    private var createdAt = System.currentTimeMillis()
    private var photoPath = ""
    private var pendingPhotoPath = ""

    private lateinit var barcodeEdit: EditText
    private lateinit var nameEdit: EditText
    private lateinit var quantityEdit: EditText
    private lateinit var noteEdit: EditText
    private lateinit var orderedCheck: CheckBox
    private lateinit var photoView: ImageView
    private lateinit var lookupStatus: TextView

    private val executor = Executors.newSingleThreadExecutor()

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && pendingPhotoPath.isNotBlank()) {
            ImageUtils.normalizePhoto(File(pendingPhotoPath))
            val oldPhotoPath = photoPath
            photoPath = pendingPhotoPath
            save(silent = true)
            if (oldPhotoPath.isNotBlank() && oldPhotoPath != photoPath) File(oldPhotoPath).delete()
            showPhoto()
        } else if (pendingPhotoPath.isNotBlank()) {
            File(pendingPhotoPath).delete()
        }
        pendingPhotoPath = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = ProductDb(this)
        productId = intent.getLongExtra(EXTRA_ID, 0L)
        setContentView(buildUi())
        loadProduct()
        if (savedInstanceState != null) {
            pendingPhotoPath = savedInstanceState.getString("pendingPhotoPath").orEmpty()
            barcodeEdit.setText(savedInstanceState.getString("barcode"))
            nameEdit.setText(savedInstanceState.getString("name"))
            quantityEdit.setText(savedInstanceState.getString("quantity", "1"))
            noteEdit.setText(savedInstanceState.getString("note"))
            orderedCheck.isChecked = savedInstanceState.getBoolean("ordered")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pendingPhotoPath", pendingPhotoPath)
        outState.putString("barcode", barcodeEdit.text.toString())
        outState.putString("name", nameEdit.text.toString())
        outState.putString("quantity", quantityEdit.text.toString())
        outState.putString("note", noteEdit.text.toString())
        outState.putBoolean("ordered", orderedCheck.isChecked)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "商品を確認"
            textSize = 26f
        })

        photoView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(0xFFECECEC.toInt())
            setImageResource(android.R.drawable.ic_menu_camera)
        }
        root.addView(photoView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(220)
        ).apply { topMargin = dp(12) })

        root.addView(Button(this).apply {
            text = "写真を撮り直す"
            setOnClickListener { capturePhoto() }
        }, fullWidth())

        barcodeEdit = addField(root, "JAN / バーコード", false).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        root.addView(Button(this).apply {
            text = "JANから商品名をもう一度取得"
            setOnClickListener { lookupProduct() }
        }, fullWidth())

        lookupStatus = TextView(this).apply {
            textSize = 13f
            setPadding(0, dp(4), 0, dp(6))
        }
        root.addView(lookupStatus, fullWidth())

        nameEdit = addField(root, "商品名", false)

        root.addView(TextView(this).apply {
            text = "数量"
            textSize = 14f
            setPadding(0, dp(10), 0, dp(2))
        })
        val qtyRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        qtyRow.addView(Button(this).apply {
            text = "−"
            setOnClickListener { changeQuantity(-1) }
        }, LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT))
        quantityEdit = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            setText("1")
        }
        qtyRow.addView(quantityEdit, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        qtyRow.addView(Button(this).apply {
            text = "+"
            setOnClickListener { changeQuantity(1) }
        }, LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(qtyRow, fullWidth())

        noteEdit = addField(root, "メモ（味・サイズ・ご両親の希望など）", true)

        orderedCheck = CheckBox(this).apply {
            text = "イオンネットスーパーで注文済み"
            textSize = 16f
            setPadding(0, dp(12), 0, dp(8))
        }
        root.addView(orderedCheck, fullWidth())

        root.addView(Button(this).apply {
            text = "イオンネットスーパーで探す"
            textSize = 18f
            minHeight = dp(60)
            setOnClickListener {
                save(silent = true)
                openAeonSearch()
            }
        }, fullWidth().apply { topMargin = dp(8) })

        root.addView(Button(this).apply {
            text = "保存して一覧へ戻る"
            setOnClickListener {
                save(silent = false)
                finish()
            }
        }, fullWidth().apply { topMargin = dp(8) })

        root.addView(Button(this).apply {
            text = "削除"
            setOnClickListener { confirmDelete() }
        }, fullWidth())

        return scroll
    }

    private fun loadProduct() {
        val p = db.getProduct(productId)
        if (p == null) {
            Toast.makeText(this, "商品が見つかりません", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        createdAt = p.createdAt
        photoPath = p.photoPath
        barcodeEdit.setText(p.barcode)
        nameEdit.setText(p.name)
        quantityEdit.setText(p.quantity.toString())
        noteEdit.setText(p.note)
        orderedCheck.isChecked = p.ordered
        showPhoto()

        if (p.name.isBlank() && p.barcode.isNotBlank()) lookupProduct()
    }

    private fun addField(root: LinearLayout, label: String, multiline: Boolean): EditText {
        root.addView(TextView(this).apply {
            text = label
            textSize = 14f
            setPadding(0, dp(10), 0, dp(2))
        })
        val edit = EditText(this).apply {
            setSingleLine(!multiline)
            if (multiline) {
                minLines = 3
                gravity = Gravity.TOP
            }
        }
        root.addView(edit, fullWidth())
        return edit
    }

    private fun changeQuantity(delta: Int) {
        val now = quantityEdit.text.toString().toIntOrNull() ?: 1
        quantityEdit.setText((now + delta).coerceAtLeast(1).toString())
    }

    private fun capturePhoto() {
        val dir = File(filesDir, "product_photos").apply { mkdirs() }
        val file = File(dir, "product_${System.currentTimeMillis()}.jpg")
        pendingPhotoPath = file.absolutePath
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        takePicture.launch(uri)
    }

    private fun showPhoto() {
        if (photoPath.isNotBlank() && File(photoPath).exists()) {
            val bitmap = ImageUtils.decodeSampled(photoPath, 1000, 1000)
            if (bitmap != null) photoView.setImageBitmap(bitmap) else photoView.setImageResource(android.R.drawable.ic_menu_camera)
        } else {
            photoView.setImageResource(android.R.drawable.ic_menu_camera)
        }
    }

    private fun lookupProduct() {
        val barcode = barcodeEdit.text.toString().trim()
        if (barcode.isBlank()) {
            lookupStatus.text = "JANがないため自動取得できません。"
            return
        }
        lookupStatus.text = "商品情報を検索中…"
        executor.execute {
            val result = ProductLookup.lookup(barcode)
            runOnUiThread {
                if (result != null) {
                    if (nameEdit.text.toString().isBlank()) nameEdit.setText(result.name)
                    if (noteEdit.text.toString().isBlank() && result.detail.isNotBlank()) noteEdit.setText(result.detail)
                    lookupStatus.text = "商品情報を取得しました。"
                } else {
                    lookupStatus.text = "商品名を自動取得できませんでした。写真とJANで確認してください。"
                }
            }
        }
    }

    private fun save(silent: Boolean) {
        val qty = quantityEdit.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
        db.save(
            Product(
                id = productId,
                barcode = barcodeEdit.text.toString().trim(),
                name = nameEdit.text.toString().trim(),
                quantity = qty,
                note = noteEdit.text.toString().trim(),
                photoPath = photoPath,
                ordered = orderedCheck.isChecked,
                createdAt = createdAt
            )
        )
        if (!silent) Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show()
    }

    private fun openAeonSearch() {
        val query = nameEdit.text.toString().trim().ifBlank { barcodeEdit.text.toString().trim() }
        if (query.isBlank()) {
            Toast.makeText(this, "商品名かJANを入力してください", Toast.LENGTH_SHORT).show()
            return
        }
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shop.aeon.com/netsuper/search/?q=$encoded")))
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("この商品を削除しますか？")
            .setMessage("買い物リストから削除します。")
            .setNegativeButton("キャンセル", null)
            .setPositiveButton("削除") { _, _ ->
                db.delete(productId)
                if (photoPath.isNotBlank()) File(photoPath).delete()
                finish()
            }
            .show()
    }

    private fun fullWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
