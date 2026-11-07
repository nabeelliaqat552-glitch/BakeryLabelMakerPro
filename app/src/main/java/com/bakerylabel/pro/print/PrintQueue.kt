package com.bakerylabel.pro.print

import com.bakerylabel.pro.model.LabelContent
import com.bakerylabel.pro.model.LabelDesign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class PrintItem(
    val name: String,
    val content: LabelContent,
    val design: LabelDesign,
    val copies: Int = 1,
    val key: String = UUID.randomUUID().toString(),
)

/** Labels waiting to be printed on an A4 sheet / exported together. Shared between screens. */
class PrintQueue {
    private val _items = MutableStateFlow<List<PrintItem>>(emptyList())
    val items: StateFlow<List<PrintItem>> = _items

    fun set(list: List<PrintItem>) { _items.value = list }
    fun add(list: List<PrintItem>) = _items.update { it + list }
    fun remove(key: String) = _items.update { l -> l.filterNot { it.key == key } }
    fun setCopies(key: String, copies: Int) = _items.update { l ->
        l.map { if (it.key == key) it.copy(copies = copies.coerceIn(1, 999)) else it }
    }
    fun clear() { _items.value = emptyList() }
}
