@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bakerylabel.pro.model.FieldKey
import com.bakerylabel.pro.model.LabelContent
import com.bakerylabel.pro.model.LabelDesign
import com.bakerylabel.pro.render.LabelRenderer
import com.bakerylabel.pro.ui.theme.Gold
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Renders a label with the same code used for printing. Optional drag/resize handles for the designer. */
@Composable
fun LabelPreview(
    content: LabelContent,
    design: LabelDesign,
    logo: Bitmap?,
    dateFormat: String,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 240.dp,
    editable: Boolean = false,
    selected: FieldKey? = null,
    onSelect: (FieldKey?) -> Unit = {},
    onMove: (FieldKey, Float, Float) -> Unit = { _, _, _ -> },
    onResize: (FieldKey, Float, Float) -> Unit = { _, _, _ -> },
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val maxW = constraints.maxWidth.toFloat()
        val maxH = with(density) { maxHeight.toPx() }
        val pxPerMm = max(0.5f, min(maxW / design.widthMm, maxH / design.heightMm))
        val wPx = design.widthMm * pxPerMm
        val hPx = design.heightMm * pxPerMm
        val wDp = with(density) { wPx.toDp() }
        val hDp = with(density) { hPx.toDp() }

        val bitmap = remember(content, design, logo, dateFormat, pxPerMm) {
            val b = Bitmap.createBitmap(max(1, wPx.roundToInt()), max(1, hPx.roundToInt()), Bitmap.Config.ARGB_8888)
            LabelRenderer.draw(Canvas(b), content, design, pxPerMm, logo, dateFormat)
            b.asImageBitmap()
        }
        val move by rememberUpdatedState(onMove)
        val resize by rememberUpdatedState(onResize)
        val select by rememberUpdatedState(onSelect)
        val curW by rememberUpdatedState(wPx)
        val curH by rememberUpdatedState(hPx)

        Box(
            Modifier
                .size(wDp, hDp)
                .shadow(6.dp, RoundedCornerShape((design.cornerRadiusMm * pxPerMm / density.density).dp))
                .pointerInput(editable) { if (editable) detectTapGestures { select(null) } },
        ) {
            Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
            if (editable) {
                for ((k, el) in design.elements) {
                    if (k !in content.visible) continue
                    key(k) {
                        val isSel = k == selected
                        val ex = with(density) { (el.w * wPx).toDp() }
                        val ey = with(density) { (el.h * hPx).toDp() }
                        Box(
                            Modifier
                                .offset { IntOffset((el.x * curW).roundToInt(), (el.y * curH).roundToInt()) }
                                .size(ex, ey)
                                .border(
                                    if (isSel) 1.5.dp else 0.5.dp,
                                    if (isSel) Gold else Color(0x553C3C3C),
                                )
                                .pointerInput(k) {
                                    detectDragGestures(onDragStart = { select(k) }) { change, drag ->
                                        change.consume()
                                        move(k, drag.x / curW, drag.y / curH)
                                    }
                                }
                                .clickable { select(k) },
                        )
                        if (isSel) {
                            // Resize handle on the bottom-right corner.
                            Box(
                                Modifier
                                    .offset {
                                        IntOffset(
                                            ((el.x + el.w) * curW).roundToInt() - 18,
                                            ((el.y + el.h) * curH).roundToInt() - 18,
                                        )
                                    }
                                    .size(14.dp)
                                    .background(Gold, CircleShape)
                                    .border(1.dp, Color.White, CircleShape)
                                    .pointerInput(k) {
                                        detectDragGestures { change, drag ->
                                            change.consume()
                                            resize(k, drag.x / curW, drag.y / curH)
                                        }
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
