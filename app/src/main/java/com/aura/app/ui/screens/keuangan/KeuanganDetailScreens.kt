package com.aura.app.ui.screens.keuangan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.core.model.JenisPengeluaran
import com.aura.app.core.model.StatusCatatanKeuangan
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.Panel
import com.aura.app.ui.components.Primary
import com.aura.app.ui.components.ServerFileImage
import com.aura.app.ui.components.date
import com.aura.app.ui.components.money
import com.aura.app.ui.keuangan.KeuanganUiState

@Composable
internal fun ExpenseDetailScreen(
    uiState: KeuanganUiState,
    onCancel: (String, String) -> Unit,
    onLoadFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expense = uiState.detailPengeluaran
    var showCancelDialog by rememberSaveable(expense?.id) { mutableStateOf(false) }
    var reason by rememberSaveable(expense?.id) { mutableStateOf("") }

    Page(modifier = modifier) {
        Heading(stringResource(R.string.finance_expense_detail), expense?.judul.orEmpty())
        uiState.errorMessage?.let { Panel { Text(it) } }
        when {
            uiState.isLoadingDetail -> CircularProgressIndicator()
            expense == null -> Panel { Text(stringResource(R.string.finance_detail_missing)) }
            else -> {
                Panel {
                    Text(money(expense.nominal), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(date(expense.tanggal))
                    Text(
                        stringResource(
                            if (expense.jenis == JenisPengeluaran.PEMBELIAN) {
                                R.string.finance_type_purchase
                            } else {
                                R.string.finance_type_custom
                            },
                        ),
                    )
                    Text(
                        stringResource(
                            if (expense.status == StatusCatatanKeuangan.DIBATALKAN) {
                                R.string.finance_status_cancelled
                            } else {
                                R.string.finance_status_recorded
                            },
                        ),
                    )
                    if (expense.jenis == JenisPengeluaran.PEMBELIAN) {
                        val product = uiState.barang.firstOrNull { it.id == expense.barangId }
                        val supplier = uiState.supplier.firstOrNull { it.id == expense.supplierId }
                        DetailRow(stringResource(R.string.finance_product_label), product?.nama ?: expense.barangId.orEmpty())
                        DetailRow(stringResource(R.string.finance_quantity_label), expense.jumlah?.toString().orEmpty())
                        DetailRow(stringResource(R.string.finance_unit_price_label), money(expense.hargaPerItem ?: 0L))
                        DetailRow(stringResource(R.string.finance_supplier_label), supplier?.nama ?: expense.supplierId.orEmpty())
                    }
                    expense.catatan?.takeIf(String::isNotBlank)?.let {
                        DetailRow(stringResource(R.string.finance_note_label), it)
                    }
                    DetailRow(stringResource(R.string.finance_recorded_by), expense.dibuatOleh.orEmpty())
                    expense.alasanBatal?.let {
                        DetailRow(stringResource(R.string.finance_cancel_reason), it)
                    }
                }
                if (expense.buktiFileId != null) {
                    ServerFileImage(
                        fileId = expense.buktiFileId,
                        bytes = uiState.fileBytes[expense.buktiFileId],
                        gagalDimuat = expense.buktiFileId in uiState.fileIdsGagalDimuat,
                        onLoad = onLoadFile,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentDescription = stringResource(R.string.finance_evidence),
                    )
                }
                if (
                    expense.jenis == JenisPengeluaran.KUSTOM &&
                    expense.status == StatusCatatanKeuangan.TERCATAT
                ) {
                    Primary(stringResource(R.string.finance_cancel_record), { showCancelDialog = true })
                }
            }
        }
    }

    if (showCancelDialog && expense != null) {
        CancelReasonDialog(
            reason = reason,
            onReasonChange = { reason = it },
            onDismiss = { showCancelDialog = false },
            onConfirm = {
                onCancel(expense.id, reason)
                showCancelDialog = false
                reason = ""
            },
        )
    }
}

@Composable
internal fun IncomeDetailScreen(
    uiState: KeuanganUiState,
    onCancel: (String, String, String) -> Unit,
    onLoadFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val income = uiState.detailPemasukan
    var showCancelDialog by rememberSaveable(income?.id) { mutableStateOf(false) }
    var reason by rememberSaveable(income?.id) { mutableStateOf("") }

    Page(modifier = modifier) {
        Heading(stringResource(R.string.finance_income_detail), income?.judul.orEmpty())
        uiState.errorMessage?.let { Panel { Text(it) } }
        when {
            uiState.isLoadingDetail -> CircularProgressIndicator()
            income == null -> Panel { Text(stringResource(R.string.finance_detail_missing)) }
            else -> {
                Panel {
                    Text(money(income.nominal), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(date(income.tanggal))
                    Text(
                        stringResource(
                            if (income.jenis == "PENJUALAN") R.string.finance_type_sale else R.string.finance_type_custom,
                        ),
                    )
                    Text(income.status)
                    if (income.jenis == "PENJUALAN") {
                        DetailRow(stringResource(R.string.finance_invoice_label), income.nomorTransaksi.orEmpty())
                        DetailRow(stringResource(R.string.finance_payment_method), income.metodePembayaran.orEmpty())
                        DetailRow(
                            stringResource(R.string.finance_cashier_label),
                            uiState.detailTransaksi?.transaksi?.namaAkun ?: income.namaAkun.orEmpty(),
                        )
                        uiState.detailTransaksi?.rincianBarang.orEmpty().forEach { item ->
                            DetailRow(
                                item.namaBarang,
                                "${item.jumlah} ${item.satuan} × ${money(item.hargaJual)}",
                            )
                        }
                    }
                    income.catatan?.takeIf(String::isNotBlank)?.let {
                        DetailRow(stringResource(R.string.finance_note_label), it)
                    }
                    income.alasanPembatalan?.let {
                        DetailRow(stringResource(R.string.finance_cancel_reason), it)
                    }
                }
                if (income.buktiFileId != null) {
                    ServerFileImage(
                        fileId = income.buktiFileId,
                        bytes = uiState.fileBytes[income.buktiFileId],
                        gagalDimuat = income.buktiFileId in uiState.fileIdsGagalDimuat,
                        onLoad = onLoadFile,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentDescription = stringResource(R.string.finance_evidence),
                    )
                }
                if (income.status == "TERCATAT" || income.status == "SELESAI") {
                    Primary(stringResource(R.string.finance_cancel_record), { showCancelDialog = true })
                }
            }
        }
    }

    if (showCancelDialog && income != null) {
        CancelReasonDialog(
            reason = reason,
            onReasonChange = { reason = it },
            onDismiss = { showCancelDialog = false },
            onConfirm = {
                onCancel(income.id, income.jenis, reason)
                showCancelDialog = false
                reason = ""
            },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 12.sp)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CancelReasonDialog(
    reason: String,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.finance_cancel_title)) },
        text = {
            Column {
                Text(stringResource(R.string.finance_cancel_prompt))
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text(stringResource(R.string.finance_cancel_reason)) },
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = reason.isNotBlank()) {
                Text(stringResource(R.string.finance_cancel_record))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
