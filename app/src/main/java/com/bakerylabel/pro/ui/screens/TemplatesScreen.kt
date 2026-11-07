@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.navigation.NavController
import com.bakerylabel.pro.LocalApp
import com.bakerylabel.pro.R
import com.bakerylabel.pro.Routes
import com.bakerylabel.pro.data.AppSettings
import com.bakerylabel.pro.render.LabelTemplate
import com.bakerylabel.pro.ui.components.AppScaffold
import com.bakerylabel.pro.ui.components.ConfirmDialog
import kotlinx.coroutines.launch

/** "Label Designer": browse templates, then open one in the visual designer. */
@Composable
fun TemplatesScreen(nav: NavController, onBack: () -> Unit) {
    val app = LocalApp.current
    val repo = app.repo
    val scope = rememberCoroutineScope()
    val settings by app.settings.flow.collectAsStateWithLifecycle(AppSettings())
    val shop by repo.shop.observe().collectAsStateWithLifecycle(null)
    val logo = remember(shop?.logoPath) { repo.loadLogo(shop?.logoPath) }
    val templates = rememberAllTemplates()
    var cat by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<LabelTemplate?>(null) }
    val categories = templates.map { it.category }.distinct()

    AppScaffold(title = stringResource(R.string.label_designer), onBack = onBack) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Text(
                stringResource(R.string.templates_intro, templates.size),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(cat == null, { cat = null }, { Text(stringResource(R.string.all)) })
                categories.forEach { c -> FilterChip(cat == c, { cat = if (cat == c) null else c }, { Text(c) }) }
            }
            TemplateGrid(
                templates = templates.filter { cat == null || it.category == cat },
                shopName = shop?.shopName ?: "",
                logo = logo,
                dateFormat = settings.dateFormat,
                onPick = { t -> nav.navigate(Routes.editor(template = t.id, tab = 2)) },
                onLongPick = { deleting = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    deleting?.let { t ->
        ConfirmDialog(stringResource(R.string.delete_template), stringResource(R.string.delete_confirm, t.name), stringResource(R.string.delete),
            onConfirm = { t.userTemplateId?.let { id -> scope.launch { repo.templates.delete(id) } } }, onDismiss = { deleting = null })
    }
}
