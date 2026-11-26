package com.aura.app.ui.screens.barang

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.app.R
import com.aura.app.data.remote.dto.SupplierDetailResponseDto
import com.aura.app.ui.components.Page
import com.aura.app.ui.theme.Red
import com.aura.app.ui.theme.Muted

@Composable
internal fun SupplierDetailScreen(
    modifier: Modifier = Modifier,
    detail: SupplierDetailResponseDto?,
    onDelete: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    Page(modifier = modifier) {
        if (detail == null) {
            Text(stringResource(R.string.katalog_supplier_detail_loading), color = Muted)
        } else {
            val supplier = detail.supplier
            Text(supplier.nama, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            Text(supplier.catatan ?: stringResource(R.string.katalog_supplier_no_note), color = Muted)
            supplier.link?.let { link ->
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            onMessage(context.getString(R.string.katalog_link_unavailable))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                ) {
                    Text(stringResource(R.string.katalog_contact_supplier))
                }
            }
            Text(stringResource(R.string.katalog_supplier_items), fontWeight = FontWeight.Bold)
            if (detail.barang.isEmpty()) {
                Text(stringResource(R.string.katalog_supplier_no_items), color = Muted)
            } else {
                detail.barang.forEach { barang ->
                    val latestPrice = detail.hargaBeliTerakhir.firstOrNull { it.barangId == barang.id }?.hargaPerItem
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(barang.nama, fontWeight = FontWeight.SemiBold)
                        Text(
                            latestPrice?.let {
                                context.getString(
                                    R.string.katalog_supplier_last_price,
                                    com.aura.app.core.format.RupiahFormatter.format(it),
                                )
                            } ?: stringResource(R.string.katalog_supplier_no_price),
                            color = Muted,
                        )
                    }
                }
            }
            TextButton(onClick = { onDelete(supplier.id) }) {
                Text(stringResource(R.string.screen_copy_084), color = Red)
            }
        }
    }
}
