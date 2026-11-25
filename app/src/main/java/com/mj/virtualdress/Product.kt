package com.mj.virtualdress

data class ProductVariant(
    val color: String,
    val sizes: List<String>,
    val stockBySize: Map<String, Int>,
    val pattern: String = "Solid",
    val material: String = "Standard"
) {
    val inStock: Boolean
        get() = stockBySize.values.any { it > 0 }
}

data class CatalogueProduct(
    val code: String,
    val name: String,
    val category: String,
    val price: Int,
    val imageUrl: String,
    val variants: List<ProductVariant>
) {
    val colors: List<String> get() = variants.map { it.color }
    val sizes: List<String> get() = variants.flatMap { it.sizes }.distinct()
    val inStock: Boolean get() = variants.any { it.inStock }
}

object StoreCatalogue {
    val products = listOf(
        CatalogueProduct(
            "SH-001", "Classic Black Shirt", "Shirt", 899,
            "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=85",
            listOf(ProductVariant("Black", listOf("S", "M", "L", "XL"), mapOf("S" to 4, "M" to 8, "L" to 6, "XL" to 2)))
        ),
        CatalogueProduct(
            "JN-014", "Premium Blue Jeans", "Jeans", 1299,
            "https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=800&q=85",
            listOf(ProductVariant("Blue", listOf("28", "30", "32", "34"), mapOf("28" to 3, "30" to 7, "32" to 5, "34" to 1)))
        ),
        CatalogueProduct(
            "JK-021", "Modern Jacket", "Jacket", 1799,
            "https://images.unsplash.com/photo-1551488831-00ddcb6c6bd3?auto=format&fit=crop&w=800&q=85",
            listOf(ProductVariant("Black", listOf("M", "L", "XL"), mapOf("M" to 2, "L" to 4, "XL" to 1)))
        ),
        CatalogueProduct(
            "KT-031", "Elegant Kurti", "Kurti", 1099,
            "https://images.unsplash.com/photo-1583391733956-6c78276477e2?auto=format&fit=crop&w=800&q=85",
            listOf(ProductVariant("Green", listOf("S", "M", "L", "XL"), mapOf("S" to 3, "M" to 6, "L" to 2, "XL" to 0)))
        )
    )
}
