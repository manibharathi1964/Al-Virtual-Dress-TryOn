package com.mj.virtualdress

object StaffMode {
    fun findProduct(rawCode: String): CatalogueProduct? =
        StoreCatalogue.products.firstOrNull { it.code.equals(rawCode.trim(), ignoreCase = true) }
}
