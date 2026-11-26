package com.aura.app.ui.screens.barang

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.core.format.RupiahFormatter
import com.aura.app.core.model.StatusStok
import com.aura.app.ui.barang.BarangTampilan
import com.aura.app.ui.components.ServerFileImage
import com.aura.app.ui.theme.Red
import com.aura.app.ui.theme.Soft
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Bg

@Composable
internal fun ProductListScreen(
    modifier: Modifier = Modifier,
    items: List<BarangTampilan>,
    search: String,
    setSearch: (String) -> Unit,
    detail: (String) -> Unit,
    edit: (String) -> Unit,
    archive: (String) -> Unit,
    manageCategories: () -> Unit,
    suppliers: () -> Unit,
    loading: Boolean,
    error: String?,
    retry: () -> Unit,
    fileBytes: Map<String, ByteArray>,
    failedFileIds: Set<String>,
    loadFile: (String) -> Unit,
    add: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("Semua") }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = items.filter { item ->
        val barang = item.barang
        val matchesQuery = barang.nama.contains(search, ignoreCase = true) ||
            barang.sku.orEmpty().contains(search, ignoreCase = true)
        val matchesStatus = filter == "Semua" || item.statusStok.name == filter
        matchesQuery && matchesStatus
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Bg).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.katalog_title), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.katalog_subtitle), color = Muted, fontSize = 12.sp)
                }
                Button(onClick = add, colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                    Text(stringResource(R.string.katalog_add))
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = setSearch,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.katalog_search)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("Semua", "AMAN", "MENIPIS", "HABIS", "DIARSIPKAN").forEach { status ->
                    val label = when (status) {
                        "AMAN" -> stringResource(R.string.katalog_status_safe)
                        "MENIPIS" -> stringResource(R.string.katalog_status_low)
                        "HABIS" -> stringResource(R.string.katalog_status_empty)
                        "DIARSIPKAN" -> stringResource(R.string.katalog_status_archived)
                        else -> stringResource(R.string.katalog_status_all)
                    }
                    FilterChip(
                        selected = filter == status,
                        onClick = { filter = status },
                        label = { Text(label) },
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = manageCategories) { Text(stringResource(R.string.katalog_manage_categories)) }
                TextButton(onClick = suppliers) { Text(stringResource(R.string.katalog_manage_suppliers)) }
            }
        }
        if (error != null) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Soft).padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(error, color = Red)
                    TextButton(onClick = retry) { Text(stringResource(R.string.katalog_retry)) }
                }
            }
        }
        if (loading && items.isEmpty()) {
            item { CircularProgressIndicator(Modifier.padding(28.dp).size(32.dp), color = Red) }
        } else if (visible.isEmpty()) {
            item {
                Text(
                    if (search.isBlank()) stringResource(R.string.katalog_empty) else stringResource(R.string.katalog_search_empty),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    color = Muted,
                )
            }
        } else {
            items(visible, key = { it.barang.id }) { item ->
                val barang = item.barang
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(androidx.compose.ui.graphics.Color.White).clickable { detail(barang.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ServerFileImage(
                        fileId = barang.fotoFileId,
                        bytes = barang.fotoFileId?.let(fileBytes::get),
                        gagalDimuat = barang.fotoFileId in failedFileIds,
                        onLoad = loadFile,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                        contentDescription = barang.nama,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(barang.nama, fontWeight = FontWeight.Bold)
                        Text(
                            "${item.kategori.joinToString(" • ").ifBlank { stringResource(R.string.katalog_no_category) }} • ${barang.stokSaatIni} ${barang.satuan}",
                            color = Muted,
                            fontSize = 11.sp,
                        )
                        Text(
                            "${RupiahFormatter.format(barang.hargaJual)} / ${barang.satuan}",
                            color = Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            when (item.statusStok) {
                                StatusStok.AMAN -> stringResource(R.string.katalog_status_safe)
                                StatusStok.MENIPIS -> stringResource(R.string.katalog_status_low)
                                StatusStok.HABIS -> stringResource(R.string.katalog_status_empty)
                                StatusStok.DIARSIPKAN -> stringResource(R.string.katalog_status_archived)
                            },
                            color = Muted,
                            fontSize = 11.sp,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(onClick = { edit(barang.id) }) {
                            Text(stringResource(R.string.screen_copy_083), color = Red)
                        }
                        if (barang.status.name != "DIARSIPKAN") {
                            TextButton(onClick = { deleteId = barang.id }) {
                                Text(stringResource(R.string.screen_copy_084), color = Red)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    deleteId?.let { id ->
        val productName = items.firstOrNull { it.barang.id == id }?.barang?.nama.orEmpty()
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text(stringResource(R.string.katalog_delete_title)) },
            text = { Text(stringResource(R.string.katalog_delete_confirmation, productName)) },
            confirmButton = {
                TextButton(onClick = { archive(id); deleteId = null }) {
                    Text(stringResource(R.string.katalog_delete_action), color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteId = null }) { Text(stringResource(R.string.screen_copy_043)) }
            },
        )
    }
}
