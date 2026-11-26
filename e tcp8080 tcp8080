package com.aura.app.ui.screens.kasir

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.aura.app.ui.theme.Bg
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Red
import com.aura.app.ui.theme.Soft

@Composable
internal fun CashierScreen(
    modifier: Modifier = Modifier,
    items: List<BarangTampilan>,
    search: String,
    setSearch: (String) -> Unit,
    fileBytes: Map<String, ByteArray>,
    failedFileIds: Set<String>,
    loadFile: (String) -> Unit,
    add: (com.aura.app.data.local.entity.BarangEntity) -> Unit,
    cart: () -> Unit,
    history: () -> Unit,
    avatar: () -> Unit,
) {
    var category by rememberSaveable { mutableStateOf("Semua") }
    val categories = items.flatMap { it.kategori }.distinct().sorted()
    val visible = items.filter { item ->
        val barang = item.barang
        (barang.nama.contains(search, true) || barang.sku.orEmpty().contains(search, true)) &&
            (category == "Semua" || category in item.kategori)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().background(Bg).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.kasir_catalog_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.kasir_catalog_subtitle), color = Muted, fontSize = 12.sp)
                }
                TextButton(onClick = avatar) { Text(stringResource(R.string.screen_copy_100), color = Red) }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = search,
                onValueChange = setSearch,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.katalog_search)) },
                singleLine = true,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (listOf("Semua") + categories).forEach { value ->
                    FilterChip(
                        selected = category == value,
                        onClick = { category = value },
                        label = { Text(value) },
                    )
                }
            }
        }
        if (visible.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.kasir_catalog_empty),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                    color = Muted,
                )
            }
        } else {
            items(visible, key = { it.barang.id }) { item ->
                val barang = item.barang
                val habis = item.statusStok == StatusStok.HABIS
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(if (habis) Soft else androidx.compose.ui.graphics.Color.White)
                        .clickable(enabled = !habis) { add(barang) }.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    ServerFileImage(
                        fileId = barang.fotoFileId,
                        bytes = barang.fotoFileId?.let(fileBytes::get),
                        gagalDimuat = barang.fotoFileId?.let(failedFileIds::contains) == true,
                        onLoad = loadFile,
                        modifier = Modifier.fillMaxWidth().size(92.dp).clip(RoundedCornerShape(12.dp)),
                        contentDescription = barang.nama,
                    )
                    Text(barang.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(item.kategori.joinToString(" • ").ifBlank { barang.satuan }, color = Muted, fontSize = 10.sp)
                    Text(RupiahFormatter.format(barang.hargaJual), color = Red, fontWeight = FontWeight.Bold)
                    Text(
                        if (habis) stringResource(R.string.katalog_status_empty)
                        else stringResource(R.string.kasir_stock_count, barang.stokSaatIni, barang.satuan),
                        color = if (habis) Red else Muted,
                        fontSize = 11.sp,
                    )
                    Button(
                        onClick = { add(barang) },
                        enabled = !habis,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                    ) {
                        Text(if (habis) stringResource(R.string.katalog_status_empty) else stringResource(R.string.screen_copy_095))
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Button(
                    onClick = cart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                ) {
                    Text(stringResource(R.string.screen_copy_101))
                }
                TextButton(onClick = history, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.screen_copy_102))
                }
            }
        }
    }
}
