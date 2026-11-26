package com.aura.app.ui.screens.barang

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.app.R
import com.aura.app.ui.barang.BarangTampilan
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Input
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.Primary
import com.aura.app.ui.components.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdjustmentScreen(
    products: List<BarangTampilan>,
    initialProductId: String?,
    saving: Boolean,
    save: (String, Int, String, String) -> Unit,
    back: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeProducts = products.filter { it.barang.status.name == "AKTIF" }
    var barangId by rememberSaveable { mutableStateOf(initialProductId ?: activeProducts.firstOrNull()?.barang?.id.orEmpty()) }
    var stockText by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("RUSAK") }
    var note by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val selected = activeProducts.firstOrNull { it.barang.id == barangId }?.barang
    val stock = stockText.toIntOrNull()
    val difference = stock?.let { it - (selected?.stokSaatIni ?: 0) }
    val reasons = listOf(
        "RUSAK" to R.string.adjustment_reason_damaged,
        "HILANG" to R.string.adjustment_reason_lost,
        "KADALUARSA" to R.string.adjustment_reason_expired,
        "SALAH_CATAT" to R.string.adjustment_reason_misrecorded,
        "LAINNYA" to R.string.adjustment_reason_other,
    )

    Page(modifier = modifier) {
        Heading(stringResource(R.string.screen_copy_038), stringResource(R.string.adjustment_owner_only))
        ExposedDropdownMenuBox(expanded, { expanded = it }) {
            OutlinedTextField(
                value = selected?.nama ?: stringResource(R.string.restock_select_product),
                onValueChange = {},
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                readOnly = true,
                label = { Text(stringResource(R.string.screen_copy_070)) },
            )
            ExposedDropdownMenu(expanded, { expanded = false }) {
                activeProducts.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.barang.nama) },
                        onClick = { barangId = item.barang.id; expanded = false },
                    )
                }
            }
        }
        selected?.let { Text(stringResource(R.string.adjustment_system_stock, it.stokSaatIni, it.satuan)) }
        Input(stringResource(R.string.screen_copy_039), stockText, { stockText = it }, number = true)
        Text(stringResource(R.string.screen_copy_040))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            reasons.forEach { (value, label) ->
                FilterChip(
                    selected = reason == value,
                    onClick = { reason = value },
                    label = { Text(stringResource(label)) },
                )
            }
        }
        difference?.let {
            Text(stringResource(R.string.adjustment_difference, if (it > 0) "+$it" else it.toString()))
        }
        Input(stringResource(R.string.screen_copy_041), note, { note = it })
        Primary(
            stringResource(R.string.screen_copy_042),
            { save(barangId, stock ?: -1, reason, note) },
            !saving && barangId.isNotBlank() && stock != null && stock >= 0,
        )
        Secondary(stringResource(R.string.screen_copy_043), back)
    }
}
