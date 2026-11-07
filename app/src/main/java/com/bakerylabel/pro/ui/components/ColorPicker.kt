@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bakerylabel.pro.R

private val swatches = listOf(
    0xFF7A1F2B, 0xFF4E0F18, 0xFFD4A944, 0xFFF1D78A, 0xFFFFF8EC, 0xFF4A2C21, 0xFF121010, 0xFFFFFFFF,
    0xFF1B3F8B, 0xFFC62828, 0xFF2E7D32, 0xFFD6487A, 0xFFEF6C00, 0xFF6A1B9A, 0xFF00838F, 0xFF9E9E9E,
).map { it.toInt() }

fun Int.toHex(): String = String.format("#%06X", this and 0xFFFFFF)

/** HSV colour picker with swatches and hex entry: any colour can be chosen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(title: String, initial: Int, onPicked: (Int) -> Unit, onDismiss: () -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1]) }
    var v by remember { mutableFloatStateOf(hsv[2]) }
    val current = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
    var hex by remember { mutableStateOf(initial.toHex()) }

    fun setFrom(color: Int) {
        val a = FloatArray(3)
        android.graphics.Color.colorToHSV(color, a)
        h = a[0]; s = a[1]; v = a[2]
        hex = color.toHex()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(48.dp).clip(MaterialTheme.shapes.small)
                        .background(Color(current)).border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
                )
                Text(stringResource(R.string.hue), style = MaterialTheme.typography.labelMedium)
                Box(
                    Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(
                        Brush.horizontalGradient((0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) }),
                    ),
                )
                Slider(h, { h = it; hex = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)).toHex() }, valueRange = 0f..360f)
                Text(stringResource(R.string.saturation), style = MaterialTheme.typography.labelMedium)
                Slider(s, { s = it; hex = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)).toHex() })
                Text(stringResource(R.string.brightness), style = MaterialTheme.typography.labelMedium)
                Slider(v, { v = it; hex = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)).toHex() })
                OutlinedTextField(
                    hex,
                    {
                        hex = it
                        runCatching { android.graphics.Color.parseColor(it.trim()) }.getOrNull()?.let { c ->
                            val a = FloatArray(3); android.graphics.Color.colorToHSV(c, a); h = a[0]; s = a[1]; v = a[2]
                        }
                    },
                    label = { Text("HEX") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    swatches.forEach { c ->
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(Color(c))
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { setFrom(c) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPicked(current or 0xFF000000.toInt()); onDismiss() }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Small row: label + colour circle that opens the picker. */
@Composable
fun ColorRow(label: String, color: Int, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { open = true }.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(color.toHex(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(30.dp).clip(CircleShape).background(Color(color)).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
    }
    if (open) ColorPickerDialog(label, color, onChange) { open = false }
}
