package com.aura.app.ui.screens.barang

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.core.format.RupiahFormatter
import com.aura.app.ui.barang.BarangTampilan
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.ServerFileImage
import com.aura.app.ui.theme.Red
import com.aura.app.ui.theme.Muted

@Composable
internal fun ProductDetailScreen(
    modifier: Modifier = Modifier,
    item: BarangTampilan?,
    hargaBeliSupplier: List<com.aura.app.data.remote.dto.HargaBeliSupplierDto>,
    fileBytes: Map<String, ByteArray>,
    failedFileIds: Set<String>,
    onLoadFile: (String) -> Unit,
    restock: () -> Unit,
    edit: () -> Unit,
    adjust: () -> Unit,
    movement: () -> Unit,
    archive: () -> Unit,
) {
    var confirmArchive by remember(item?.barang?.id) { mutableStateOf(false) }
    Page(modifier = modifier) {
        if (item == null) {
            Text(stringResource(R.string.screen_copy_046))
        } else {
            val barang = item.barang
            ServerFileImage(
                fileId = barang.fotoFileId,
                bytes = barang.fotoFileId?.let(fileBytes::get),
                gagalDimuat = barang.fotoFileId in failedFileIds,
                onLoad = onLoadFile,
                modifier = Modifier.fillMaxWidth().height(190.dp).padding(vertical = 10.dp),
                contentDescription = barang.nama,
            )
            Text(barang.nama, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                listOfNotNull(barang.sku?.takeIf(String::isNotBlank), item.kategori.joinToString(" • ").takeIf(String::isNotBlank))
                    .joinToString(" • "),
                color = Muted,
            )
            Text("${RupiahFormatter.format(barang.hargaJual)} / ${barang.satuan}", color = Red, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.screen_copy_048), color = Muted)
                    Text("${barang.stokSaatIni} ${barang.satuan}", fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.screen_copy_049), color = Muted)
                    Text(RupiahFormatter.format(barang.hppRataRata), fontWeight = FontWeight.Bold)
                }
            }
            Text(
                stringResource(R.string.katalog_minimum_stock, barang.stokMinimum, barang.satuan),
                color = Muted,
            )
            if (hargaBeliSupplier.isNotEmpty()) {
                Text(stringResource(R.string.katalog_purchase_prices), fontWeight = FontWeight.Bold)
                hargaBeliSupplier.forEach { harga ->
                    Text("${harga.namaSupplier}: ${RupiahFormatter.format(harga.hargaPerItem)} / ${barang.satuan}")
                }
            }
            Button(onClick = restock, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                Text(stringResource(R.string.screen_copy_051))
            }
            TextButton(onClick = edit, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.screen_copy_052)) }
            TextButton(onClick = adjust, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.screen_copy_053)) }
            TextButton(onClick = movement, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.screen_copy_054)) }
            TextButton(onClick = { confirmArchive = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.katalog_delete_action), color = Red)
            }
        }
    }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text(stringResource(R.string.katalog_delete_title)) },
            text = { Text(stringResource(R.string.katalog_delete_confirmation, item?.barang?.nama.orEmpty())) },
            confirmButton = {
                TextButton(onClick = { archive(); confirmArchive = false }) {
                    Text(stringResource(R.string.katalog_delete_action), color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }) { Text(stringResource(R.string.screen_copy_043)) }
            },
        )
    }
}
