package jp.isshokai

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class QuickCaptureActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_BARCODE = "barcode"
        const val EXTRA_SCAN_NEXT = "scan_next"
    }

    private lateinit var db: ProductDb
    private lateinit var barcode: String
    private var pendingPhotoPath = ""
    private var savedProductId = 0L
    private var saved = false

    private lateinit var statusText: TextView
    private lateinit var photoView: ImageView
    private lateinit var captureButton: Button
    private lateinit var saveNoPhotoButton: Button
    private lateinit var nextButton: Button
    private lateinit var finishButton: Button
    private lateinit var plusOneButton: Button

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && pendingPhotoPath.isNotBlank()) {
            val file = File(pendingPhotoPath)
            ImageUtils.normalizePhoto(file)
            showPhoto(pendingPhotoPath)
            saveSelection(pendingPhotoPath)
        } else {
            if (pendingPhotoPath.isNotBlank()) File(pendingPhotoPath).delete()
            pendingPhotoPath = ""
            statusText.text = "写真を撮りませんでした。撮り直すか、写真なしで保存できます。"
            captureButton.visibility = View.VISIBLE
            saveNoPhotoButton.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = ProductDb(this)
        barcode = intent.getStringExtra(EXTRA_BARCODE).orEmpty().trim()
        pendingPhotoPath = savedInstanceState?.getString("pendingPhotoPath").orEmpty()
        savedProductId = savedInstanceState?.getLong("savedProductId") ?: 0L
        saved = savedInstanceState?.getBoolean("saved") ?: false
        setContentView(buildUi())

        if (saved) {
            statusText.text = "登録済みです。"
            db.getProduct(savedProductId)?.let { showPhoto(it.photoPath) }
            plusOneButton.visibility = View.VISIBLE
            nextButton.visibility = View.VISIBLE
            finishButton.visibility = View.VISIBLE
        } else if (savedInstanceState != null && pendingPhotoPath.isBlank()) {
            statusText.text = "撮影するか、写真なしで保存できます。"
            captureButton.visibility = View.VISIBLE
            saveNoPhotoButton.visibility = View.VISIBLE
        }
        if (savedInstanceState == null) photoView.post { capturePhoto() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pendingPhotoPath", pendingPhotoPath)
        outState.putLong("savedProductId", savedProductId)
        outState.putBoolean("saved", saved)
        super.onSaveInstanceState(outState)
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(18))
        }

        root.addView(TextView(this).apply {
            text = "商品写真"
            textSize = 26f
        })
        root.addView(TextView(this).apply {
            text = if (barcode.isBlank()) "バーコードなし商品" else "JAN $barcode"
            textSize = 14f
            setPadding(0, dp(4), 0, dp(10))
        })

        photoView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(0xFFECECEC.toInt())
            setImageResource(android.R.drawable.ic_menu_camera)
        }
        root.addView(photoView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        statusText = TextView(this).apply {
            text = "カメラを開きます…"
            gravity = Gravity.CENTER
            textSize = 16f
            setPadding(dp(4), dp(14), dp(4), dp(10))
        }
        root.addView(statusText, fullWidth())

        captureButton = Button(this).apply {
            text = "写真を撮り直す"
            visibility = View.GONE
            setOnClickListener { capturePhoto() }
        }
        root.addView(captureButton, fullWidth())

        saveNoPhotoButton = Button(this).apply {
            text = "写真なしで保存"
            visibility = View.GONE
            setOnClickListener { saveSelection("") }
        }
        root.addView(saveNoPhotoButton, fullWidth())

        plusOneButton = Button(this).apply {
            text = "同じ商品をもう1個  ＋1"
            visibility = View.GONE
            setOnClickListener {
                if (savedProductId != 0L) {
                    val q = db.incrementQuantity(savedProductId)
                    text = "数量 $q 個（さらに＋1）"
                    statusText.text = "数量を $q 個にしました。"
                }
            }
        }
        root.addView(plusOneButton, fullWidth())

        nextButton = Button(this).apply {
            text = "次の商品をスキャン"
            textSize = 20f
            minHeight = dp(72)
            visibility = View.GONE
            setOnClickListener {
                setResult(RESULT_OK, android.content.Intent().putExtra(EXTRA_SCAN_NEXT, true))
                finish()
            }
        }
        root.addView(nextButton, fullWidth().apply { topMargin = dp(8) })

        finishButton = Button(this).apply {
            text = "商品一覧に戻る"
            visibility = View.GONE
            setOnClickListener {
                setResult(RESULT_OK, android.content.Intent().putExtra(EXTRA_SCAN_NEXT, false))
                finish()
            }
        }
        root.addView(finishButton, fullWidth())

        root.addView(Button(this).apply {
            text = "キャンセル"
            setOnClickListener {
                if (!saved && pendingPhotoPath.isNotBlank()) File(pendingPhotoPath).delete()
                setResult(RESULT_CANCELED)
                finish()
            }
        }, fullWidth())

        return root
    }

    private fun capturePhoto() {
        if (saved) return
        val dir = File(filesDir, "product_photos").apply { mkdirs() }
        val file = File(dir, "product_${System.currentTimeMillis()}.jpg")
        pendingPhotoPath = file.absolutePath
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        takePicture.launch(uri)
    }

    private fun saveSelection(photoPath: String) {
        if (saved) return
        saved = true

        val existing = db.findPendingByBarcode(barcode)
        if (existing != null && barcode.isNotBlank()) {
            savedProductId = existing.id
            val newQuantity = db.incrementQuantity(existing.id)
            if (photoPath.isNotBlank()) {
                if (existing.photoPath.isBlank()) {
                    db.save(existing.copy(photoPath = photoPath, quantity = newQuantity))
                } else {
                    File(photoPath).delete()
                }
            }
            statusText.text = "同じ商品なので数量を $newQuantity 個にしました。"
        } else {
            savedProductId = db.save(Product(barcode = barcode, photoPath = photoPath, quantity = 1))
            statusText.text = "登録しました。"
        }

        pendingPhotoPath = ""
        captureButton.visibility = View.GONE
        saveNoPhotoButton.visibility = View.GONE
        plusOneButton.visibility = View.VISIBLE
        nextButton.visibility = View.VISIBLE
        finishButton.visibility = View.VISIBLE

        if (barcode.isNotBlank()) ProductLookupQueue.enqueue(this, savedProductId, barcode)
    }

    private fun showPhoto(path: String) {
        val bitmap = ImageUtils.decodeSampled(path, 1000, 1000)
        if (bitmap != null) photoView.setImageBitmap(bitmap)
    }

    private fun fullWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
