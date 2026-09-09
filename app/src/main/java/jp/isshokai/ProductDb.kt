package jp.isshokai

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ProductDb(context: Context) : SQLiteOpenHelper(context, "shopping.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                barcode TEXT NOT NULL DEFAULT '',
                name TEXT NOT NULL DEFAULT '',
                quantity INTEGER NOT NULL DEFAULT 1,
                note TEXT NOT NULL DEFAULT '',
                photo_path TEXT NOT NULL DEFAULT '',
                ordered INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE products ADD COLUMN ordered INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE products ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE products SET updated_at = created_at WHERE updated_at = 0")
        }
    }

    fun listProducts(includeOrdered: Boolean = true): List<Product> {
        val selection = if (includeOrdered) null else "ordered = 0"
        return queryProducts(selection, null)
    }

    fun listPending(): List<Product> = queryProducts("ordered = 0", null)

    fun getProduct(id: Long): Product? {
        readableDatabase.query(
            "products", null, "id = ?", arrayOf(id.toString()), null, null, null
        ).use { c ->
            return if (c.moveToFirst()) c.toProduct() else null
        }
    }

    fun findPendingByBarcode(barcode: String): Product? {
        if (barcode.isBlank()) return null
        readableDatabase.query(
            "products",
            null,
            "barcode = ? AND ordered = 0",
            arrayOf(barcode),
            null,
            null,
            "updated_at DESC",
            "1"
        ).use { c ->
            return if (c.moveToFirst()) c.toProduct() else null
        }
    }

    fun save(product: Product): Long {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("barcode", product.barcode)
            put("name", product.name)
            put("quantity", product.quantity.coerceAtLeast(1))
            put("note", product.note)
            put("photo_path", product.photoPath)
            put("ordered", if (product.ordered) 1 else 0)
            put("created_at", product.createdAt)
            put("updated_at", now)
        }
        return if (product.id == 0L) {
            writableDatabase.insertOrThrow("products", null, values)
        } else {
            writableDatabase.update("products", values, "id = ?", arrayOf(product.id.toString()))
            product.id
        }
    }

    fun updateLookup(id: Long, name: String, detailNote: String) {
        val current = getProduct(id) ?: return
        val values = ContentValues().apply {
            if (current.name.isBlank() && name.isNotBlank()) put("name", name)
            if (current.note.isBlank() && detailNote.isNotBlank()) put("note", detailNote)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update("products", values, "id = ?", arrayOf(id.toString()))
    }

    fun incrementQuantity(id: Long): Int {
        val current = getProduct(id) ?: return 1
        val newQuantity = current.quantity + 1
        val values = ContentValues().apply {
            put("quantity", newQuantity)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update("products", values, "id = ?", arrayOf(id.toString()))
        return newQuantity
    }

    fun setOrdered(id: Long, ordered: Boolean) {
        val values = ContentValues().apply {
            put("ordered", if (ordered) 1 else 0)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update("products", values, "id = ?", arrayOf(id.toString()))
    }

    fun countPending(): Int = count("ordered = 0")
    fun countOrdered(): Int = count("ordered = 1")

    fun delete(id: Long) {
        writableDatabase.delete("products", "id = ?", arrayOf(id.toString()))
    }

    private fun count(selection: String): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM products WHERE $selection", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    private fun queryProducts(selection: String?, args: Array<String>?): List<Product> {
        val result = mutableListOf<Product>()
        readableDatabase.query(
            "products", null, selection, args, null, null, "ordered ASC, updated_at DESC"
        ).use { c ->
            while (c.moveToNext()) result += c.toProduct()
        }
        return result
    }

    private fun Cursor.toProduct(): Product = Product(
        id = getLong(getColumnIndexOrThrow("id")),
        barcode = getString(getColumnIndexOrThrow("barcode")),
        name = getString(getColumnIndexOrThrow("name")),
        quantity = getInt(getColumnIndexOrThrow("quantity")),
        note = getString(getColumnIndexOrThrow("note")),
        photoPath = getString(getColumnIndexOrThrow("photo_path")),
        ordered = getInt(getColumnIndexOrThrow("ordered")) == 1,
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        updatedAt = getLong(getColumnIndexOrThrow("updated_at"))
    )
}
