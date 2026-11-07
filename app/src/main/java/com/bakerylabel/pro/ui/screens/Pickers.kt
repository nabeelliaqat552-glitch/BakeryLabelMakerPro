@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bakerylabel.pro.R
import com.bakerylabel.pro.data.CategoryEntity
import com.bakerylabel.pro.data.ProductEntity
import com.bakerylabel.pro.model.LabelContent
import com.bakerylabel.pro.model.LabelDesign
import com.bakerylabel.pro.render.Templates
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakerylabel.pro.render.LabelTemplate
import com.bakerylabel.pro.ui.components.LabelPreview
import com.bakerylabel.pro.ui.components.PressCard
import com.bakerylabel.pro.util.Dates

@Composable
fun ProductPickerDialog(
    products: List<ProductEntity>,
    categories: List<CategoryEntity>,
    onPick: (ProductEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var q by remember { mutableStateOf("") }
    val catName = categories.associate { it.id to it.name }
    val list = products.filter { q.isBlank() || it.name.contains(q.trim(), true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_product)) },
        text = {
            Column {
                OutlinedTextField(q, { q = it }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text(stringResource(R.string.search_products)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(list, key = { it.id }) { p ->
                        ListItem(
                            headlineContent = { Text(p.name) },
                            supportingContent = { p.categoryId?.let { catName[it] }?.let { Text(it) } },
                            modifier = Modifier.clickable { onPick(p); onDismiss() },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

fun sampleContent(shopName: String, productName: String) = LabelContent(
    shopName = shopName.ifBlank { "Royal Sweets & Bakers" },
    productName = productName,
    netWeight = "500 g",
    price = "Rs. 850",
    expEpochDay = Dates.today() + 7,
)

@Composable
fun TemplateGrid(
    templates: List<LabelTemplate>,
    shopName: String,
    logo: Bitmap?,
    dateFormat: String,
    onPick: (LabelTemplate) -> Unit,
    modifier: Modifier = Modifier,
    onLongPick: ((LabelTemplate) -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(templates, key = { it.id }) { t ->
            val sample = remember(t.id, shopName) { sampleContent(shopName, t.name) }
            PressCard(onClick = { onPick(t) }) {
                Column(Modifier.padding(10.dp)) {
                    LabelPreview(sample, t.design, logo, dateFormat, maxHeight = 90.dp)
                    Text(t.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp))
                    Text(
                        t.category + " · " + "%.0f×%.0f mm".format(t.design.widthMm, t.design.heightMm),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (onLongPick != null && t.userTemplateId != null) {
                        TextButton(onClick = { onLongPick(t) }) { Text(stringResource(R.string.delete)) }
                    }
                }
            }
        }
    }
}

@Composable
fun TemplatePickerDialog(
    templates: List<LabelTemplate>,
    shopName: String,
    logo: Bitmap?,
    dateFormat: String,
    onPick: (LabelTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_template)) },
        text = {
            TemplateGrid(templates, shopName, logo, dateFormat, { onPick(it); onDismiss() }, Modifier.heightIn(max = 520.dp))
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Built-in templates plus the user's own saved templates. */
@Composable
fun rememberAllTemplates(): List<LabelTemplate> {
    val repo = com.bakerylabel.pro.LocalApp.current.repo
    val user by repo.templates.observeAll().collectAsStateWithLifecycle(emptyList())
    return remember(user) {
        user.map {
            LabelTemplate("user_${it.id}", it.name, "My templates", LabelDesign.fromJsonString(it.designJson), it.id)
        } + Templates.builtIn
    }
}
