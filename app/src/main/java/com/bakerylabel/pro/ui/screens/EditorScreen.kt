@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.bakerylabel.pro.data.SavedLabelEntity
import com.bakerylabel.pro.data.TemplateEntity
import com.bakerylabel.pro.model.BgPattern
import com.bakerylabel.pro.model.BorderStyle
import com.bakerylabel.pro.model.ColorRole
import com.bakerylabel.pro.model.ColorThemes
import com.bakerylabel.pro.model.CustomField
import com.bakerylabel.pro.model.ElementStyle
import com.bakerylabel.pro.model.FieldKey
import com.bakerylabel.pro.model.LabelContent
import com.bakerylabel.pro.model.LabelDesign
import com.bakerylabel.pro.model.LabelFont
import com.bakerylabel.pro.model.LabelLayout
import com.bakerylabel.pro.model.LabelSizes
import com.bakerylabel.pro.model.LogoPosition
import com.bakerylabel.pro.model.TextAlignX
import com.bakerylabel.pro.print.BluetoothPrinter
import com.bakerylabel.pro.print.PrintItem
import com.bakerylabel.pro.render.LayoutEngine
import com.bakerylabel.pro.render.Templates
import com.bakerylabel.pro.ui.components.AppScaffold
import com.bakerylabel.pro.ui.components.ColorRow
import com.bakerylabel.pro.ui.components.DateField
import com.bakerylabel.pro.ui.components.LabelPreview
import com.bakerylabel.pro.ui.components.SectionTitle
import com.bakerylabel.pro.ui.components.TextInputDialog
import com.bakerylabel.pro.util.Dates
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun fieldName(k: FieldKey): String = stringResource(
    when (k) {
        FieldKey.LOGO -> R.string.f_logo
        FieldKey.SHOP -> R.string.f_shop
        FieldKey.BAKERY -> R.string.f_bakery
        FieldKey.PRODUCT -> R.string.f_product
        FieldKey.CONTACT -> R.string.f_contact
        FieldKey.LICENSE -> R.string.f_license
        FieldKey.INGREDIENTS -> R.string.ingredients
        FieldKey.STORAGE -> R.string.storage
        FieldKey.BATCH -> R.string.f_batch
        FieldKey.MFG -> R.string.f_mfg
        FieldKey.EXP -> R.string.f_exp
        FieldKey.BEST_BEFORE -> R.string.f_best_before
        FieldKey.WEIGHT -> R.string.net_weight
        FieldKey.PRICE -> R.string.price
        FieldKey.CUSTOM -> R.string.f_custom
        FieldKey.BARCODE -> R.string.barcode
        FieldKey.QR -> R.string.f_qr
    },
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    nav: NavController,
    labelId: Long,
    productId: Long,
    templateId: String,
    initialTab: Int,
    onBack: () -> Unit,
) {
    val app = LocalApp.current
    val repo = app.repo
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val settings by app.settings.flow.collectAsStateWithLifecycle(AppSettings())
    val shop by repo.shop.observe().collectAsStateWithLifecycle(null)
    val products by repo.products.observeAll().collectAsStateWithLifecycle(emptyList())
    val cats by repo.categories.observeAll().collectAsStateWithLifecycle(emptyList())
    val templates = rememberAllTemplates()
    val logo = remember(shop?.logoPath) { repo.loadLogo(shop?.logoPath) }

    var loaded by remember { mutableStateOf(false) }
    var content by remember { mutableStateOf(LabelContent()) }
    var design by remember { mutableStateOf(LabelDesign()) }
    var currentId by remember { mutableLongStateOf(labelId) }
    var name by remember { mutableStateOf("") }
    var linkedProduct by remember { mutableStateOf<Long?>(null) }
    var linkedCategory by remember { mutableStateOf<Long?>(null) }
    var createdAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var tab by remember { mutableIntStateOf(initialTab) }
    var selected by remember { mutableStateOf<FieldKey?>(null) }
    var dirty by remember { mutableStateOf(false) }

    var showSave by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var showSaveTemplate by remember { mutableStateOf(false) }
    var showCustomDuration by remember { mutableStateOf(false) }
    var showCustomField by remember { mutableStateOf(false) }

    // ---------- load ----------
    LaunchedEffect(Unit) {
        if (labelId > 0) {
            repo.labels.get(labelId)?.let { e ->
                content = LabelContent.fromJsonString(e.contentJson)
                design = LabelDesign.fromJsonString(e.designJson)
                name = e.name; linkedProduct = e.productId; linkedCategory = e.categoryId; createdAt = e.createdAt
            }
        } else {
            val p = if (productId > 0) repo.products.get(productId) else null
            val (c, d) = repo.newLabel(p)
            content = c
            design = d
            if (p != null) { linkedProduct = p.id; linkedCategory = p.categoryId; name = p.name }
        }
        if (templateId.isNotEmpty()) {
            val tDesign = if (templateId.startsWith("user_")) {
                repo.templates.getAll().firstOrNull { "user_${it.id}" == templateId }?.let { LabelDesign.fromJsonString(it.designJson) }
            } else {
                Templates.byId(templateId)?.design
            }
            tDesign?.let { design = LayoutEngine.arrange(it, content.visible) }
        }
        loaded = true
    }

    fun relayout() { design = LayoutEngine.arrange(design, content.visible); dirty = true }
    fun setContent(c: LabelContent, relayoutNeeded: Boolean = false) {
        content = c; dirty = true
        if (relayoutNeeded) relayout()
    }
    fun setDesign(d: LabelDesign, relayoutNeeded: Boolean = false) {
        design = d; dirty = true
        if (relayoutNeeded) relayout()
    }
    fun toggle(k: FieldKey, on: Boolean) =
        setContent(content.copy(visible = if (on) content.visible + k else content.visible - k), relayoutNeeded = true)

    fun updateElement(k: FieldKey, f: (ElementStyle) -> ElementStyle) {
        val el = design.elements[k] ?: return
        design = design.copy(layout = LabelLayout.CUSTOM, elements = design.elements + (k to f(el)))
        dirty = true
    }

    fun entity(): SavedLabelEntity = SavedLabelEntity(
        id = currentId,
        name = name.ifBlank { content.productName.ifBlank { "Label" } },
        productId = linkedProduct, categoryId = linkedCategory,
        contentJson = content.toJsonString(), designJson = design.toJsonString(),
        createdAt = createdAt,
    )

    suspend fun save(): Long {
        val id = repo.saveLabel(entity())
        if (currentId == 0L) linkedProduct?.let { repo.products.markUsed(it, System.currentTimeMillis()) }
        currentId = id
        dirty = false
        return id
    }

    // Auto-save edits of a label that has already been saved once.
    LaunchedEffect(content, design, name) {
        if (!loaded || currentId == 0L || !dirty) return@LaunchedEffect
        delay(1200)
        save()
    }

    fun item() = PrintItem(entity().name, content, design)

    fun launchAction(block: suspend () -> String?) {
        scope.launch {
            val msg = runCatching { block() }.getOrElse { it.message ?: it.javaClass.simpleName }
            msg?.let { snack.showSnackbar(it) }
        }
    }

    val btPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.all { it }) launchAction { LabelActions.bluetoothPrint(context, listOf(item()), logo, settings) }
        else scope.launch { snack.showSnackbar(context.getString(R.string.bt_permission_needed)) }
    }
    fun btPrint() {
        val missing = BluetoothPrinter.requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) btPermission.launch(missing.toTypedArray())
        else launchAction { LabelActions.bluetoothPrint(context, listOf(item()), logo, settings) }
    }

    AppScaffold(
        title = if (currentId == 0L) stringResource(R.string.create_label) else name.ifBlank { stringResource(R.string.edit_label) },
        onBack = onBack,
        snackbar = snack,
        actions = {
            IconButton(onClick = { showSave = true }) { Icon(Icons.Filled.Save, stringResource(R.string.save)) }
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    BarAction(Icons.Filled.PictureAsPdf, stringResource(R.string.pdf)) { launchAction { LabelActions.sharePdf(context, entity().name, listOf(item()), logo, settings) } }
                    BarAction(Icons.Filled.Image, stringResource(R.string.png)) { launchAction { LabelActions.savePng(context, item(), logo, settings) } }
                    BarAction(Icons.Filled.Print, stringResource(R.string.print)) { launchAction { LabelActions.systemPrint(context, entity().name, listOf(item()), logo, settings); null } }
                    BarAction(Icons.Filled.Bluetooth, stringResource(R.string.bluetooth)) { btPrint() }
                    BarAction(Icons.Filled.ViewModule, stringResource(R.string.add_to_sheet)) {
                        app.printQueue.add(listOf(item()))
                        nav.navigate(Routes.PRINT)
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // Live preview
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LabelPreview(
                    content, design, logo, settings.dateFormat,
                    maxHeight = 210.dp,
                    editable = tab == 2,
                    selected = selected,
                    onSelect = { selected = it },
                    onMove = { k, dx, dy ->
                        updateElement(k) { e ->
                            e.copy(x = (e.x + dx).coerceIn(0f, 1f - e.w), y = (e.y + dy).coerceIn(0f, 1f - e.h))
                        }
                    },
                    onResize = { k, dx, dy ->
                        updateElement(k) { e ->
                            val nw = (e.w + dx).coerceIn(0.05f, 1f - e.x)
                            val nh = (e.h + dy).coerceIn(0.03f, 1f - e.y)
                            e.copy(w = nw, h = nh, sizePt = (e.sizePt * nh / e.h).coerceIn(2.5f, 72f))
                        }
                    },
                )
                Text(
                    "%.1f × %.1f mm  ·  %.2f × %.2f in".format(Locale.US, design.widthMm, design.heightMm, design.widthMm / 25.4f, design.heightMm / 25.4f) +
                        if (tab == 2) "  ·  " + stringResource(R.string.drag_hint) else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                listOf(R.string.tab_content, R.string.tab_dates, R.string.tab_design, R.string.tab_size_layout).forEachIndexed { i, r ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(r)) })
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                when (tab) {
                    0 -> ContentTab(
                        content = content,
                        onChange = { c, relayout -> setContent(c, relayout) },
                        onToggle = ::toggle,
                        onPickProduct = { showProductPicker = true },
                        onAddCustom = { showCustomField = true },
                        hasLogo = logo != null,
                    )
                    1 -> DatesTab(
                        content = content, dateFormat = settings.dateFormat,
                        onChange = { setContent(it) }, onToggle = ::toggle,
                        onCustomDuration = { showCustomDuration = true },
                    )
                    2 -> DesignTab(
                        design = design, selected = selected,
                        onChange = { d, re -> setDesign(d, re) },
                        onElement = { k, f -> updateElement(k, f) },
                        onHideSelected = { selected?.let { toggle(it, false) }; selected = null },
                        onResetLayout = {
                            setDesign(design.copy(layout = LabelLayout.CLASSIC), relayoutNeeded = true)
                        },
                    )
                    3 -> SizeLayoutTab(
                        design = design,
                        onChange = { d, re -> setDesign(d, re) },
                        onTemplates = { showTemplatePicker = true },
                        onSaveTemplate = { showSaveTemplate = true },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showSave) {
        TextInputDialog(
            title = stringResource(R.string.save_label),
            initial = name.ifBlank { content.productName.ifBlank { "Label" } },
            label = stringResource(R.string.label_name),
            onConfirm = { n ->
                name = n
                scope.launch { save(); snack.showSnackbar(context.getString(R.string.saved)) }
            },
            onDismiss = { showSave = false },
        )
    }
    if (showProductPicker) {
        ProductPickerDialog(products, cats, onPick = { p ->
            linkedProduct = p.id; linkedCategory = p.categoryId
            if (name.isBlank()) name = p.name
            setContent(repo.applyProduct(content, p), relayoutNeeded = true)
        }, onDismiss = { showProductPicker = false })
    }
    if (showTemplatePicker) {
        TemplatePickerDialog(templates, shop?.shopName ?: "", logo, settings.dateFormat, onPick = { t ->
            setDesign(LayoutEngine.arrange(t.design, content.visible))
        }, onDismiss = { showTemplatePicker = false })
    }
    if (showSaveTemplate) {
        TextInputDialog(
            title = stringResource(R.string.save_as_template),
            initial = (name.ifBlank { content.productName }).ifBlank { "My template" },
            label = stringResource(R.string.template_name),
            onConfirm = { n ->
                scope.launch {
                    repo.templates.insert(TemplateEntity(name = n, designJson = design.toJsonString()))
                    snack.showSnackbar(context.getString(R.string.template_saved))
                }
            },
            onDismiss = { showSaveTemplate = false },
        )
    }
    if (showCustomDuration) {
        CustomDurationDialog(onPick = { rule ->
            setContent(content.copy(expRule = rule, expEpochDay = Dates.applyRule(content.mfgEpochDay, rule), visible = content.visible + FieldKey.EXP), relayoutNeeded = FieldKey.EXP !in content.visible)
        }, onDismiss = { showCustomDuration = false })
    }
    if (showCustomField) {
        CustomFieldDialog(onAdd = { f ->
            setContent(content.copy(customFields = content.customFields + f, visible = content.visible + FieldKey.CUSTOM), relayoutNeeded = FieldKey.CUSTOM !in content.visible)
        }, onDismiss = { showCustomField = false })
    }
}

@Composable
private fun BarAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ======================= CONTENT TAB =======================

@Composable
private fun FieldRow(
    key: FieldKey,
    visible: Boolean,
    value: String,
    onValue: (String) -> Unit,
    onToggle: (FieldKey, Boolean) -> Unit,
    multiline: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        OutlinedTextField(
            value, onValue,
            label = { Text(fieldName(key)) },
            singleLine = !multiline, minLines = if (multiline) 2 else 1,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Switch(visible, { onToggle(key, it) })
    }
}

@Composable
private fun ContentTab(
    content: LabelContent,
    onChange: (LabelContent, Boolean) -> Unit,
    onToggle: (FieldKey, Boolean) -> Unit,
    onPickProduct: () -> Unit,
    onAddCustom: () -> Unit,
    hasLogo: Boolean,
) {
    val v = content.visible
    Spacer(Modifier.height(8.dp))
    FilledTonalButton(onClick = onPickProduct, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Inventory2, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.choose_product))
    }
    Text(stringResource(R.string.switch_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp))
    SectionTitle(stringResource(R.string.section_main))
    FieldRow(FieldKey.PRODUCT, FieldKey.PRODUCT in v, content.productName, { onChange(content.copy(productName = it), false) }, onToggle)
    FieldRow(FieldKey.SHOP, FieldKey.SHOP in v, content.shopName, { onChange(content.copy(shopName = it), false) }, onToggle)
    FieldRow(FieldKey.BAKERY, FieldKey.BAKERY in v, content.bakeryName, { onChange(content.copy(bakeryName = it), false) }, onToggle)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(fieldName(FieldKey.LOGO) + if (!hasLogo) " — " + stringResource(R.string.no_logo_set) else "", Modifier.weight(1f))
        Switch(FieldKey.LOGO in v, { onToggle(FieldKey.LOGO, it) })
    }
    SectionTitle(stringResource(R.string.section_product_info))
    FieldRow(FieldKey.WEIGHT, FieldKey.WEIGHT in v, content.netWeight, { onChange(content.copy(netWeight = it), false) }, onToggle)
    FieldRow(FieldKey.PRICE, FieldKey.PRICE in v, content.price, { onChange(content.copy(price = it), false) }, onToggle)
    FieldRow(FieldKey.BATCH, FieldKey.BATCH in v, content.batch, { onChange(content.copy(batch = it), false) }, onToggle)
    FieldRow(FieldKey.INGREDIENTS, FieldKey.INGREDIENTS in v, content.ingredients, { onChange(content.copy(ingredients = it), false) }, onToggle, multiline = true)
    FieldRow(FieldKey.STORAGE, FieldKey.STORAGE in v, content.storage, { onChange(content.copy(storage = it), false) }, onToggle, multiline = true)
    SectionTitle(stringResource(R.string.section_codes))
    FieldRow(FieldKey.BARCODE, FieldKey.BARCODE in v, content.barcode, { onChange(content.copy(barcode = it), false) }, onToggle)
    Text(stringResource(R.string.barcode_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FieldRow(FieldKey.QR, FieldKey.QR in v, content.qrText, { onChange(content.copy(qrText = it), false) }, onToggle)
    SectionTitle(stringResource(R.string.section_contact))
    FieldRow(FieldKey.CONTACT, FieldKey.CONTACT in v, content.contact, { onChange(content.copy(contact = it), false) }, onToggle, multiline = true)
    FieldRow(FieldKey.LICENSE, FieldKey.LICENSE in v, content.license, { onChange(content.copy(license = it), false) }, onToggle)
    SectionTitle(stringResource(R.string.f_custom), trailing = {
        Switch(FieldKey.CUSTOM in v, { onToggle(FieldKey.CUSTOM, it) })
    })
    content.customFields.forEachIndexed { i, f ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            OutlinedTextField(f.label, { t -> onChange(content.copy(customFields = content.customFields.toMutableList().also { it[i] = f.copy(label = t) }), false) },
                label = { Text(stringResource(R.string.field_title)) }, singleLine = true, modifier = Modifier.weight(0.8f))
            Spacer(Modifier.width(6.dp))
            OutlinedTextField(f.value, { t -> onChange(content.copy(customFields = content.customFields.toMutableList().also { it[i] = f.copy(value = t) }), false) },
                label = { Text(stringResource(R.string.field_value)) }, singleLine = true, modifier = Modifier.weight(1.2f))
            IconButton(onClick = { onChange(content.copy(customFields = content.customFields.filterIndexed { j, _ -> j != i }), false) }) {
                Icon(Icons.Filled.Close, stringResource(R.string.delete))
            }
        }
    }
    OutlinedButton(onClick = onAddCustom) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.add_custom_field)) }
}

@Composable
private fun CustomFieldDialog(onAdd: (CustomField) -> Unit, onDismiss: () -> Unit) {
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_custom_field)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(label, { label = it }, label = { Text(stringResource(R.string.field_title)) }, placeholder = { Text("Halal") }, singleLine = true)
                OutlinedTextField(value, { value = it }, label = { Text(stringResource(R.string.field_value)) }, placeholder = { Text("Certified") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(enabled = label.isNotBlank() || value.isNotBlank(), onClick = { onAdd(CustomField(label.trim(), value.trim())); onDismiss() }) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ======================= DATES TAB =======================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DatesTab(
    content: LabelContent,
    dateFormat: String,
    onChange: (LabelContent) -> Unit,
    onToggle: (FieldKey, Boolean) -> Unit,
    onCustomDuration: () -> Unit,
) {
    SectionTitle(stringResource(R.string.mfg_date), trailing = { Switch(FieldKey.MFG in content.visible, { onToggle(FieldKey.MFG, it) }) })
    Row(verticalAlignment = Alignment.CenterVertically) {
        DateField(stringResource(R.string.f_mfg), content.mfgEpochDay, dateFormat, onPicked = { d ->
            // If expiry came from a duration, keep that duration; a manually chosen expiry stays as it is.
            val exp = content.expRule?.let { Dates.applyRule(d, it) } ?: content.expEpochDay
            onChange(content.copy(mfgEpochDay = d, expEpochDay = exp))
        }, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = {
            val d = Dates.today()
            val exp = content.expRule?.let { Dates.applyRule(d, it) } ?: content.expEpochDay
            onChange(content.copy(mfgEpochDay = d, expEpochDay = exp))
        }) { Icon(Icons.Filled.Today, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.today)) }
    }

    SectionTitle(stringResource(R.string.exp_date), trailing = { Switch(FieldKey.EXP in content.visible, { onToggle(FieldKey.EXP, it) }) })
    Row(verticalAlignment = Alignment.CenterVertically) {
        DateField(stringResource(R.string.f_exp), content.expEpochDay, dateFormat, onPicked = { d ->
            onChange(content.copy(expEpochDay = d, expRule = null))
        }, modifier = Modifier.weight(1f), placeholder = stringResource(R.string.not_set))
        if (content.expEpochDay != null) {
            IconButton(onClick = { onChange(content.copy(expEpochDay = null, expRule = null)) }) {
                Icon(Icons.Filled.Close, stringResource(R.string.clear))
            }
        }
    }
    Text(
        content.expRule?.let { stringResource(R.string.exp_from_rule, Dates.ruleLabel(it)) } ?: stringResource(R.string.exp_manual_note),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp),
    )
    Text(stringResource(R.string.quick_expiry), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Dates.quickRules.forEach { r ->
            FilterChip(
                selected = content.expRule == r,
                onClick = { onChange(content.copy(expRule = r, expEpochDay = Dates.applyRule(content.mfgEpochDay, r))) },
                label = { Text(Dates.ruleLabel(r)) },
            )
        }
        AssistChip(onClick = onCustomDuration, label = { Text(stringResource(R.string.custom_duration)) }, leadingIcon = { Icon(Icons.Filled.Add, null) })
    }
    SectionTitle(stringResource(R.string.f_best_before), trailing = { Switch(FieldKey.BEST_BEFORE in content.visible, { onToggle(FieldKey.BEST_BEFORE, it) }) })
    OutlinedTextField(
        content.bestBefore, { onChange(content.copy(bestBefore = it)) },
        label = { Text(stringResource(R.string.best_before_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        content.expEpochDay?.let { exp ->
            AssistChip(onClick = { onChange(content.copy(bestBefore = Dates.format(exp, dateFormat))) }, label = { Text(stringResource(R.string.use_exp_date)) })
        }
    }
}

@Composable
private fun CustomDurationDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var n by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("D") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_duration)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(n, { v -> n = v.filter { it.isDigit() }.take(4) }, label = { Text(stringResource(R.string.number)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                val units = listOf("D" to R.string.days, "W" to R.string.weeks, "M" to R.string.months, "Y" to R.string.years)
                SingleChoiceSegmentedButtonRow {
                    units.forEachIndexed { i, (u, r) ->
                        SegmentedButton(unit == u, { unit = u }, SegmentedButtonDefaults.itemShape(i, units.size)) { Text(stringResource(r)) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = (n.toIntOrNull() ?: 0) > 0, onClick = { onPick("$unit:${n.toInt()}"); onDismiss() }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ======================= DESIGN TAB =======================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesignTab(
    design: LabelDesign,
    selected: FieldKey?,
    onChange: (LabelDesign, Boolean) -> Unit,
    onElement: (FieldKey, (ElementStyle) -> ElementStyle) -> Unit,
    onHideSelected: () -> Unit,
    onResetLayout: () -> Unit,
) {
    // Selected element panel
    val el = selected?.let { design.elements[it] }
    if (selected != null && el != null) {
        Column(
            Modifier.fillMaxWidth().padding(top = 10.dp).clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.secondaryContainer).padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.selected_element, fieldName(selected)), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onHideSelected) { Text(stringResource(R.string.hide)) }
            }
            if (selected.isText) {
                Text(stringResource(R.string.font_size_value, el.sizePt), style = MaterialTheme.typography.labelMedium)
                Slider(el.sizePt, { s -> onElement(selected) { it.copy(sizePt = s) } }, valueRange = 3f..60f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.bold), Modifier.weight(1f))
                    Switch(el.bold, { b -> onElement(selected) { it.copy(bold = b) } })
                }
                AlignSelector(el.align) { a -> onElement(selected) { it.copy(align = a) } }
                Text(stringResource(R.string.text_colour), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ColorRole.TEXT to R.string.c_text, ColorRole.ACCENT to R.string.c_accent, ColorRole.ON_ACCENT to R.string.c_background).forEach { (r, s) ->
                        FilterChip(el.role == r, { onElement(selected) { it.copy(role = r) } }, { Text(stringResource(s)) })
                    }
                }
            }
            Text(stringResource(R.string.element_resize_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Text(stringResource(R.string.tap_element_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
    }

    SectionTitle(stringResource(R.string.colour_themes))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorThemes.all.forEach { t ->
            FilterChip(
                selected = design.themeName == t.name,
                onClick = { onChange(design.copy(bgColor = t.bg, textColor = t.text, accentColor = t.accent, borderColor = t.border, themeName = t.name), false) },
                label = { Text(t.name) },
                leadingIcon = {
                    Row {
                        androidx.compose.foundation.layout.Box(Modifier.size(12.dp).clip(CircleShape).background(Color(t.accent)))
                        androidx.compose.foundation.layout.Box(Modifier.size(12.dp).clip(CircleShape).background(Color(t.border)))
                    }
                },
            )
        }
        FilterChip(design.themeName == "Custom", { onChange(design.copy(themeName = "Custom"), false) }, { Text(stringResource(R.string.custom_colours)) })
    }
    ColorRow(stringResource(R.string.c_background), design.bgColor) { onChange(design.copy(bgColor = it, themeName = "Custom"), false) }
    ColorRow(stringResource(R.string.c_text), design.textColor) { onChange(design.copy(textColor = it, themeName = "Custom"), false) }
    ColorRow(stringResource(R.string.c_accent), design.accentColor) { onChange(design.copy(accentColor = it, themeName = "Custom"), false) }
    ColorRow(stringResource(R.string.c_border), design.borderColor) { onChange(design.copy(borderColor = it, themeName = "Custom"), false) }

    SectionTitle(stringResource(R.string.font))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LabelFont.entries.forEach { f -> FilterChip(design.font == f, { onChange(design.copy(font = f), false) }, { Text(f.displayName) }) }
    }
    Text(stringResource(R.string.font_scale_value, (design.fontScale * 100).toInt()), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
    Slider(design.fontScale, { s ->
        // Scale every element proportionally so custom layouts keep their look.
        val ratio = s / design.fontScale
        onChange(design.copy(fontScale = s, elements = design.elements.mapValues { (_, e) -> e.copy(sizePt = (e.sizePt * ratio).coerceIn(2.5f, 72f)) }), false)
    }, valueRange = 0.5f..2f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.bold_all), Modifier.weight(1f))
        Switch(design.boldText, { b ->
            onChange(design.copy(boldText = b, elements = design.elements.mapValues { (k, e) -> e.copy(bold = b || k == FieldKey.PRODUCT || k == FieldKey.SHOP) }), false)
        })
    }
    Text(stringResource(R.string.alignment), style = MaterialTheme.typography.labelMedium)
    AlignSelector(design.align) { a ->
        onChange(design.copy(align = a, elements = design.elements.mapValues { (_, e) -> e.copy(align = a) }), false)
    }

    SectionTitle(stringResource(R.string.border))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BorderStyle.entries.forEach { b ->
            FilterChip(design.borderStyle == b, { onChange(design.copy(borderStyle = b), false) }, { Text(b.name.lowercase().replaceFirstChar { it.uppercase() }) })
        }
    }
    Text(stringResource(R.string.border_thickness_value, design.borderWidthMm), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
    Slider(design.borderWidthMm, { onChange(design.copy(borderWidthMm = it), false) }, valueRange = 0f..3f)
    Text(stringResource(R.string.corner_radius_value, design.cornerRadiusMm), style = MaterialTheme.typography.labelMedium)
    Slider(design.cornerRadiusMm, { onChange(design.copy(cornerRadiusMm = it), false) }, valueRange = 0f..15f)

    SectionTitle(stringResource(R.string.background_pattern))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BgPattern.entries.forEach { p ->
            FilterChip(design.pattern == p, { onChange(design.copy(pattern = p), false) }, { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) })
        }
    }

    SectionTitle(stringResource(R.string.f_logo))
    Text(stringResource(R.string.logo_size_value, (design.logoSize * 100).toInt()), style = MaterialTheme.typography.labelMedium)
    Slider(design.logoSize, { onChange(design.copy(logoSize = it), true) }, valueRange = 0.1f..0.6f)
    Text(stringResource(R.string.logo_position), style = MaterialTheme.typography.labelMedium)
    val positions = LogoPosition.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        positions.forEachIndexed { i, p ->
            SegmentedButton(design.logoPosition == p, { onChange(design.copy(logoPosition = p), true) }, SegmentedButtonDefaults.itemShape(i, positions.size)) {
                Text(stringResource(when (p) { LogoPosition.TOP_LEFT -> R.string.left; LogoPosition.TOP_CENTER -> R.string.center; LogoPosition.TOP_RIGHT -> R.string.right }))
            }
        }
    }
    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    OutlinedButton(onClick = onResetLayout, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Dashboard, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.reset_layout))
    }
}

@Composable
private fun AlignSelector(value: TextAlignX, onPick: (TextAlignX) -> Unit) {
    val all = TextAlignX.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        all.forEachIndexed { i, a ->
            SegmentedButton(value == a, { onPick(a) }, SegmentedButtonDefaults.itemShape(i, all.size)) {
                Text(stringResource(when (a) { TextAlignX.START -> R.string.left; TextAlignX.CENTER -> R.string.center; TextAlignX.END -> R.string.right }))
            }
        }
    }
}

// ======================= SIZE & LAYOUT TAB =======================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SizeLayoutTab(
    design: LabelDesign,
    onChange: (LabelDesign, Boolean) -> Unit,
    onTemplates: () -> Unit,
    onSaveTemplate: () -> Unit,
) {
    var inches by remember { mutableStateOf(false) }
    SectionTitle(stringResource(R.string.label_size))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LabelSizes.presets.forEach { s ->
            val sel = kotlin.math.abs(design.widthMm - s.widthMm) < 0.2f && kotlin.math.abs(design.heightMm - s.heightMm) < 0.2f
            FilterChip(sel, { onChange(design.copy(widthMm = s.widthMm, heightMm = s.heightMm), true) }, { Text(s.label) })
        }
    }
    Text(stringResource(R.string.custom_size), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(!inches, { inches = false }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("mm") }
        SegmentedButton(inches, { inches = true }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("inch") }
    }
    val factor = if (inches) 25.4f else 1f
    // Re-key on unit and size so the fields refresh when a preset is chosen.
    androidx.compose.runtime.key(inches, design.widthMm, design.heightMm) {
        var w by remember { mutableStateOf(fmt(design.widthMm / factor)) }
        var h by remember { mutableStateOf(fmt(design.heightMm / factor)) }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(w, { w = it }, label = { Text(stringResource(R.string.width)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(h, { h = it }, label = { Text(stringResource(R.string.height)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = {
                val nw = w.replace(',', '.').toFloatOrNull()?.times(factor)
                val nh = h.replace(',', '.').toFloatOrNull()?.times(factor)
                if (nw != null && nh != null && nw in 10f..300f && nh in 10f..300f) onChange(design.copy(widthMm = nw, heightMm = nh), true)
            }) { Text(stringResource(R.string.apply)) }
        }
    }
    Text(stringResource(R.string.size_range_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    SectionTitle(stringResource(R.string.layout))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            LabelLayout.CLASSIC to R.string.layout_classic,
            LabelLayout.BANNER to R.string.layout_banner,
            LabelLayout.LOGO_LEFT to R.string.layout_logo_left,
            LabelLayout.COMPACT to R.string.layout_compact,
            LabelLayout.CUSTOM to R.string.layout_custom,
        ).forEach { (l, r) ->
            FilterChip(design.layout == l, { onChange(design.copy(layout = l), l != LabelLayout.CUSTOM) }, { Text(stringResource(r)) })
        }
    }
    SectionTitle(stringResource(R.string.templates))
    FilledTonalButton(onClick = onTemplates, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.choose_template)) }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onSaveTemplate, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.BookmarkAdd, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.save_as_template))
    }
    Spacer(Modifier.height(8.dp).navigationBarsPadding())
}

private fun fmt(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
