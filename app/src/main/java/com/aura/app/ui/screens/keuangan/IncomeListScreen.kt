package com.aura.app.ui.screens.keuangan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.Panel
import com.aura.app.ui.components.Primary
import com.aura.app.ui.components.Secondary
import com.aura.app.ui.components.date
import com.aura.app.ui.components.money
import com.aura.app.ui.keuangan.KeuanganUiState

@Composable
internal fun IncomeListScreen(
    uiState: KeuanganUiState,
    onAdd: () -> Unit,
    onDetail: (String, String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by remember { mutableStateOf("SEMUA") }
    val records = uiState.pemasukan.filter { selectedFilter == "SEMUA" || it.jenis == selectedFilter }
    val total = records.filter { it.status != "DIBATALKAN" && it.status != "DIRETUR" }.sumOf { it.nominal }

    Page(modifier = modifier) {
        Heading(stringResource(R.string.finance_income_title), stringResource(R.string.finance_income_subtitle))
        Primary(stringResource(R.string.finance_income_add), onAdd)
        Spacer(Modifier.height(10.dp))
        Panel {
            Text(stringResource(R.string.finance_total_income), fontSize = 11.sp)
            Text(money(total), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            listOf(
                "SEMUA" to R.string.finance_filter_all,
                "PENJUALAN" to R.string.finance_type_sale,
                "KUSTOM" to R.string.finance_type_custom,
            ).forEach { (value, label) ->
                androidx.compose.material3.FilterChip(
                    selected = selectedFilter == value,
                    onClick = { selectedFilter = value },
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
        if (uiState.isLoading && uiState.pemasukan.isEmpty()) {
            CircularProgressIndicator()
        } else if (records.isEmpty()) {
            Panel { Text(stringResource(R.string.finance_income_empty)) }
        } else {
            records.forEach { income ->
                Panel {
                    Column(
                        Modifier.fillMaxWidth().clickable { onDetail(income.id, income.jenis) },
                    ) {
                        Text(
                            if (income.jenis == "PENJUALAN") {
                                stringResource(R.string.finance_sale_number, income.judul)
                            } else {
                                income.judul
                            },
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${date(income.tanggal)} • ${stringResource(if (income.jenis == "PENJUALAN") R.string.finance_type_sale else R.string.finance_type_custom)}",
                            fontSize = 11.sp,
                        )
                        Text(
                            if (income.status == "DIBATALKAN") {
                                stringResource(R.string.finance_status_cancelled)
                            } else {
                                money(income.nominal)
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
