package com.aura.app.ui.screens.keuangan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.aura.app.ui.components.Secondary
import com.aura.app.ui.components.money
import com.aura.app.ui.components.date
import com.aura.app.ui.keuangan.KeuanganUiState

@Composable
internal fun ExpenseListScreen(
    uiState: KeuanganUiState,
    onAdd: () -> Unit,
    onDetail: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    varFilterExpenseScreen(uiState, onAdd, onDetail, onRefresh, modifier)
}

@Composable
private fun varFilterExpenseScreen(
    uiState: KeuanganUiState,
    onAdd: () -> Unit,
    onDetail: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier,
) {
    val selectedFilter = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("SEMUA") }
    val records = uiState.pengeluaran.filter { record ->
        selectedFilter.value == "SEMUA" ||
            (selectedFilter.value == "PEMBELIAN" && record.jenis == JenisPengeluaran.PEMBELIAN) ||
            (selectedFilter.value == "KUSTOM" && record.jenis == JenisPengeluaran.KUSTOM)
    }
    val total = records.filter { it.status == StatusCatatanKeuangan.TERCATAT }.sumOf { it.nominal }

    Page(modifier = modifier) {
        Heading(stringResource(R.string.finance_expense_title), stringResource(R.string.finance_expense_subtitle))
        Primary(stringResource(R.string.finance_expense_add), onAdd)
        Spacer(Modifier.height(10.dp))
        Panel {
            Text(stringResource(R.string.finance_total_expense), fontSize = 11.sp)
            Text(money(total), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "SEMUA" to R.string.finance_filter_all,
                "PEMBELIAN" to R.string.finance_type_purchase,
                "KUSTOM" to R.string.finance_type_custom,
            ).forEach { (value, label) ->
                FilterChip(
                    selected = selectedFilter.value == value,
                    onClick = { selectedFilter.value = value },
                    label = { Text(stringResource(label)) },
                )
            }
        }
        uiState.errorMessage?.let { message ->
            Panel {
                Text(message)
                Secondary(stringResource(R.string.action_retry), onRefresh)
            }
        }
        if (uiState.isLoading && uiState.pengeluaran.isEmpty()) {
            CircularProgressIndicator()
        } else if (records.isEmpty()) {
            Panel { Text(stringResource(R.string.finance_expense_empty)) }
        } else {
            records.forEach { expense ->
                Panel {
                    androidx.compose.foundation.layout.Column(
                        Modifier.fillMaxWidth().clickable { onDetail(expense.id) },
                    ) {
                        Text(expense.judul, fontWeight = FontWeight.Bold)
                        Text(
                            "${date(expense.tanggal)} • ${stringResource(if (expense.jenis == JenisPengeluaran.PEMBELIAN) R.string.finance_type_purchase else R.string.finance_type_custom)}",
                            fontSize = 11.sp,
                        )
                        Text(
                            if (expense.status == StatusCatatanKeuangan.DIBATALKAN) {
                                stringResource(R.string.finance_status_cancelled)
                            } else {
                                money(expense.nominal)
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
