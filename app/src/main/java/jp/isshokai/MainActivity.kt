package jp.isshokai

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var db: ProductDb
    private lateinit var statsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = ProductDb(this)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "いっしょ買い"
            textSize = 31f
        })
        root.addView(TextView(this).apply {
            text = "店頭でご両親と選び、帰宅後にネットスーパーで注文するための買い物メモ。"
            textSize = 15f
            setPadding(0, dp(6), 0, dp(16))
        })

        root.addView(TextView(this).apply {
            text = "おすすめ：2台運用"
            textSize = 18f
        })
        root.addView(TextView(this).apply {
            text = "① 1台はLINEビデオ通話をつけたまま商品棚を映す\n② このアプリを入れたスマホでJANスキャンと商品写真を記録\n\n同じスマホでLINEとカメラを切り替えるより、通話が途切れにくく使いやすい構成です。"
            textSize = 14f
            setPadding(0, dp(4), 0, dp(20))
        })

        root.addView(Button(this).apply {
            text = "店頭で商品を記録する"
            textSize = 21f
            minHeight = dp(76)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ShoppingActivity::class.java))
            }
        }, fullWidth())

        root.addView(Button(this).apply {
            text = "帰宅後：確認して注文する"
            textSize = 20f
            minHeight = dp(72)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ReviewActivity::class.java))
            }
        }, fullWidth().apply { topMargin = dp(10) })

        statsText = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 16f
            setPadding(dp(12), dp(20), dp(12), dp(12))
        }
        root.addView(statsText, fullWidth())

        root.addView(TextView(this).apply {
            text = "店頭での基本操作\nご両親が『これ』と決める → JANを読む → 写真を撮る → 次の商品。数量や商品名の修正は帰宅後で大丈夫です。"
            textSize = 15f
            setPadding(0, dp(18), 0, 0)
        })

        return scroll
    }

    private fun refreshStats() {
        statsText.text = "未注文 ${db.countPending()}商品　／　注文済み ${db.countOrdered()}商品"
    }

    private fun fullWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
