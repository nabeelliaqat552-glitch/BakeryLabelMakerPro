@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bakerylabel.pro.LocalApp
import com.bakerylabel.pro.R
import com.bakerylabel.pro.Routes
import com.bakerylabel.pro.data.AppSettings
import com.bakerylabel.pro.model.LabelContent
import com.bakerylabel.pro.model.LabelDesign
import com.bakerylabel.pro.ui.components.ActionTile
import com.bakerylabel.pro.ui.components.LabelPreview
import com.bakerylabel.pro.ui.components.PressCard
import com.bakerylabel.pro.ui.components.SectionTitle
import com.bakerylabel.pro.ui.theme.Gold
import com.bakerylabel.pro.ui.theme.GoldLight
import com.bakerylabel.pro.ui.theme.HeroGradient
import com.bakerylabel.pro.util.Dates

private data class DashAction(val icon: ImageVector, val label: Int, val route: String, val highlight: Boolean = false)

@Composable
fun DashboardScreen(nav: NavController) {
    val app = LocalApp.current
    val repo = app.repo
    val productCount by repo.products.observeCount().collectAsStateWithLifecycle(0)
    val labelCount by repo.labels.observeCount().collectAsStateWithLifecycle(0)
    val recent by repo.labels.observeRecent(8).collectAsStateWithLifecycle(emptyList())
    val frequent by repo.products.observeFrequent(10).collectAsStateWithLifecycle(emptyList())
    val shop by repo.shop.observe().collectAsStateWithLifecycle(null)
    val settings by app.settings.flow.collectAsStateWithLifecycle(AppSettings())
    val logo = remember(shop?.logoPath) { repo.loadLogo(shop?.logoPath) }

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val actions = listOf(
        DashAction(Icons.Filled.AddCircle, R.string.create_label, Routes.editor(), highlight = true),
        DashAction(Icons.Filled.Inventory2, R.string.all_products, Routes.products("all")),
        DashAction(Icons.Filled.BakeryDining, R.string.bakery_products, Routes.products("bakery")),
        DashAction(Icons.Filled.Cake, R.string.sweets_mithai, Routes.products("sweets")),
        DashAction(Icons.Filled.LibraryAdd, R.string.custom_products, Routes.products("custom")),
        DashAction(Icons.Filled.Collections, R.string.saved_labels, Routes.saved()),
        DashAction(Icons.Filled.Layers, R.string.bulk_maker, Routes.BULK, highlight = true),
        DashAction(Icons.Filled.Brush, R.string.label_designer, Routes.TEMPLATES),
        DashAction(Icons.Filled.Print, R.string.print_labels, Routes.PRINT),
        DashAction(Icons.Filled.Storefront, R.string.shop_settings, Routes.SHOP),
        DashAction(Icons.Filled.CloudUpload, R.string.cloud_backup, Routes.BACKUP),
        DashAction(Icons.Filled.Settings, R.string.app_settings, Routes.SETTINGS),
    )

    Scaffold(contentWindowInsets = WindowInsets(0)) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
        ) {
            // Hero header
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(HeroGradient)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Column {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = GoldLight)
                    Text(
                        shop?.shopName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.dashboard_tagline),
                        style = MaterialTheme.typography.bodyMedium, color = Color(0xFFF6E8D0),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatBox(stringResource(R.string.total_products), productCount.toString(), Modifier.weight(1f))
                        StatBox(stringResource(R.string.total_labels), labelCount.toString(), Modifier.weight(1f))
                    }
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                AnimatedVisibility(shown, enter = fadeIn() + slideInVertically { it / 6 }) {
                    Column {
                        SectionTitle(stringResource(R.string.quick_actions))
                        actions.chunked(3).forEach { row ->
                            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { a ->
                                    ActionTile(a.icon, stringResource(a.label), { nav.navigate(a.route) }, Modifier.weight(1f), a.highlight)
                                }
                            }
                        }
                    }
                }

                SectionTitle(stringResource(R.string.recent_labels))
                if (recent.isEmpty()) {
                    Text(stringResource(R.string.no_labels_yet), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        recent.forEach { l ->
                            val c = remember(l.contentJson) { LabelContent.fromJsonString(l.contentJson) }
                            val d = remember(l.designJson) { LabelDesign.fromJsonString(l.designJson) }
                            PressCard(onClick = { nav.navigate(Routes.editor(labelId = l.id)) }, modifier = Modifier.width(170.dp)) {
                                Column(Modifier.padding(10.dp)) {
                                    LabelPreview(c, d, logo, settings.dateFormat, maxHeight = 80.dp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(l.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("MFG " + Dates.format(c.mfgEpochDay, settings.dateFormat), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                SectionTitle(stringResource(R.string.frequent_products))
                if (frequent.isEmpty()) {
                    Text(stringResource(R.string.frequent_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        frequent.forEach { p ->
                            AssistChip(
                                onClick = { nav.navigate(Routes.editor(productId = p.id)) },
                                label = { Text("${p.name} · ${p.useCount}") },
                                leadingIcon = { Icon(Icons.Filled.Tune, null, tint = MaterialTheme.colorScheme.primary) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(MaterialTheme.shapes.medium).background(Color(0x22FFFFFF)).padding(14.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = Gold, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFFF6E8D0))
    }
}
