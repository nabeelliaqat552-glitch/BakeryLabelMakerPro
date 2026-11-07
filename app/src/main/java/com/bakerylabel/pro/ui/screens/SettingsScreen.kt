@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bakerylabel.pro.LocalApp
import com.bakerylabel.pro.R
import com.bakerylabel.pro.Routes
import com.bakerylabel.pro.data.AppSettings
import com.bakerylabel.pro.data.PrinterProtocol
import com.bakerylabel.pro.data.ThemeMode
import com.bakerylabel.pro.model.ColorThemes
import com.bakerylabel.pro.model.LabelSizes
import com.bakerylabel.pro.print.BluetoothPrinter
import com.bakerylabel.pro.print.BtDevice
import com.bakerylabel.pro.print.Exporter
import com.bakerylabel.pro.print.PrintItem
import com.bakerylabel.pro.ui.components.AppScaffold
import com.bakerylabel.pro.ui.components.ConfirmDialog
import com.bakerylabel.pro.ui.components.SectionTitle
import com.bakerylabel.pro.util.Dates
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(nav: NavController, onBack: () -> Unit) {
    val app = LocalApp.current
    val repo = app.repo
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val s by app.settings.flow.collectAsStateWithLifecycle(AppSettings())
    val printer = remember { BluetoothPrinter(context) }
    var devices by remember { mutableStateOf<List<BtDevice>?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var lang by remember { mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "en" }) }

    fun update(f: (AppSettings) -> AppSettings) = scope.launch { app.settings.update(f) }

    val btPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.all { it }) devices = printer.pairedDevices()
        else scope.launch { snack.showSnackbar(context.getString(R.string.bt_permission_needed)) }
    }
    fun openDevices() {
        val missing = BluetoothPrinter.requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        when {
            !printer.isSupported() -> scope.launch { snack.showSnackbar(context.getString(R.string.bt_not_supported)) }
            missing.isNotEmpty() -> btPermission.launch(missing.toTypedArray())
            else -> devices = printer.pairedDevices()
        }
    }

    AppScaffold(title = stringResource(R.string.app_settings), onBack = onBack, snackbar = snack) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // Language
            SectionTitle(stringResource(R.string.language))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("en" to "English", "ur" to "اردو").forEachIndexed { i, (tag, label) ->
                    SegmentedButton(lang.startsWith(tag), {
                        lang = tag
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    }, SegmentedButtonDefaults.itemShape(i, 2)) { Text(label) }
                }
            }

            // Theme
            SectionTitle(stringResource(R.string.theme))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark)
                    .forEachIndexed { i, (m, r) ->
                        SegmentedButton(s.themeMode == m, { update { it.copy(themeMode = m) } }, SegmentedButtonDefaults.itemShape(i, 3)) { Text(stringResource(r)) }
                    }
            }

            // Date format
            SectionTitle(stringResource(R.string.date_format))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Dates.formats.forEach { f ->
                    FilterChip(s.dateFormat == f, { update { it.copy(dateFormat = f) } }, { Text(Dates.displayName(f)) })
                }
            }
            Text(stringResource(R.string.example_date, Dates.format(Dates.today(), s.dateFormat)), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

            // Defaults for new labels
            SectionTitle(stringResource(R.string.default_label_size))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabelSizes.presets.forEach { sz ->
                    val sel = kotlin.math.abs(s.defaultWidthMm - sz.widthMm) < 0.2f && kotlin.math.abs(s.defaultHeightMm - sz.heightMm) < 0.2f
                    FilterChip(sel, { update { it.copy(defaultWidthMm = sz.widthMm, defaultHeightMm = sz.heightMm) } }, { Text(sz.label) })
                }
            }
            SectionTitle(stringResource(R.string.default_label_colour))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorThemes.all.forEachIndexed { i, t ->
                    FilterChip(s.defaultThemeIndex == i, { update { it.copy(defaultThemeIndex = i) } }, { Text(t.name) })
                }
            }
            SectionTitle(stringResource(R.string.default_shop_profile))
            ListItem(
                headlineContent = { Text(stringResource(R.string.shop_settings)) },
                supportingContent = { Text(stringResource(R.string.default_shop_hint)) },
                leadingContent = { Icon(Icons.Filled.Storefront, null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier.clickable { nav.navigate(Routes.SHOP) },
            )

            // Printer
            SectionTitle(stringResource(R.string.printer_settings))
            ListItem(
                headlineContent = { Text(s.printerName.ifBlank { stringResource(R.string.no_printer) }) },
                supportingContent = { Text(s.printerAddress.ifBlank { stringResource(R.string.pair_hint) }) },
                leadingContent = { Icon(Icons.Filled.Bluetooth, null) },
                trailingContent = { TextButton(onClick = { openDevices() }) { Text(stringResource(R.string.choose)) } },
            )
            Text(stringResource(R.string.printer_language), style = MaterialTheme.typography.labelMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                SegmentedButton(s.printerProtocol == PrinterProtocol.ESC_POS, { update { it.copy(printerProtocol = PrinterProtocol.ESC_POS) } },
                    SegmentedButtonDefaults.itemShape(0, 2)) { Text("ESC/POS") }
                SegmentedButton(s.printerProtocol == PrinterProtocol.TSPL, { update { it.copy(printerProtocol = PrinterProtocol.TSPL) } },
                    SegmentedButtonDefaults.itemShape(1, 2)) { Text("TSPL") }
            }
            Text(stringResource(R.string.protocol_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.printer_width), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(48f to "58 mm paper", 72f to "80 mm paper", 104f to "4 inch label").forEach { (w, l) ->
                    FilterChip(s.printerWidthMm == w, { update { it.copy(printerWidthMm = w) } }, { Text(l) })
                }
            }
            Text(stringResource(R.string.printer_dpi), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(203, 300).forEach { d -> FilterChip(s.printerDpi == d, { update { it.copy(printerDpi = d) } }, { Text("$d DPI") }) }
            }
            OutlinedButton(
                enabled = s.printerAddress.isNotBlank(),
                onClick = {
                    scope.launch {
                        val (c, d) = repo.newLabel()
                        val sample = c.copy(productName = c.productName.ifBlank { "Test Label" }, shopName = c.shopName.ifBlank { "Bakery Label Maker Pro" })
                        val logo = repo.loadLogo(repo.shop.get()?.logoPath)
                        snack.showSnackbar(LabelActions.bluetoothPrint(context, listOf(PrintItem("test", sample, d)), logo, s))
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text(stringResource(R.string.test_print)) }

            // Data
            SectionTitle(stringResource(R.string.backup_restore))
            ListItem(
                headlineContent = { Text(stringResource(R.string.cloud_backup)) },
                supportingContent = { Text(stringResource(R.string.backup_settings_hint)) },
                leadingContent = { Icon(Icons.Filled.CloudUpload, null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                modifier = Modifier.clickable { nav.navigate(Routes.BACKUP) },
            )
            SectionTitle(stringResource(R.string.data_export))
            FilledTonalButton(onClick = {
                scope.launch {
                    runCatching {
                        val csv = repo.exportProductsCsv()
                        val f = File(File(context.cacheDir, "exports").apply { mkdirs() }, "products.csv")
                        f.writeText(csv)
                        Exporter.share(context, Exporter.uriFor(context, f), "text/csv")
                    }.onFailure { snack.showSnackbar(it.message ?: "Error") }
                }
            }) { Icon(Icons.Filled.TableChart, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.export_products_csv)) }

            SectionTitle(stringResource(R.string.reset_settings))
            OutlinedButton(onClick = { confirmReset = true }) { Text(stringResource(R.string.reset_settings)) }
            Text(stringResource(R.string.reset_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.version_line, appVersion(context)), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(32.dp))
        }
    }

    devices?.let { list ->
        AlertDialog(
            onDismissRequest = { devices = null },
            title = { Text(stringResource(R.string.paired_printers)) },
            text = {
                Column {
                    if (!printer.isEnabled()) Text(stringResource(R.string.bt_off), color = MaterialTheme.colorScheme.error)
                    if (list.isEmpty()) Text(stringResource(R.string.no_paired))
                    list.forEach { d ->
                        ListItem(
                            headlineContent = { Text(d.name) }, supportingContent = { Text(d.address) },
                            modifier = Modifier.clickable {
                                update { it.copy(printerAddress = d.address, printerName = d.name) }
                                devices = null
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { devices = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmReset) {
        ConfirmDialog(stringResource(R.string.reset_settings), stringResource(R.string.reset_confirm), stringResource(R.string.reset),
            onConfirm = {
                scope.launch { app.settings.reset() }
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            },
            onDismiss = { confirmReset = false })
    }
}

private fun appVersion(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
}.getOrDefault("")
