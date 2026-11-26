package com.aura.app.ui.screens.barang

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.app.R
import com.aura.app.data.local.entity.KategoriEntity
import com.aura.app.data.local.entity.SupplierEntity
import com.aura.app.data.repository.DataBarang
import com.aura.app.ui.barang.BarangTampilan
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.ServerFileImage
import com.aura.app.ui.theme.Red

@Composable
internal fun ProductFormScreen(
    modifier: Modifier = Modifier,
    barang: BarangTampilan?,
    categories: List<KategoriEntity>,
    suppliers: List<SupplierEntity>,
    fileBytes: Map<String, ByteArray>,
    failedFileIds: Set<String>,
    uploadedPhotoFileId: String?,
    uploadingPhoto: Boolean,
    saving: Boolean,
    onUploadPhoto: (android.content.ContentResolver, Uri) -> Unit,
    onLoadFile: (String) -> Unit,
    onCreateCategory: (String) -> Unit,
    onManageCategories: () -> Unit,
    save: (DataBarang) -> Unit,
    back: () -> Unit,
) {
    val entity = barang?.barang
    val context = LocalContext.current
    var sku by rememberSaveable(entity?.id) { mutableStateOf(entity?.sku.orEmpty()) }
    var name by rememberSaveable(entity?.id) { mutableStateOf(entity?.nama.orEmpty()) }
    var unit by rememberSaveable(entity?.id) { mutableStateOf(entity?.satuan ?: "pcs") }
    var sell by rememberSaveable(entity?.id) { mutableStateOf(entity?.hargaJual?.toString().orEmpty()) }
    var minimum by rememberSaveable(entity?.id) { mutableStateOf(entity?.stokMinimum?.toString() ?: "0") }
    var initialStock by rememberSaveable(entity?.id) { mutableStateOf("0") }
    var initialPurchasePrice by rememberSaveable(entity?.id) { mutableStateOf("") }
    var selectedSupplierId by rememberSaveable(entity?.id) { mutableStateOf<String?>(null) }
    var selectedCategoryIds by rememberSaveable(entity?.id) {
        mutableStateOf(barang?.kategoriIds.orEmpty().joinToString(","))
    }
    var categoryName by rememberSaveable(entity?.id) { mutableStateOf("") }
    var categoryInputFocused by remember { mutableStateOf(false) }
    var pendingCategoryName by remember { mutableStateOf<String?>(null) }
    var categoryIdsBeforeCreate by remember { mutableStateOf<Set<String>>(emptySet()) }
    var photoFileId by rememberSaveable(entity?.id) { mutableStateOf(entity?.fotoFileId) }
    var unitMenuOpen by remember { mutableStateOf(false) }
    var supplierMenuOpen by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onUploadPhoto(context.contentResolver, uri)
    }

    androidx.compose.runtime.LaunchedEffect(entity?.id, uploadedPhotoFileId) {
        if (uploadedPhotoFileId != null) photoFileId = uploadedPhotoFileId
    }

    androidx.compose.runtime.LaunchedEffect(categories, pendingCategoryName) {
        val requestedName = pendingCategoryName ?: return@LaunchedEffect
        val createdCategory = categories.firstOrNull {
            it.id !in categoryIdsBeforeCreate && it.nama.equals(requestedName, ignoreCase = true)
        } ?: return@LaunchedEffect
        val selectedIds = selectedCategoryIds.split(',').filter(String::isNotBlank).toMutableSet()
        selectedIds.add(createdCategory.id)
        selectedCategoryIds = selectedIds.joinToString(",")
        categoryName = ""
        pendingCategoryName = null
    }

    Page(modifier = modifier) {
        Text(
            if (entity == null) stringResource(R.string.katalog_add_title) else stringResource(R.string.katalog_edit_title),
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
        )
        Text(stringResource(R.string.katalog_form_note))
        OutlinedTextField(sku, { sku = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.screen_copy_056)) }, singleLine = true)
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.screen_copy_057)) }, singleLine = true)
        Column {
            Text(stringResource(R.string.screen_copy_059))
            Button(onClick = { unitMenuOpen = true }, colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                Text(unit)
            }
            DropdownMenu(expanded = unitMenuOpen, onDismissRequest = { unitMenuOpen = false }) {
                listOf("pcs", "gram", "ml", "kg", "liter", "pack", "dus").forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { unit = option; unitMenuOpen = false },
                    )
                }
            }
        }
        OutlinedTextField(sell, { sell = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.screen_copy_060)) })
        OutlinedTextField(minimum, { minimum = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.screen_copy_063)) })

        Text(stringResource(R.string.katalog_categories))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { category ->
                val selected = category.id in selectedCategoryIds.split(',').filter(String::isNotBlank)
                FilterChip(
                    selected = selected,
                    onClick = {
                        val ids = selectedCategoryIds.split(',').filter(String::isNotBlank).toMutableSet()
                        if (selected) ids.remove(category.id) else ids.add(category.id)
                        selectedCategoryIds = ids.joinToString(",")
                    },
                    label = { Text(category.nama) },
                )
            }
        }
        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            modifier = Modifier.fillMaxWidth().onFocusChanged { categoryInputFocused = it.isFocused },
            label = { Text(stringResource(R.string.katalog_category_name)) },
            singleLine = true,
        )
        if (categoryInputFocused || categoryName.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val requestedName = categoryName.trim()
                        categoryIdsBeforeCreate = categories.map { it.id }.toSet()
                        pendingCategoryName = requestedName
                        onCreateCategory(requestedName)
                    },
                    enabled = categoryName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                ) {
                    Text(stringResource(R.string.katalog_create_category))
                }
                TextButton(onClick = { categoryName = ""; pendingCategoryName = null }) {
                    Text(stringResource(R.string.screen_copy_043))
                }
            }
        }
        TextButton(onClick = onManageCategories) { Text(stringResource(R.string.katalog_manage_categories)) }

        if (entity == null) {
            OutlinedTextField(initialStock, { initialStock = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.screen_copy_062)) })
            OutlinedTextField(
                initialPurchasePrice,
                { initialPurchasePrice = it.filter(Char::isDigit) },
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.katalog_initial_purchase_price)) },
            )
            if (initialPurchasePrice.isNotBlank()) {
                Column {
                    Text(stringResource(R.string.katalog_initial_supplier))
                    Button(onClick = { supplierMenuOpen = true }) {
                        Text(suppliers.firstOrNull { it.id == selectedSupplierId }?.nama ?: stringResource(R.string.katalog_select_supplier))
                    }
                    DropdownMenu(expanded = supplierMenuOpen, onDismissRequest = { supplierMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.katalog_no_supplier)) },
                            onClick = { selectedSupplierId = null; supplierMenuOpen = false },
                        )
                        suppliers.filter { it.status.name == "AKTIF" }.forEach { supplier ->
                            DropdownMenuItem(
                                text = { Text(supplier.nama) },
                                onClick = { selectedSupplierId = supplier.id; supplierMenuOpen = false },
                            )
                        }
                    }
                    val purchasePrice = initialPurchasePrice.toLongOrNull() ?: 0L
                    if (sell.toLongOrNull()?.let { it < purchasePrice } == true) {
                        Text(stringResource(R.string.katalog_price_warning), color = Red)
                    }
                }
            }
        } else {
            Text(stringResource(R.string.katalog_stock_edit_hint))
        }

        Text(stringResource(R.string.katalog_photo))
        ServerFileImage(
            fileId = photoFileId,
            bytes = photoFileId?.let(fileBytes::get),
            gagalDimuat = photoFileId in failedFileIds,
            onLoad = onLoadFile,
            modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp),
            contentDescription = name,
        )
        Button(onClick = { photoPicker.launch("image/*") }, enabled = !uploadingPhoto) {
            Text(if (uploadingPhoto) stringResource(R.string.katalog_uploading_photo) else stringResource(R.string.katalog_choose_photo))
        }

        Button(
            onClick = {
                val categoriesSelected = selectedCategoryIds.split(',').filter(String::isNotBlank)
                save(
                    DataBarang(
                        nama = name.trim(),
                        satuan = unit,
                        sku = sku.trim().takeIf(String::isNotEmpty),
                        hargaJual = sell.toLongOrNull() ?: 0,
                        stokMinimum = minimum.toIntOrNull() ?: 0,
                        stokAwal = if (entity == null) initialStock.toIntOrNull() ?: 0 else 0,
                        fotoFileId = photoFileId,
                        kategoriIds = categoriesSelected,
                        hargaBeliAwal = if (entity == null) initialPurchasePrice.toLongOrNull() else null,
                        supplierId = if (entity == null) selectedSupplierId else null,
                    ),
                )
            },
            enabled = name.isNotBlank() && (sell.toLongOrNull() ?: -1) >= 0 &&
                (minimum.toIntOrNull() ?: -1) >= 0 &&
                (entity != null || (initialStock.toIntOrNull() ?: -1) >= 0) && !saving && !uploadingPhoto,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Red),
        ) {
            Text(if (saving) stringResource(R.string.katalog_saving) else stringResource(R.string.screen_copy_064))
        }
        TextButton(onClick = back, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.screen_copy_043)) }
    }
}
