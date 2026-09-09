package jp.isshokai

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class ReviewActivity : AppCompatActivity() {
    private lateinit var db: ProductDb
    private lateinit var listView: ListView
    private lateinit var emptyText: TextView
    private lateinit var summaryText: TextView
    private lateinit var filterButton: Button
    private var showAll = false

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
            text = "確認して注文"
            textSize = 27f
        })
        root.addView(TextView(this).apply {
            text = "商品を確認しながら、イオンネットスーパーで探します。"
            textSize = 14f
            setPadding(0, dp(4), 0, dp(10))
        })

        summaryText = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 16f
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(summaryText, fullWidth())

        filterButton = Button(this).apply {
            setOnClickListener {
                showAll = !showAll
                refresh()
            }
        }
        root.addView(filterButton, fullWidth())

        emptyText = TextView(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(28), 0, dp(28))
        }
        root.addView(emptyText, fullWidth())

        listView = ListView(this).apply { dividerHeight = dp(1) }
        root.addView(listView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        root.addView(Button(this).apply {
            text = "戻る"
            setOnClickListener { finish() }
        }, fullWidth().apply { topMargin = dp(8) })

        return root
    }

    private fun refresh() {
        val items = db.listProducts(includeOrdered = showAll)
        val pending = db.countPending()
        val ordered = db.countOrdered()
        summaryText.text = "未注文 $pending 商品　／　注文済み $ordered 商品"
        filterButton.text = if (showAll) "未注文だけ表示" else "注文済みも表示"
        emptyText.text = if (showAll) "商品はまだありません。" else "未注文の商品はありません。"
        emptyText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        listView.adapter = ProductAdapter(items)
    }

    private inner class ProductAdapter(private val items: List<Product>) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val p = items[position]
            val row = LinearLayout(this@ReviewActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(10))
                setOnClickListener {
                    startActivity(Intent(this@ReviewActivity, ProductActivity::class.java).apply {
                        putExtra(ProductActivity.EXTRA_ID, p.id)
                    })
                }
            }

            val image = ImageView(this@ReviewActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(0xFFECECEC.toInt())
                if (p.photoPath.isNotBlank() && File(p.photoPath).exists()) {
                    val bitmap = ImageUtils.decodeSampled(p.photoPath, 200, 200)
                    if (bitmap != null) setImageBitmap(bitmap) else setImageResource(android.R.drawable.ic_menu_camera)
                } else {
                    setImageResource(android.R.drawable.ic_menu_camera)
                }
            }
            row.addView(image, LinearLayout.LayoutParams(dp(72), dp(72)).apply { marginEnd = dp(12) })

            val area = LinearLayout(this@ReviewActivity).apply { orientation = LinearLayout.VERTICAL }
            area.addView(TextView(this@ReviewActivity).apply {
                text = p.name.ifBlank { "商品名未確認" }
                textSize = 17f
            })
            area.addView(TextView(this@ReviewActivity).apply {
                text = "数量 ${p.quantity}${if (p.barcode.isNotBlank()) "  /  JAN ${p.barcode}" else ""}"
                textSize = 13f
            })
            area.addView(TextView(this@ReviewActivity).apply {
                text = if (p.ordered) "✓ 注文済み" else "未注文 → タップして確認"
                textSize = 13f
            })
            row.addView(area, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            return row
        }
    }

    private fun fullWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
