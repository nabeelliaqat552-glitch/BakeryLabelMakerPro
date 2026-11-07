@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bakerylabel.pro.LocalApp
import com.bakerylabel.pro.R
import com.bakerylabel.pro.Routes
import com.bakerylabel.pro.data.AppSettings
import com.bakerylabel.pro.print.BluetoothPrinter
import com.bakerylabel.pro.print.Exporter
import com.bakerylabel.pro.print.SheetConfig
import com.bakerylabel.pro.print.SheetLayout
import com.bakerylabel.pro.ui.components.AppScaffold
import com.bakerylabel.pro.ui.components.EmptyState
import com.bakerylabel.pro.ui.components.LabelPreview
import com.bakerylabel.pro.ui.components.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PrintScreen(nav: NavController, onBack: () -> Unit) {
    val app = LocalApp.current
    val repo = app.repo
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val settings by app.settings.flow.collectAsStateWithLifecycle(AppSettings())
    val queue by app.printQueue.items.collectAsStateWithLifecycle()
    val shop by repo.shop.observe().collectAsStateWithLifecycle(null)
    val logo = remember(shop?.logoPath) { repo.loadLogo(shop?.logoPath) }

    var margin by remember { mutableFloatStateOf(-1f) }
    var gap by remember { mutableFloatStateOf(-1f) }
    LaunchedEffect(settings.sheetMarginMm, settings.sheetGapMm) {
        if (margin < 0f) margin = settings.sheetMarginMm
        if (gap < 0f) gap = settings.sheetGapMm
    }
    var rowsText by remember { mutableStateOf("") }
    var colsText by remember { mutableStateOf("") }
    var cutMarks by remember { mutableStateOf(true) }
    var page by remember { mutableIntStateOf(0) }

    val cfg = SheetConfig(
        marginMm = margin.coerceAtLeast(0f), gapMm = gap.coerceAtLeast(0f),
        rows = rowsText.toIntOrNull() ?: 0, cols = colsText.toIntOrNull() ?: 0, cutMarks = cutMarks,
    )
    val result = remember(queue, cfg) { SheetLayout.layout(queue, cfg) }
    LaunchedEffect(result.pages.size) {
        if (page >= result.pages.size) page = (result.pages.size - 1).coerceAtLeast(0)
    }

    val preview by produceState<ImageBitmap?>(null, result, page, logo, settings.dateFormat, cutMarks) {
        value = withContext(Dispatchers.Default) {
            result.pages.getOrNull(page)?.let { SheetLayout.previewBitmap(it, 3f, logo, settings.dateFormat, cutMarks).asImageBitmap() }
        }
    }

    fun launchMsg(block: suspend () -> String?) {
        scope.launch {
            runCatching { block() }.getOrElse { it.message ?: it.javaClass.simpleName }?.let { snack.showSnackbar(it) }
        }
    }

    val btPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.all { it }) launchMsg { LabelActions.bluetoothPrint(context, queue, logo, settings) }
        else launchMsg { context.getString(R.string.bt_permission_needed) }
    }

    AppScaffold(
        title = stringResource(R.string.print_labels),
        onBack = onBack,
        snackbar = snack,
        actions = {
            IconButton(onClick = { nav.navigate(Routes.saved(select = true)) }) { Icon(Icons.Filled.Add, stringResource(R.string.add_saved_labels)) }
            if (queue.isNotEmpty()) IconButton(onClick = { app.printQueue.clear() }) { Icon(Icons.Filled.DeleteSweep, stringResource(R.string.clear)) }
        },
    ) { pad ->
        if (queue.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(pad), horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(Icons.Filled.ViewModule, stringResource(R.string.print_empty))
                Button(onClick = { nav.navigate(Routes.saved(select = true)) }) { Text(stringResource(R.string.add_saved_labels)) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { nav.navigate(Routes.BULK) }) { Text(stringResource(R.string.bulk_maker)) }
            }
            return@AppScaffold
        }
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // ---- A4 preview ----
            SectionTitle(stringResource(R.string.a4_preview))
            Box(
                Modifier.fillMaxWidth(0.8f).align(Alignment.CenterHorizontally).aspectRatio(SheetLayout.A4_W / SheetLayout.A4_H)
                    .background(Color.White).border(1.dp, MaterialTheme.colorScheme.outline),
                contentAlignment = Alignment.Center,
            ) {
                val img = preview
                when {
                    result.pages.isEmpty() -> Text(result.warning ?: "", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                    img != null -> Image(img, stringResource(R.string.a4_preview), Modifier.fillMaxSize())
                    else -> CircularProgressIndicator()
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(enabled = page > 0, onClick = { page-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
                Text(stringResource(R.string.page_x_of_y, page + 1, result.pages.size.coerceAtLeast(1)))
                IconButton(enabled = page < result.pages.size - 1, onClick = { page++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
            }
            Text(
                stringResource(R.string.sheet_info, result.cols, result.rows, result.rows * result.cols, queue.sumOf { it.copies }, result.pages.size),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            result.warning?.takeIf { result.pages.isNotEmpty() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            // ---- Actions ----
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = result.pages.isNotEmpty(), modifier = Modifier.weight(1f), onClick = {
                    launchMsg {
                        val f = Exporter.exportSheetPdf(context, "a4_sticker_sheet", result, cfg, logo, settings.dateFormat)
                        withContext(Dispatchers.Main) { Exporter.printPdf(context, "A4 labels", f, result.pages.size, 210f, 297f, isA4 = true) }
                        null
                    }
                }) { Icon(Icons.Filled.Print, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.print_a4)) }
                FilledTonalButton(enabled = result.pages.isNotEmpty(), modifier = Modifier.weight(1f), onClick = {
                    launchMsg {
                        val f = Exporter.exportSheetPdf(context, "a4_sticker_sheet", result, cfg, logo, settings.dateFormat)
                        Exporter.saveToDownloads(context, f)
                        Exporter.share(context, Exporter.uriFor(context, f), "application/pdf")
                        context.getString(R.string.msg_pdf_ready, f.name)
                    }
                }) { Icon(Icons.Filled.PictureAsPdf, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.export_pdf)) }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                    launchMsg { LabelActions.systemPrint(context, "labels", queue, logo, settings); null }
                }) { Text(stringResource(R.string.print_individual)) }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                    val missing = BluetoothPrinter.requiredPermissions.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (missing.isNotEmpty()) btPermission.launch(missing.toTypedArray())
                    else launchMsg { LabelActions.bluetoothPrint(context, queue, logo, settings) }
                }) { Icon(Icons.Filled.Bluetooth, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.bluetooth)) }
            }
            Text(stringResource(R.string.print_scale_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))

            // ---- Sheet settings ----
            SectionTitle(stringResource(R.string.sheet_layout))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(colsText, { v -> colsText = v.filter { it.isDigit() }.take(2) }, label = { Text(stringResource(R.string.columns_auto)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(rowsText, { v -> rowsText = v.filter { it.isDigit() }.take(2) }, label = { Text(stringResource(R.string.rows_auto)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Text(stringResource(R.string.margin_value, cfg.marginMm), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            Slider(cfg.marginMm, { margin = it }, valueRange = 0f..30f)
            Text(stringResource(R.string.gap_value, cfg.gapMm), style = MaterialTheme.typography.labelMedium)
            Slider(cfg.gapMm, { gap = it }, valueRange = 0f..15f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cut_marks), Modifier.weight(1f))
                Switch(cutMarks, { cutMarks = it })
            }
            TextButton2(stringResource(R.string.save_as_default)) {
                scope.launch {
                    app.settings.update { it.copy(sheetMarginMm = cfg.marginMm, sheetGapMm = cfg.gapMm) }
                    snack.showSnackbar(context.getString(R.string.saved))
                }
            }

            // ---- Items ----
            SectionTitle(stringResource(R.string.labels_on_sheet, queue.size))
            queue.forEach { item ->
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(96.dp)) { LabelPreview(item.content, item.design, logo, settings.dateFormat, maxHeight = 54.dp) }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("%.0f×%.0f mm".format(item.design.widthMm, item.design.heightMm), style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { app.printQueue.setCopies(item.key, item.copies - 1) }) { Icon(Icons.Filled.Remove, null) }
                        Text(item.copies.toString())
                        IconButton(onClick = { app.printQueue.setCopies(item.key, item.copies + 1) }) { Icon(Icons.Filled.Add, null) }
                        IconButton(onClick = { app.printQueue.remove(item.key) }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TextButton2(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(text) }
}
