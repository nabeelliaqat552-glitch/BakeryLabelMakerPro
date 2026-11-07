@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bakerylabel.pro.LocalApp
import com.bakerylabel.pro.R
import com.bakerylabel.pro.Routes
import com.bakerylabel.pro.data.AppSettings
import com.bakerylabel.pro.data.ProductEntity
import com.bakerylabel.pro.data.SavedLabelEntity
import com.bakerylabel.pro.model.FieldKey
import com.bakerylabel.pro.model.LabelContent
import com.bakerylabel.pro.model.LabelDesign
import com.bakerylabel.pro.print.BluetoothPrinter
import com.bakerylabel.pro.print.PrintItem
import com.bakerylabel.pro.render.LabelTemplate
import com.bakerylabel.pro.render.LayoutEngine
import com.bakerylabel.pro.ui.components.AppScaffold
import com.bakerylabel.pro.ui.components.DateField
import com.bakerylabel.pro.ui.components.LabelPreview
import com.bakerylabel.pro.ui.components.SectionTitle
import com.bakerylabel.pro.util.Dates
import kotlinx.coroutines.launch

/** One product in a bulk batch. A null template means "use the shared design". */
private data class BulkEntry(
    val productId: Long,
    val categoryId: Long?,
    val content: LabelContent,
    val template: LabelTemplate? = null,
    val copies: Int = 1,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BulkScreen(nav: NavController, onBack: () -> Unit) {
    val app = LocalApp.current
    val repo = app.repo
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val settings by app.settings.flow.collectAsStateWithLifecycle(AppSettings())
    val products by repo.products.observeAll().collectAsStateWithLifecycle(emptyList())
    val cats by repo.categories.observeAll().collectAsStateWithLifecycle(emptyList())
    val shop by repo.shop.observe().collectAsStateWithLifecycle(null)
    val logo = remember(shop?.logoPath) { repo.loadLogo(shop?.logoPath) }
    val templates = rememberAllTemplates()

    var step by remember { mutableIntStateOf(0) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var query by remember { mutableStateOf("") }
    var catFilter by remember { mutableStateOf<Long?>(null) }

    val entries = remember { mutableStateListOf<BulkEntry>() }
    var sameDates by remember { mutableStateOf(true) }
    var sameDesign by remember { mutableStateOf(true) }
    var mfg by remember { mutableLongStateOf(Dates.today()) }
    var exp by remember { mutableStateOf<Long?>(null) }
    var expRule by remember { mutableStateOf<String?>(null) }
    var sharedDesign by remember { mutableStateOf<LabelDesign?>(null) }
    var sharedTemplateName by remember { mutableStateOf("") }
    var templateFor by remember { mutableStateOf<Int?>(null) } // -1 = shared design, else entry index
    var editIndex by remember { mutableStateOf<Int?>(null) }

    suspend fun buildEntries() {
        val chosen = products.filter { it.id in selectedIds }
        val (_, base) = repo.newLabel()
        if (sharedDesign == null) { sharedDesign = base; sharedTemplateName = base.themeName }
        val existing = entries.associateBy { it.productId }
        val next = chosen.map { p ->
            existing[p.id] ?: run {
                val (c, _) = repo.newLabel(p)
                BulkEntry(p.id, p.categoryId, c)
            }
        }
        entries.clear(); entries.addAll(next)
    }

    fun datesFor(e: BulkEntry): LabelContent =
        if (sameDates) e.content.copy(mfgEpochDay = mfg, expEpochDay = exp, expRule = expRule) else e.content

    fun designFor(e: BulkEntry, c: LabelContent): LabelDesign {
        val d = if (!sameDesign && e.template != null) e.template.design else (sharedDesign ?: LabelDesign())
        return LayoutEngine.arrange(d, c.visible)
    }

    fun buildItems(): List<PrintItem> = entries.map { e ->
        val c = datesFor(e)
        PrintItem(c.productName, c, designFor(e, c), e.copies)
    }

    val btPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        scope.launch {
            snack.showSnackbar(
                if (res.values.all { it }) LabelActions.bluetoothPrint(context, buildItems(), logo, settings)
                else context.getString(R.string.bt_permission_needed),
            )
        }
    }

    AppScaffold(
        title = stringResource(R.string.bulk_maker),
        onBack = { if (step > 0) step-- else onBack() },
        snackbar = snack,
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // Step indicator
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.step_select, R.string.step_details, R.string.step_generate).forEachIndexed { i, r ->
                    FilterChip(step == i, { if (i == 0 || entries.isNotEmpty()) step = i }, { Text("${i + 1}. " + stringResource(r)) })
                }
            }
            when (step) {
                // ---------------- Step 1: choose products ----------------
                0 -> {
                    val visible = products.filter { p ->
                        (catFilter == null || p.categoryId == catFilter) && (query.isBlank() || p.name.contains(query.trim(), true))
                    }
                    OutlinedTextField(query, { query = it }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                        placeholder = { Text(stringResource(R.string.search_products)) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(catFilter == null, { catFilter = null }, { Text(stringResource(R.string.all)) })
                        cats.forEach { c -> FilterChip(catFilter == c.id, { catFilter = if (catFilter == c.id) null else c.id }, { Text(c.name) }) }
                    }
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.n_selected, selectedIds.size), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { selectedIds = selectedIds + visible.map { it.id } }) { Text(stringResource(R.string.select_all)) }
                        TextButton(onClick = { selectedIds = emptySet() }) { Text(stringResource(R.string.clear)) }
                    }
                    LazyColumn(Modifier.weight(1f)) {
                        items(visible, key = { it.id }) { p ->
                            val on = p.id in selectedIds
                            ListItem(
                                leadingContent = { Checkbox(on, { selectedIds = if (it) selectedIds + p.id else selectedIds - p.id }) },
                                headlineContent = { Text(p.name) },
                                supportingContent = { cats.firstOrNull { it.id == p.categoryId }?.let { Text(it.name) } },
                                modifier = Modifier.clickable { selectedIds = if (on) selectedIds - p.id else selectedIds + p.id },
                            )
                        }
                    }
                    Button(
                        enabled = selectedIds.isNotEmpty(),
                        onClick = { scope.launch { buildEntries(); step = 1 } },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) { Text(stringResource(R.string.next_details)); Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.ArrowForward, null) }
                }

                // ---------------- Step 2: dates, design, per-item details ----------------
                1 -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    item {
                        SectionTitle(stringResource(R.string.dates))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            SegmentedButton(sameDates, { sameDates = true }, SegmentedButtonDefaults.itemShape(0, 2)) { Text(stringResource(R.string.same_dates)) }
                            SegmentedButton(!sameDates, { sameDates = false }, SegmentedButtonDefaults.itemShape(1, 2)) { Text(stringResource(R.string.different_dates)) }
                        }
                        if (sameDates) {
                            Row(Modifier.padding(top = 10.dp)) {
                                DateField(stringResource(R.string.f_mfg), mfg, settings.dateFormat, { d ->
                                    mfg = d; expRule?.let { exp = Dates.applyRule(d, it) }
                                }, Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                DateField(stringResource(R.string.f_exp), exp, settings.dateFormat, { d -> exp = d; expRule = null },
                                    Modifier.weight(1f), placeholder = stringResource(R.string.not_set))
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                                Dates.quickRules.forEach { r ->
                                    FilterChip(expRule == r, { expRule = r; exp = Dates.applyRule(mfg, r) }, { Text(Dates.ruleLabel(r)) })
                                }
                                FilterChip(exp == null, { exp = null; expRule = null }, { Text(stringResource(R.string.no_expiry)) })
                            }
                        } else {
                            Text(stringResource(R.string.per_item_dates_hint), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                        }

                        SectionTitle(stringResource(R.string.design))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            SegmentedButton(sameDesign, { sameDesign = true }, SegmentedButtonDefaults.itemShape(0, 2)) { Text(stringResource(R.string.same_design)) }
                            SegmentedButton(!sameDesign, { sameDesign = false }, SegmentedButtonDefaults.itemShape(1, 2)) { Text(stringResource(R.string.different_templates)) }
                        }
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (if (sameDesign) stringResource(R.string.design_for_all) else stringResource(R.string.default_design)) + ": " + sharedTemplateName,
                                Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                            )
                            FilledTonalButton(onClick = { templateFor = -1 }) { Text(stringResource(R.string.change)) }
                        }
                        SectionTitle(stringResource(R.string.products_n, entries.size))
                    }
                    itemsIndexed(entries, key = { _, e -> e.productId }) { i, e ->
                        ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(e.content.productName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { editIndex = i }) { Icon(Icons.Filled.Edit, stringResource(R.string.edit)) }
                                }
                                if (!sameDates) {
                                    Row {
                                        DateField(stringResource(R.string.f_mfg), e.content.mfgEpochDay, settings.dateFormat, { d ->
                                            val ne = e.content.expRule?.let { Dates.applyRule(d, it) } ?: e.content.expEpochDay
                                            entries[i] = e.copy(content = e.content.copy(mfgEpochDay = d, expEpochDay = ne))
                                        }, Modifier.weight(1f))
                                        Spacer(Modifier.width(8.dp))
                                        DateField(stringResource(R.string.f_exp), e.content.expEpochDay, settings.dateFormat, { d ->
                                            entries[i] = e.copy(content = e.content.copy(expEpochDay = d, expRule = null))
                                        }, Modifier.weight(1f), placeholder = stringResource(R.string.not_set))
                                    }
                                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Dates.quickRules.forEach { r ->
                                            FilterChip(e.content.expRule == r, {
                                                entries[i] = e.copy(content = e.content.copy(expRule = r, expEpochDay = Dates.applyRule(e.content.mfgEpochDay, r)))
                                            }, { Text(Dates.ruleLabel(r)) })
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                                    Text(stringResource(R.string.copies), Modifier.weight(1f))
                                    IconButton(onClick = { entries[i] = e.copy(copies = (e.copies - 1).coerceAtLeast(1)) }) { Icon(Icons.Filled.Remove, null) }
                                    Text(e.copies.toString(), style = MaterialTheme.typography.titleMedium)
                                    IconButton(onClick = { entries[i] = e.copy(copies = (e.copies + 1).coerceAtMost(500)) }) { Icon(Icons.Filled.Add, null) }
                                }
                                if (!sameDesign) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(R.string.template) + ": " + (e.template?.name ?: sharedTemplateName), Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall)
                                        TextButton(onClick = { templateFor = i }) { Text(stringResource(R.string.change)) }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Button(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            Text(stringResource(R.string.generate_n, entries.sumOf { it.copies }))
                        }
                    }
                }

                // ---------------- Step 3: preview + output ----------------
                else -> {
                    val all = buildItems()
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Text(stringResource(R.string.generated_summary, all.size, all.sumOf { it.copies }), style = MaterialTheme.typography.titleMedium)
                        }
                        items(all, key = { it.key }) { it2 ->
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp)) {
                                    LabelPreview(it2.content, it2.design, logo, settings.dateFormat, maxHeight = 110.dp)
                                    Text("${it2.name} × ${it2.copies}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { app.printQueue.set(all); nav.navigate(Routes.PRINT) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.ViewModule, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.a4_sheet))
                            }
                            FilledTonalButton(onClick = {
                                scope.launch {
                                    snack.showSnackbar(runCatching { LabelActions.sharePdf(context, "bulk_labels", all, logo, settings) }.getOrElse { it.message ?: "Error" })
                                }
                            }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.PictureAsPdf, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.bulk_pdf))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val missing = BluetoothPrinter.requiredPermissions.filter {
                                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                                }
                                if (missing.isNotEmpty()) btPermission.launch(missing.toTypedArray())
                                else scope.launch { snack.showSnackbar(LabelActions.bluetoothPrint(context, all, logo, settings)) }
                            }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Bluetooth, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.bluetooth))
                            }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    entries.forEachIndexed { idx, e ->
                                        val it2 = all[idx]
                                        repo.saveLabel(SavedLabelEntity(
                                            name = it2.name, productId = e.productId, categoryId = e.categoryId,
                                            contentJson = it2.content.toJsonString(), designJson = it2.design.toJsonString(),
                                        ))
                                        repo.products.markUsed(e.productId, System.currentTimeMillis())
                                    }
                                    snack.showSnackbar(context.getString(R.string.saved_n_labels, entries.size))
                                }
                            }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Save, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.save_all))
                            }
                        }
                    }
                }
            }
        }
    }

    templateFor?.let { target ->
        TemplatePickerDialog(templates, shop?.shopName ?: "", logo, settings.dateFormat, onPick = { t ->
            if (target == -1) { sharedDesign = t.design; sharedTemplateName = t.name }
            else entries.getOrNull(target)?.let { entries[target] = it.copy(template = t) }
        }, onDismiss = { templateFor = null })
    }
    editIndex?.let { idx ->
        entries.getOrNull(idx)?.let { e ->
            BulkItemDialog(e.content, onSave = { entries[idx] = e.copy(content = it) }, onDismiss = { editIndex = null })
        }
    }
}

@Composable
private fun BulkItemDialog(initial: LabelContent, onSave: (LabelContent) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial.productName) }
    var weight by remember { mutableStateOf(initial.netWeight) }
    var price by remember { mutableStateOf(initial.price) }
    var batch by remember { mutableStateOf(initial.batch) }
    var barcode by remember { mutableStateOf(initial.barcode) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.product_name)) }, singleLine = true)
                OutlinedTextField(weight, { weight = it }, label = { Text(stringResource(R.string.net_weight)) }, singleLine = true)
                OutlinedTextField(price, { price = it }, label = { Text(stringResource(R.string.price)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                OutlinedTextField(batch, { batch = it }, label = { Text(stringResource(R.string.f_batch)) }, singleLine = true)
                OutlinedTextField(barcode, { barcode = it }, label = { Text(stringResource(R.string.barcode)) }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                var v = initial.visible
                if (weight.isNotBlank()) v = v + FieldKey.WEIGHT
                if (price.isNotBlank()) v = v + FieldKey.PRICE
                if (batch.isNotBlank()) v = v + FieldKey.BATCH
                v = if (barcode.isNotBlank()) v + FieldKey.BARCODE else v - FieldKey.BARCODE
                onSave(initial.copy(productName = name, netWeight = weight, price = price, batch = batch, barcode = barcode, visible = v))
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
