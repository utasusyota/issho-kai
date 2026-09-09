package jp.isshokai

import android.content.Context
import java.util.concurrent.Executors

/** Keeps lookups independent from short-lived camera/screens. */
object ProductLookupQueue {
    private val executor = Executors.newSingleThreadExecutor()

    fun enqueue(context: Context, productId: Long, barcode: String) {
        if (productId == 0L || barcode.isBlank()) return
        val appContext = context.applicationContext
        executor.execute {
            val result = ProductLookup.lookup(barcode) ?: return@execute
            val db = ProductDb(appContext)
            try {
                db.updateLookup(productId, result.name, result.detail)
            } finally {
                db.close()
            }
        }
    }
}
