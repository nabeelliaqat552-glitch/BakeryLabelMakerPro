@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bakerylabel.pro.LocalApp
import com.bakerylabel.pro.R
import com.bakerylabel.pro.Routes
import com.bakerylabel.pro.data.AppSettings
import com.bakerylabel.pro.data.SavedLabelEntity
import com.bakerylabel.pro.model.LabelContent
import com.bakerylabel.pro.model.LabelDesign
import com.bakerylabel.pro.print.PrintItem
import com.bakerylabel.pro.ui.components.AppScaffold
import com.bakerylabel.pro.ui.components.ConfirmDialog
import com.bakerylabel.pro.ui.components.EmptyState
import com.bakerylabel.pro.ui.components.LabelPreview
import com.bakerylabel.pro.ui.components.PressCard
import com.bakerylabel.pro.ui.components.TextInputDialog
import com.bakerylabel.pro.util.Dates
import kotlinx.coroutines.launch

fun SavedLabelEntity.toPrintItem() =
    PrintItem(name, LabelContent.fromJsonString(contentJson), LabelDesign.fromJsonString(designJson))

@Composable
fun SavedLabelsScreen(nav: NavController, selectMode: Boolean, onBack: () -> Unit) {
    val app = LocalApp.current
    val repo = app.repo
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val labels by repo.labels.observeAll().collectAsStateWithLifecycle(emptyList())
    val cats by repo.categories.observeAll().collectAsStateWithLifecycle(emptyList())
    val settings by app.settings.flow.collectAsStateWithLifecycle(AppSettings())
    val shop by repo.shop.observe().collectAsStateWithLifecycle(null)
    val logo = remember(shop?.logoPath) { repo.loadLogo(shop?.logoPath) }

    var query by rememberSaveable { mutableStateOf("") }
    var catFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var selecting by rememberSaveable { mutableStateOf(selectMode) }
    var picked by remember { mutableStateOf(setOf<Long>()) }
    var renaming by remember { mutableStateOf<SavedLabelEntity?>(null) }
    var deleting by remember { mutableStateOf<SavedLabelEntity?>(null) }

    val list = labels.filter { l ->
        (catFilter == null || l.categoryId == catFilter) &&
            (query.isBlank() || l.name.contains(query.trim(), true) || l.contentJson.contains(query.trim(), true))
    }
    val usedCats = cats.filter { c -> labels.any { it.categoryId == c.id } }

    AppScaffold(
        title = stringResource(if (selecting) R.string.select_labels else R.string.saved_labels),
        onBack = onBack,
        snackbar = snack,
        actions = {
            IconButton(onClick = { selecting = !selecting; picked = emptySet() }) { Icon(Icons.Filled.Checklist, stringResource(R.string.select)) }
        },
        floating = {
            if (selecting && picked.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            val items = repo.labels.getMany(picked.toList()).map { it.toPrintItem() }
                            app.printQueue.add(items)
                            if (selectMode) onBack() else nav.navigate(Routes.PRINT)
                        }
                    },
                    icon = { Icon(Icons.Filled.ViewModule, null) },
                    text = { Text(stringResource(R.string.add_n_to_sheet, picked.size)) },
                    containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            OutlinedTextField(
                query, { query = it }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text(stringResource(R.string.search_labels)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (usedCats.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(catFilter == null, { catFilter = null }, { Text(stringResource(R.string.all)) })
                    usedCats.forEach { c -> FilterChip(catFilter == c.id, { catFilter = if (catFilter == c.id) null else c.id }, { Text(c.name) }) }
                }
            }
            if (list.isEmpty()) {
                EmptyState(Icons.Filled.CollectionsBookmark, stringResource(R.string.no_saved_labels))
            } else {
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(list, key = { it.id }) { l ->
                        val c = remember(l.contentJson) { LabelContent.fromJsonString(l.contentJson) }
                        val d = remember(l.designJson) { LabelDesign.fromJsonString(l.designJson) }
                        var menu by remember { mutableStateOf(false) }
                        PressCard(onClick = {
                            if (selecting) picked = if (l.id in picked) picked - l.id else picked + l.id
                            else nav.navigate(Routes.editor(labelId = l.id))
                        }) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (selecting) Checkbox(l.id in picked, { picked = if (it) picked + l.id else picked - l.id })
                                Box(Modifier.width(120.dp)) { LabelPreview(c, d, logo, settings.dateFormat, maxHeight = 70.dp) }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(l.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("MFG " + Dates.format(c.mfgEpochDay, settings.dateFormat) +
                                        (c.expEpochDay?.let { " · EXP " + Dates.format(it, settings.dateFormat) } ?: ""),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("%.0f×%.0f mm · %s".format(d.widthMm, d.heightMm, d.themeName),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!selecting) Box {
                                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null) }
                                    DropdownMenu(menu, { menu = false }) {
                                        DropdownMenuItem({ Text(stringResource(R.string.edit)) }, { menu = false; nav.navigate(Routes.editor(labelId = l.id)) })
                                        DropdownMenuItem({ Text(stringResource(R.string.rename)) }, { menu = false; renaming = l })
                                        DropdownMenuItem({ Text(stringResource(R.string.duplicate)) }, {
                                            menu = false
                                            scope.launch { repo.duplicateLabel(l.id); snack.showSnackbar(context.getString(R.string.duplicated)) }
                                        })
                                        DropdownMenuItem({ Text(stringResource(R.string.new_design_same_product)) }, {
                                            menu = false
                                            // Copy the label and open the copy in the designer, so the product gets a second design.
                                            scope.launch {
                                                repo.duplicateLabel(l.id)?.let { id -> nav.navigate(Routes.editor(labelId = id, tab = 2)) }
                                            }
                                        })
                                        DropdownMenuItem({ Text(stringResource(R.string.export_pdf)) }, {
                                            menu = false
                                            scope.launch {
                                                val msg = runCatching { LabelActions.sharePdf(context, l.name, listOf(l.toPrintItem()), logo, settings) }
                                                    .getOrElse { it.message ?: "Error" }
                                                snack.showSnackbar(msg)
                                            }
                                        })
                                        DropdownMenuItem({ Text(stringResource(R.string.add_to_sheet)) }, {
                                            menu = false
                                            app.printQueue.add(listOf(l.toPrintItem()))
                                            scope.launch { snack.showSnackbar(context.getString(R.string.added_to_sheet)) }
                                        })
                                        DropdownMenuItem({ Text(stringResource(R.string.delete)) }, { menu = false; deleting = l })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renaming?.let { l ->
        TextInputDialog(stringResource(R.string.rename), l.name, stringResource(R.string.label_name),
            onConfirm = { n -> scope.launch { repo.labels.rename(l.id, n, System.currentTimeMillis()) } },
            onDismiss = { renaming = null })
    }
    deleting?.let { l ->
        ConfirmDialog(stringResource(R.string.delete_label), stringResource(R.string.delete_confirm, l.name), stringResource(R.string.delete),
            onConfirm = { scope.launch { repo.labels.delete(l.id) } }, onDismiss = { deleting = null })
    }
}
