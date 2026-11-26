package com.aura.app.ui.screens.barang

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.app.R
import com.aura.app.core.format.RupiahFormatter
import com.aura.app.core.model.StatusAkun
import com.aura.app.data.local.entity.SupplierEntity
import com.aura.app.ui.barang.BarangTampilan
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Input
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.Primary
import com.aura.app.ui.components.Secondary
import com.aura.app.ui.theme.Red
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RestockScreen(
    modifier: Modifier = Modifier,
    products: List<BarangTampilan>,
    suppliers: List<SupplierEntity>,
    initialProductId: String?,
    saving: Boolean,
    creatingSupplier: Boolean,
    save: (String, String, Int, Long, String) -> Unit,
    createSupplier: (String) -> Unit,
    manageSuppliers: () -> Unit,
    back: () -> Unit,
) {
    val activeSuppliers = suppliers.filter { it.status == StatusAkun.AKTIF }
    var productId by rememberSaveable { mutableStateOf(initialProductId ?: products.firstOrNull()?.barang?.id.orEmpty()) }
    var supplierId by rememberSaveable { mutableStateOf(activeSuppliers.firstOrNull()?.id.orEmpty()) }
    var quantityText by rememberSaveable { mutableStateOf("1") }
    var totalText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var newSupplierName by rememberSaveable { mutableStateOf("") }
    var supplierInputFocused by remember { mutableStateOf(false) }
    var pendingSupplierName by remember { mutableStateOf<String?>(null) }
    var supplierIdsBeforeCreate by remember { mutableStateOf<Set<String>>(emptySet()) }
    var productExpanded by remember { mutableStateOf(false) }
    var supplierExpanded by remember { mutableStateOf(false) }
    val quantity = quantityText.toIntOrNull() ?: 0
    val total = totalText.toLongOrNull() ?: -1L
    val unitPrice = if (quantity > 0 && total >= 0) {
        BigDecimal.valueOf(total).divide(BigDecimal.valueOf(quantity.toLong()), 0, RoundingMode.HALF_UP).longValueExact()
    } else {
        null
    }

    androidx.compose.runtime.LaunchedEffect(activeSuppliers, pendingSupplierName) {
        val requestedName = pendingSupplierName ?: return@LaunchedEffect
        val createdSupplier = activeSuppliers.firstOrNull {
            it.id !in supplierIdsBeforeCreate && it.nama.equals(requestedName, ignoreCase = true)
        } ?: return@LaunchedEffect
        supplierId = createdSupplier.id
        newSupplierName = ""
        pendingSupplierName = null
    }

    Page(modifier = modifier) {
        Heading(stringResource(R.string.screen_copy_069), stringResource(R.string.restock_effects))
        ExposedDropdownMenuBox(productExpanded, { productExpanded = it }) {
            OutlinedTextField(
                value = products.firstOrNull { it.barang.id == productId }?.barang?.nama
                    ?: stringResource(R.string.restock_select_product),
                onValueChange = {},
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                readOnly = true,
                label = { Text(stringResource(R.string.screen_copy_070)) },
            )
            ExposedDropdownMenu(productExpanded, { productExpanded = false }) {
                products.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.barang.nama) },
                        onClick = { productId = item.barang.id; productExpanded = false },
                    )
                }
            }
        }
        ExposedDropdownMenuBox(supplierExpanded, { supplierExpanded = it }) {
            OutlinedTextField(
                value = activeSuppliers.firstOrNull { it.id == supplierId }?.nama
                    ?: stringResource(R.string.restock_select_supplier),
                onValueChange = {},
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                readOnly = true,
                label = { Text(stringResource(R.string.katalog_select_supplier)) },
            )
            ExposedDropdownMenu(supplierExpanded, { supplierExpanded = false }) {
                activeSuppliers.forEach { supplier ->
                    DropdownMenuItem(
                        text = { Text(supplier.nama) },
                        onClick = { supplierId = supplier.id; supplierExpanded = false },
                    )
                }
            }
        }
        OutlinedTextField(
            value = newSupplierName,
            onValueChange = { newSupplierName = it },
            modifier = Modifier.fillMaxWidth().onFocusChanged { supplierInputFocused = it.isFocused },
            enabled = !creatingSupplier,
            label = { Text(stringResource(R.string.katalog_supplier_name)) },
            singleLine = true,
        )
        if (supplierInputFocused || newSupplierName.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val requestedName = newSupplierName.trim()
                        supplierIdsBeforeCreate = activeSuppliers.map { it.id }.toSet()
                        pendingSupplierName = requestedName
                        createSupplier(requestedName)
                    },
                    enabled = newSupplierName.isNotBlank() && !creatingSupplier,
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                ) {
                    Text(
                        if (creatingSupplier) stringResource(R.string.katalog_saving)
                        else stringResource(R.string.katalog_create_supplier),
                    )
                }
                TextButton(
                    onClick = { newSupplierName = ""; pendingSupplierName = null },
                    enabled = !creatingSupplier,
                ) {
                    Text(stringResource(R.string.screen_copy_043))
                }
            }
        }
        TextButton(onClick = manageSuppliers) {
            Text(stringResource(R.string.restock_manage_suppliers))
        }
        Input(stringResource(R.string.restock_quantity), quantityText, { quantityText = it }, number = true)
        Input(stringResource(R.string.restock_total_paid), totalText, { totalText = it }, number = true)
        unitPrice?.let {
            Text(
                stringResource(R.string.restock_unit_price, RupiahFormatter.format(it)),
                fontWeight = FontWeight.SemiBold,
            )
        }
        Input(stringResource(R.string.screen_copy_041), note, { note = it })
        Primary(
            stringResource(R.string.screen_copy_075),
            { save(productId, supplierId, quantity, total, note) },
            !saving && productId.isNotBlank() && supplierId.isNotBlank() && quantity > 0 && total >= 0,
        )
        Secondary(stringResource(R.string.screen_copy_043), back)
    }
}
