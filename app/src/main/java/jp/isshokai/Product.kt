package jp.isshokai

data class Product(
    val id: Long = 0L,
    val barcode: String = "",
    val name: String = "",
    val quantity: Int = 1,
    val note: String = "",
    val photoPath: String = "",
    val ordered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
