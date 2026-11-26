package com.aura.app.ui.screens.barang

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
    val categories = items.flatMap { it.kategori }.distinct().sorted()
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = items.filter { item ->
        val barang = item.barang
        val matchesQuery = barang.nama.contains(search, ignoreCase = true) ||
            barang.sku.orEmpty().contains(search, ignoreCase = true)
        val matchesCategory = filter == "Semua" || filter in item.kategori
        matchesQuery && matchesCategory
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
                    Text("Manajemen Barang", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Kelola katalog dan pantau ketersediaan stok", color = Muted, fontSize = 12.sp)
                }
                Button(onClick = add, colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                    Text(stringResource(R.string.katalog_add))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val active = items.count { it.statusStok == StatusStok.AMAN }
                val low = items.count { it.statusStok == StatusStok.MENIPIS }
                val empty = items.count { it.statusStok == StatusStok.HABIS }
                Box(Modifier.weight(1f)) { MiniStockMetric("Semua", items.size.toString(), Red) }
                Box(Modifier.weight(1f)) { MiniStockMetric("Aman", active.toString(), androidx.compose.ui.graphics.Color(0xFF16834A)) }
                Box(Modifier.weight(1f)) { MiniStockMetric("Menipis", low.toString(), androidx.compose.ui.graphics.Color(0xFFF59E0B)) }
                Box(Modifier.weight(1f)) { MiniStockMetric("Habis", empty.toString(), Red) }
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
                (listOf("Semua") + categories).forEach { status ->
                    val label = status
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
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                        .background(androidx.compose.ui.graphics.Color.White)
                        .border(1.dp, androidx.compose.ui.graphics.Color(0xFFE5E7EB), RoundedCornerShape(17.dp))
                        .clickable { detail(barang.id) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ServerFileImage(
                        fileId = barang.fotoFileId,
                        bytes = barang.fotoFileId?.let(fileBytes::get),
                        gagalDimuat = barang.fotoFileId in failedFileIds,
                        onLoad = loadFile,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
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
                            color = when (item.statusStok) {
                                StatusStok.AMAN -> androidx.compose.ui.graphics.Color(0xFF16834A)
                                StatusStok.MENIPIS -> androidx.compose.ui.graphics.Color(0xFFB7791F)
                                else -> Red
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
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


@Composable
private fun MiniStockMetric(label: String, value: String, accent: androidx.compose.ui.graphics.Color) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
            .background(androidx.compose.ui.graphics.Color.White).padding(vertical = 10.dp, horizontal = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        Text(label, color = Muted, fontSize = 9.sp)
    }
}
