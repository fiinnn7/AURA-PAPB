package com.aura.app.ui.screens.kasir

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.core.model.MetodePembayaran
import com.aura.app.core.model.StatusTransaksi
import com.aura.app.data.local.entity.TransaksiPenjualanEntity
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Panel
import com.aura.app.ui.components.date
import com.aura.app.ui.components.money
import com.aura.app.ui.theme.Bg
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Red

@Composable
internal fun HistoryScreen(
    items: List<TransaksiPenjualanEntity>,
    isLoading: Boolean,
    errorMessage: String?,
    retry: () -> Unit,
    openCancel: (TransaksiPenjualanEntity) -> Unit,
    isCashier: Boolean,
    submittedCancellationIds: Set<String>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(Bg).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Heading(
                stringResource(R.string.screen_copy_102),
                stringResource(R.string.kasir_history_today_subtitle),
            )
        }
        if (errorMessage != null) {
            item {
                Panel {
                    Column {
                        Text(errorMessage, color = Red)
                        TextButton(onClick = retry) {
                            Text(stringResource(R.string.kasir_retry))
                        }
                    }
                }
            }
        }
        if (isLoading && items.isEmpty()) {
            item { CircularProgressIndicator(color = Red) }
        } else if (items.isEmpty() && errorMessage == null) {
            item { Text(stringResource(R.string.screen_copy_104), color = Muted) }
        }
        items(items, key = { it.id }) { transaksi ->
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(transaksi.nomorTransaksi, fontWeight = FontWeight.Bold)
                    Text(
                        "${date(transaksi.dibuatPada)} • ${transaksi.kasirNama}",
                        fontSize = 10.sp,
                        color = Muted,
                    )
                    Text(
                        "${labelMetode(transaksi.metodePembayaran)} • ${labelStatus(transaksi.status)}",
                        fontSize = 11.sp,
                        color = if (transaksi.status == StatusTransaksi.SELESAI) Muted else Red,
                    )
                    Text(money(transaksi.total), fontWeight = FontWeight.Bold, color = Red)
                    if (transaksi.status == StatusTransaksi.SELESAI) {
                        if (isCashier && transaksi.id in submittedCancellationIds) {
                            Text(
                                stringResource(R.string.kasir_cancel_request_submitted),
                                modifier = Modifier.padding(top = 8.dp),
                                color = Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Text(
                                stringResource(
                                    if (isCashier) R.string.kasir_request_cancel else R.string.kasir_cancel_transaction,
                                ),
                                modifier = Modifier.clickable { openCancel(transaksi) }.padding(top = 8.dp),
                                color = Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (transaksi.status == StatusTransaksi.DIBATALKAN && transaksi.alasanBatal != null) {
                        Text(
                            stringResource(R.string.kasir_cancel_reason, transaksi.alasanBatal),
                            fontSize = 10.sp,
                            color = Muted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun labelMetode(metode: MetodePembayaran): String = stringResource(
    when (metode) {
        MetodePembayaran.TUNAI -> R.string.kasir_method_cash
        MetodePembayaran.TRANSFER -> R.string.kasir_method_transfer
        MetodePembayaran.QRIS -> R.string.kasir_method_qris
    },
)

@Composable
private fun labelStatus(status: StatusTransaksi): String = stringResource(
    when (status) {
        StatusTransaksi.SELESAI -> R.string.kasir_status_completed
        StatusTransaksi.DIBATALKAN -> R.string.kasir_status_cancelled
        StatusTransaksi.DIRETUR -> R.string.kasir_status_returned
    },
)
