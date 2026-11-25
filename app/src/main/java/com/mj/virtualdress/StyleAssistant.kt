package com.mj.virtualdress

data class StyleSuggestion(
    val title: String,
    val reason: String,
    val product: CatalogueProduct
)

object StyleAssistant {
    fun recommend(baseCategory: String, baseColor: String): List<StyleSuggestion> {
        val candidates = StoreCatalogue.products.filter { it.inStock }
        return candidates
            .filterNot { it.category.equals(baseCategory, ignoreCase = true) }
            .sortedByDescending { score(it, baseColor) }
            .take(3)
            .map {
                StyleSuggestion(
                    title = it.name,
                    reason = "Available in catalogue · " + it.category + " · " + it.colors.joinToString(),
                    product = it
                )
            }
    }

    private fun score(product: CatalogueProduct, color: String): Int {
        val neutral = setOf("Black", "White", "Blue")
        return when {
            product.colors.any { it.equals(color, true) } -> 4
            product.colors.any { it in neutral } -> 3
            else -> 1
        }
    }
}
