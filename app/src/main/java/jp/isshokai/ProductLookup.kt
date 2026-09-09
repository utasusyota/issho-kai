package jp.isshokai

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ProductLookup {
    data class Result(val name: String, val detail: String)

    fun lookup(barcode: String): Result? {
        if (barcode.isBlank()) return null
        var connection: HttpURLConnection? = null
        return try {
            val encoded = URLEncoder.encode(barcode, StandardCharsets.UTF_8.name())
            val endpoint = "https://world.openfoodfacts.org/api/v2/product/$encoded.json?fields=product_name_ja,product_name,brands,quantity"
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "IsshoKai/0.2 personal-shopping-prototype")
            }
            if (connection.responseCode !in 200..299) return null
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            if (root.optInt("status", 0) != 1) return null
            val product = root.optJSONObject("product") ?: return null
            val name = product.optString("product_name_ja")
                .ifBlank { product.optString("product_name") }
                .trim()
            if (name.isBlank()) return null
            val detail = listOfNotNull(
                product.optString("brands").takeIf { it.isNotBlank() }?.let { "ブランド: $it" },
                product.optString("quantity").takeIf { it.isNotBlank() }?.let { "内容量: $it" }
            ).joinToString(" / ")
            Result(name, detail)
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
