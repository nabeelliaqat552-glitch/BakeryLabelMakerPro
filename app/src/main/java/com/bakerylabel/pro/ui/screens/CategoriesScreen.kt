@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakerylabel.pro.LocalApp
import com.bakerylabel.pro.R
import com.bakerylabel.pro.data.CategoryEntity
import com.bakerylabel.pro.data.CategoryGroup
import com.bakerylabel.pro.ui.components.AppScaffold
import com.bakerylabel.pro.ui.components.ConfirmDialog
import kotlinx.coroutines.launch

@Composable
fun groupLabel(g: String): String = when (g) {
    CategoryGroup.SWEETS -> stringResource(R.string.group_sweets)
    CategoryGroup.BAKERY -> stringResource(R.string.group_bakery)
    CategoryGroup.OTHER -> stringResource(R.string.group_other)
    else -> stringResource(R.string.group_custom)
}

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val repo = LocalApp.current.repo
    val scope = rememberCoroutineScope()
    val cats by repo.categories.observeAll().collectAsStateWithLifecycle(emptyList())
    val products by repo.products.observeAll().collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CategoryEntity?>(null) }
    val counts = products.groupingBy { it.categoryId }.eachCount()

    AppScaffold(
        title = stringResource(R.string.manage_categories),
        onBack = onBack,
        floating = {
            ExtendedFloatingActionButton(
                onClick = { adding = true }, icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.add_category)) },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 96.dp)) {
            items(cats, key = { it.id }) { c ->
                ListItem(
                    headlineContent = { Text(c.name, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text(groupLabel(c.groupType) + " · " + stringResource(R.string.n_products, counts[c.id] ?: 0)) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { editing = c }) { Icon(Icons.Filled.Edit, stringResource(R.string.edit)) }
                            IconButton(onClick = { deleting = c }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                        }
                    },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }

    if (adding || editing != null) {
        CategoryDialog(
            initial = editing ?: CategoryEntity(name = "", groupType = CategoryGroup.CUSTOM, sortOrder = cats.size),
            onSave = { c -> scope.launch { if (c.id == 0L) repo.categories.insert(c) else repo.categories.update(c) } },
            onDismiss = { adding = false; editing = null },
        )
    }
    deleting?.let { c ->
        ConfirmDialog(
            stringResource(R.string.delete_category), stringResource(R.string.delete_category_confirm, c.name),
            stringResource(R.string.delete),
            onConfirm = { scope.launch { repo.deleteCategory(c) } }, onDismiss = { deleting = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryDialog(initial: CategoryEntity, onSave: (CategoryEntity) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var group by rememberSaveable { mutableStateOf(initial.groupType) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) stringResource(R.string.add_category) else stringResource(R.string.edit_category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.category_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.category_group), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryGroup.all.forEach { g -> FilterChip(group == g, { group = g }, { Text(groupLabel(g)) }) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(initial.copy(name = name.trim(), groupType = group)); onDismiss() }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
