package com.mj.virtualdress

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SavedLook(
    val id: Long,
    val resultDataUri: String,
    val productCode: String?,
    val productName: String?,
    val price: Int?,
    val color: String?,
    val size: String?
)

data class SelectedProduct(
    val product: CatalogueProduct,
    val color: String,
    val size: String
)

object CustomerSessionStore {
    private val _looks = MutableStateFlow<List<SavedLook>>(emptyList())
    val looks: StateFlow<List<SavedLook>> = _looks.asStateFlow()

    private val _selected = MutableStateFlow<List<SelectedProduct>>(emptyList())
    val selected: StateFlow<List<SelectedProduct>> = _selected.asStateFlow()

    fun saveLook(look: SavedLook) {
        _looks.value = listOf(look) + _looks.value.filterNot { it.id == look.id }
    }

    fun removeLook(id: Long) {
        _looks.value = _looks.value.filterNot { it.id == id }
    }

    fun addProduct(item: SelectedProduct) {
        if (_selected.value.none { it.product.code == item.product.code && it.color == item.color && it.size == item.size }) {
            _selected.value = _selected.value + item
        }
    }

    fun removeProduct(code: String) {
        _selected.value = _selected.value.filterNot { it.product.code == code }
    }

    fun clear() {
        _looks.value = emptyList()
        _selected.value = emptyList()
    }
}
