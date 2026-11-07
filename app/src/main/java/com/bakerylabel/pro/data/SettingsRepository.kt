package com.bakerylabel.pro.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class PrinterProtocol { ESC_POS, TSPL }

data class AppSettings(
    val dateFormat: String = "dd/MM/yyyy",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultWidthMm: Float = 50.8f,
    val defaultHeightMm: Float = 25.4f,
    val defaultThemeIndex: Int = 0,
    val printerAddress: String = "",
    val printerName: String = "",
    val printerProtocol: PrinterProtocol = PrinterProtocol.ESC_POS,
    /** Printable width of the printer head in mm (48 for 58 mm paper, 72 for 80 mm, 104 for 4-inch label printers). */
    val printerWidthMm: Float = 48f,
    val printerDpi: Int = 203,
    val sheetMarginMm: Float = 8f,
    val sheetGapMm: Float = 3f,
    val seeded: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private object K {
        val dateFormat = stringPreferencesKey("date_format")
        val themeMode = stringPreferencesKey("theme_mode")
        val defW = floatPreferencesKey("def_w")
        val defH = floatPreferencesKey("def_h")
        val defTheme = intPreferencesKey("def_theme")
        val prAddr = stringPreferencesKey("printer_addr")
        val prName = stringPreferencesKey("printer_name")
        val prProto = stringPreferencesKey("printer_proto")
        val prWidth = floatPreferencesKey("printer_width")
        val prDpi = intPreferencesKey("printer_dpi")
        val margin = floatPreferencesKey("sheet_margin")
        val gap = floatPreferencesKey("sheet_gap")
        val seeded = booleanPreferencesKey("seeded")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = flow.first()

    private fun Preferences.toSettings() = AppSettings(
        dateFormat = this[K.dateFormat] ?: "dd/MM/yyyy",
        themeMode = runCatching { ThemeMode.valueOf(this[K.themeMode] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
        defaultWidthMm = this[K.defW] ?: 50.8f,
        defaultHeightMm = this[K.defH] ?: 25.4f,
        defaultThemeIndex = this[K.defTheme] ?: 0,
        printerAddress = this[K.prAddr] ?: "",
        printerName = this[K.prName] ?: "",
        printerProtocol = runCatching { PrinterProtocol.valueOf(this[K.prProto] ?: "ESC_POS") }.getOrDefault(PrinterProtocol.ESC_POS),
        printerWidthMm = this[K.prWidth] ?: 48f,
        printerDpi = this[K.prDpi] ?: 203,
        sheetMarginMm = this[K.margin] ?: 8f,
        sheetGapMm = this[K.gap] ?: 3f,
        seeded = this[K.seeded] ?: false,
    )

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val n = transform(p.toSettings())
            p[K.dateFormat] = n.dateFormat
            p[K.themeMode] = n.themeMode.name
            p[K.defW] = n.defaultWidthMm
            p[K.defH] = n.defaultHeightMm
            p[K.defTheme] = n.defaultThemeIndex
            p[K.prAddr] = n.printerAddress
            p[K.prName] = n.printerName
            p[K.prProto] = n.printerProtocol.name
            p[K.prWidth] = n.printerWidthMm
            p[K.prDpi] = n.printerDpi
            p[K.margin] = n.sheetMarginMm
            p[K.gap] = n.sheetGapMm
            p[K.seeded] = n.seeded
        }
    }

    /** Restores every setting to its default but keeps the "seeded" flag so starter data isn't re-added. */
    suspend fun reset() = update { AppSettings(seeded = it.seeded) }
}
