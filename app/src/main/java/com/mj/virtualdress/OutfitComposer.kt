package com.mj.virtualdress

data class OutfitSelection(
    val products: List<CatalogueProduct>
) {
    val primary: CatalogueProduct? get() = products.firstOrNull()
    fun instruction(): String {
        if (products.isEmpty()) return ""
        return products.joinToString(prefix = "Compose the selected store outfit with: ", separator = "; ") {
            it.category + " " + it.name + ", available colors " + it.colors.joinToString()
        } + ". Preserve natural layering and proportions where supported."
    }
}

object OutfitComposer {
    fun add(current: List<CatalogueProduct>, product: CatalogueProduct): List<CatalogueProduct> =
        if (current.any { it.code == product.code }) current else current + product
    fun remove(current: List<CatalogueProduct>, code: String): List<CatalogueProduct> =
        current.filterNot { it.code == code }
}