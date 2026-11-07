@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bakerylabel.pro.LocalApp
import com.bakerylabel.pro.R
import com.bakerylabel.pro.Routes
import com.bakerylabel.pro.data.CategoryEntity
import com.bakerylabel.pro.data.CategoryGroup
import com.bakerylabel.pro.data.ProductEntity
import com.bakerylabel.pro.ui.components.AppScaffold
import com.bakerylabel.pro.ui.components.ConfirmDialog
import com.bakerylabel.pro.ui.components.EmptyState
import kotlinx.coroutines.launch

/** Filters products for the dashboard shortcuts. */
fun filterProducts(
    all: List<ProductEntity>, cats: List<CategoryEntity>, filter: String, categoryId: Long?, query: String,
): List<ProductEntity> {
    val groupOf = cats.associate { it.id to it.groupType }
    return all.filter { p ->
        val g = p.categoryId?.let { groupOf[it] }
        val byFilter = when (filter) {
            "bakery" -> g == CategoryGroup.BAKERY
            "sweets" -> g == CategoryGroup.SWEETS
            "custom" -> p.isCustom || g == CategoryGroup.CUSTOM
            else -> true
        }
        byFilter && (categoryId == null || p.categoryId == categoryId) &&
            (query.isBlank() || p.name.contains(query.trim(), ignoreCase = true))
    }
}

@Composable
fun ProductsScreen(nav: NavController, filter: String, onBack: () -> Unit) {
    val repo = LocalApp.current.repo
    val scope = rememberCoroutineScope()
    val products by repo.products.observeAll().collectAsStateWithLifecycle(emptyList())
    val cats by repo.categories.observeAll().collectAsStateWithLifecycle(emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var catFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf<ProductEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ProductEntity?>(null) }

    val title = when (filter) {
        "bakery" -> stringResource(R.string.bakery_products)
        "sweets" -> stringResource(R.string.sweets_mithai)
        "custom" -> stringResource(R.string.custom_products)
        else -> stringResource(R.string.all_products)
    }
    val shownCats = when (filter) {
        "bakery" -> cats.filter { it.groupType == CategoryGroup.BAKERY }
        "sweets" -> cats.filter { it.groupType == CategoryGroup.SWEETS }
        else -> cats
    }
    val list = remember(products, cats, filter, catFilter, query) { filterProducts(products, cats, filter, catFilter, query) }
    val catName = cats.associate { it.id to it.name }

    AppScaffold(
        title = title,
        onBack = onBack,
        actions = {
            IconButton(onClick = { nav.navigate(Routes.CATEGORIES) }) {
                Icon(Icons.Filled.Category, stringResource(R.string.manage_categories))
            }
        },
        floating = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.add_product)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            OutlinedTextField(
                query, { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text(stringResource(R.string.search_products)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(catFilter == null, { catFilter = null }, { Text(stringResource(R.string.all)) })
                shownCats.forEach { c ->
                    FilterChip(catFilter == c.id, { catFilter = if (catFilter == c.id) null else c.id }, { Text(c.name) })
                }
            }
            if (list.isEmpty()) {
                EmptyState(Icons.Filled.SearchOff, stringResource(R.string.no_products_found))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(list, key = { it.id }) { p ->
                        ListItem(
                            headlineContent = { Text(p.name, style = MaterialTheme.typography.titleMedium) },
                            supportingContent = {
                                val parts = listOfNotNull(
                                    p.categoryId?.let { catName[it] },
                                    p.shelfLifeValue?.let { v -> stringResource(R.string.shelf_life_short, v, if (p.shelfLifeUnit == "M") stringResource(R.string.months) else stringResource(R.string.days)) },
                                    p.netWeight.takeIf { it.isNotBlank() },
                                    p.price.takeIf { it.isNotBlank() },
                                )
                                Text(parts.joinToString(" · "))
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { nav.navigate(Routes.editor(productId = p.id)) }) {
                                        Icon(Icons.Filled.NewLabel, stringResource(R.string.make_label), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { editing = p }) { Icon(Icons.Filled.Edit, stringResource(R.string.edit)) }
                                    IconButton(onClick = { deleting = p }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        val defaultCat = when (filter) {
            "bakery" -> cats.firstOrNull { it.groupType == CategoryGroup.BAKERY }?.id
            "sweets" -> cats.firstOrNull { it.groupType == CategoryGroup.SWEETS }?.id
            "custom" -> cats.firstOrNull { it.groupType == CategoryGroup.CUSTOM }?.id
            else -> catFilter
        }
        ProductDialog(
            initial = editing ?: ProductEntity(name = "", categoryId = defaultCat, isCustom = true),
            categories = cats,
            onSave = { p ->
                scope.launch { if (p.id == 0L) repo.products.insert(p) else repo.products.update(p) }
            },
            onDismiss = { adding = false; editing = null },
        )
    }
    deleting?.let { p ->
        ConfirmDialog(
            stringResource(R.string.delete_product), stringResource(R.string.delete_confirm, p.name), stringResource(R.string.delete),
            onConfirm = { scope.launch { repo.products.delete(p) } }, onDismiss = { deleting = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(categories: List<CategoryEntity>, selected: Long?, onSelect: (Long?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val none = stringResource(R.string.no_category)
    ExposedDropdownMenuBox(open, { open = it }, modifier) {
        OutlinedTextField(
            value = categories.firstOrNull { it.id == selected }?.name ?: none,
            onValueChange = {}, readOnly = true,
            label = { Text(stringResource(R.string.category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(open, { open = false }) {
            DropdownMenuItem({ Text(none) }, { onSelect(null); open = false })
            categories.forEach { c -> DropdownMenuItem({ Text(c.name) }, { onSelect(c.id); open = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(initial: ProductEntity, categories: List<CategoryEntity>, onSave: (ProductEntity) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var cat by rememberSaveable { mutableStateOf(initial.categoryId) }
    var shelf by rememberSaveable { mutableStateOf(initial.shelfLifeValue?.toString() ?: "") }
    var unit by rememberSaveable { mutableStateOf(initial.shelfLifeUnit ?: "D") }
    var weight by rememberSaveable { mutableStateOf(initial.netWeight) }
    var price by rememberSaveable { mutableStateOf(initial.price) }
    var ingr by rememberSaveable { mutableStateOf(initial.ingredients) }
    var storage by rememberSaveable { mutableStateOf(initial.storage) }
    var barcode by rememberSaveable { mutableStateOf(initial.barcode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) stringResource(R.string.add_product) else stringResource(R.string.edit_product)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.product_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                CategoryDropdown(categories, cat, { cat = it })
                Text(stringResource(R.string.shelf_life_optional), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    OutlinedTextField(
                        shelf, { v -> shelf = v.filter { it.isDigit() }.take(4) },
                        label = { Text(stringResource(R.string.shelf_life)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.weight(1.2f).padding(top = 8.dp)) {
                        SegmentedButton(unit == "D", { unit = "D" }, SegmentedButtonDefaults.itemShape(0, 2)) { Text(stringResource(R.string.days)) }
                        SegmentedButton(unit == "M", { unit = "M" }, SegmentedButtonDefaults.itemShape(1, 2)) { Text(stringResource(R.string.months)) }
                    }
                }
                OutlinedTextField(weight, { weight = it }, label = { Text(stringResource(R.string.net_weight)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(price, { price = it }, label = { Text(stringResource(R.string.price)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(ingr, { ingr = it }, label = { Text(stringResource(R.string.ingredients)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(storage, { storage = it }, label = { Text(stringResource(R.string.storage)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(barcode, { barcode = it }, label = { Text(stringResource(R.string.barcode)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                val sl = shelf.toIntOrNull()?.takeIf { it > 0 }
                onSave(initial.copy(
                    name = name.trim(), categoryId = cat,
                    shelfLifeValue = sl, shelfLifeUnit = if (sl != null) unit else null,
                    netWeight = weight.trim(), price = price.trim(), ingredients = ingr.trim(),
                    storage = storage.trim(), barcode = barcode.trim(),
                ))
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
