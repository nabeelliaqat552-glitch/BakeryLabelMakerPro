@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.bakerylabel.pro.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bakerylabel.pro.LocalApp
import com.bakerylabel.pro.R
import com.bakerylabel.pro.data.ShopProfileEntity
import com.bakerylabel.pro.ui.components.AppScaffold
import com.bakerylabel.pro.ui.components.SectionTitle
import kotlinx.coroutines.launch

@Composable
fun ShopScreen(onBack: () -> Unit) {
    val repo = LocalApp.current.repo
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var p by remember { mutableStateOf<ShopProfileEntity?>(null) }
    LaunchedEffect(Unit) { p = repo.shop.get() ?: ShopProfileEntity() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            runCatching { repo.importLogo(uri) }
                .onSuccess { path -> p = p?.copy(logoPath = path) }
                .onFailure { snack.showSnackbar(it.message ?: "Could not load image") }
        }
    }

    AppScaffold(title = stringResource(R.string.shop_settings), onBack = onBack, snackbar = snack) { pad ->
        val s = p ?: return@AppScaffold
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.shop_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SectionTitle(stringResource(R.string.f_logo))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val bmp = remember(s.logoPath) { repo.loadLogo(s.logoPath) }
                Box(
                    Modifier.size(96.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize().padding(6.dp))
                    else Icon(Icons.Filled.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.outline)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text(stringResource(if (s.logoPath == null) R.string.choose_logo else R.string.change_logo))
                    }
                    if (s.logoPath != null) TextButton(onClick = { p = s.copy(logoPath = null) }) { Text(stringResource(R.string.remove_logo)) }
                }
            }
            SectionTitle(stringResource(R.string.shop_details))
            ShopField(stringResource(R.string.f_shop), s.shopName) { p = s.copy(shopName = it) }
            ShopField(stringResource(R.string.f_bakery), s.bakeryName) { p = s.copy(bakeryName = it) }
            ShopField(stringResource(R.string.address), s.address, multiline = true) { p = s.copy(address = it) }
            ShopField(stringResource(R.string.phone), s.phone, KeyboardType.Phone) { p = s.copy(phone = it) }
            ShopField(stringResource(R.string.whatsapp), s.whatsapp, KeyboardType.Phone) { p = s.copy(whatsapp = it) }
            ShopField(stringResource(R.string.website), s.website, KeyboardType.Uri) { p = s.copy(website = it) }
            ShopField(stringResource(R.string.food_license), s.license) { p = s.copy(license = it) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    repo.shop.save(s.copy(
                        shopName = s.shopName.trim(), bakeryName = s.bakeryName.trim(), address = s.address.trim(),
                        phone = s.phone.trim(), whatsapp = s.whatsapp.trim(), website = s.website.trim(), license = s.license.trim(),
                    ))
                    snack.showSnackbar(context.getString(R.string.shop_saved))
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ShopField(label: String, value: String, keyboard: KeyboardType = KeyboardType.Text, multiline: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange, label = { Text(label) }, singleLine = !multiline,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard), modifier = Modifier.fillMaxWidth(),
    )
}
