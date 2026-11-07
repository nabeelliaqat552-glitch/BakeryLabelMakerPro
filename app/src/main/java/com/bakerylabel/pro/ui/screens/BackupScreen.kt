@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bakerylabel.pro.LocalApp
import com.bakerylabel.pro.R
import com.bakerylabel.pro.print.Exporter
import com.bakerylabel.pro.ui.components.AppScaffold
import com.bakerylabel.pro.ui.components.ConfirmDialog
import com.bakerylabel.pro.ui.components.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * Backup & restore. The backup is a single file holding products, categories, labels, templates,
 * shop profile (with logo) and settings. Android's file picker lets the user save it straight to
 * Google Drive, OneDrive, Dropbox or the phone, and restore it on any other phone.
 */
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val repo = LocalApp.current.repo
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    fun work(block: suspend () -> String) {
        scope.launch {
            busy = true
            val msg = runCatching { block() }.getOrElse { context.getString(R.string.backup_failed, it.message ?: "") }
            busy = false
            snack.showSnackbar(msg)
        }
    }

    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) work {
            val json = repo.exportBackupJson()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) } ?: error("Cannot write file")
            }
            context.getString(R.string.backup_saved)
        }
    }
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }

    val fileName = "bakery-labels-backup-${LocalDate.now()}.json"

    AppScaffold(title = stringResource(R.string.cloud_backup), onBack = onBack, snackbar = snack) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(stringResource(R.string.backup_intro), Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
            SectionTitle(stringResource(R.string.backup))
            Button(enabled = !busy, onClick = { createDoc.launch(fileName) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CloudUpload, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.backup_to_drive))
            }
            FilledTonalButton(enabled = !busy, onClick = {
                work {
                    val json = repo.exportBackupJson()
                    val f = File(File(context.cacheDir, "exports").apply { mkdirs() }, fileName)
                    withContext(Dispatchers.IO) { f.writeText(json) }
                    Exporter.share(context, Exporter.uriFor(context, f), "application/json")
                    context.getString(R.string.backup_ready_share)
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Share, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.share_backup))
            }
            SectionTitle(stringResource(R.string.restore))
            OutlinedButton(enabled = !busy, onClick = { openDoc.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CloudDownload, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.restore_from_file))
            }
            Text(stringResource(R.string.restore_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (busy) CircularProgressIndicator(Modifier.padding(16.dp))
            Spacer(Modifier.height(24.dp))
        }
    }

    pendingRestore?.let { uri ->
        ConfirmDialog(
            stringResource(R.string.restore), stringResource(R.string.restore_confirm), stringResource(R.string.restore),
            onConfirm = {
                work {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("Cannot read file")
                    }
                    context.getString(R.string.restore_done, repo.restoreBackupJson(json))
                }
            },
            onDismiss = { pendingRestore = null },
        )
    }
}
