package com.aura.app.ui.screens.kasir

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.core.model.MetodePembayaran
import com.aura.app.data.remote.dto.MetodePembayaranTokoDto
import com.aura.app.data.remote.dto.TransaksiBuatRequestDto
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Input
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.Panel
import com.aura.app.ui.components.Primary
import com.aura.app.ui.components.Secondary
import com.aura.app.ui.components.ServerFileImage
import com.aura.app.ui.components.money
import com.aura.app.ui.theme.Green
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Red

@Composable
internal fun PaymentScreen(
    total: Long,
    metodePembayaran: MetodePembayaranTokoDto?,
    isLoadingMetode: Boolean,
    metodeError: String?,
    retryMetode: () -> Unit,
    qrisBytes: ByteArray?,
    qrisLoading: Boolean,
    qrisError: String?,
    loadQris: (String) -> Unit,
    retryQris: () -> Unit,
    isSaving: Boolean,
    requestTertunda: TransaksiBuatRequestDto?,
    checkoutError: String?,
    checkout: (MetodePembayaran, Long, Boolean) -> Unit,
    copyInfoRekening: (String) -> Unit,
    back: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var methodName by rememberSaveable { mutableStateOf(MetodePembayaran.TUNAI.name) }
    var received by rememberSaveable(total) { mutableStateOf(total.toString()) }
    var paymentConfirmed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(requestTertunda) {
        requestTertunda?.let { request ->
            methodName = request.metodePembayaran
            received = (request.uangDiterima ?: total).toString()
            paymentConfirmed = request.pembayaranDiterima == true
        }
    }

    val method = if (requestTertunda != null) {
        MetodePembayaran.valueOf(requestTertunda.metodePembayaran)
    } else {
        MetodePembayaran.valueOf(methodName)
    }
    val receivedAmount = received.toLongOrNull() ?: 0L
    val finalReceivedAmount = requestTertunda?.uangDiterima ?: receivedAmount
    val transferInfo = metodePembayaran?.infoRekening?.takeIf(String::isNotBlank)
    val transferReady = transferInfo != null && !isLoadingMetode && metodeError == null
    val qrisReady = metodePembayaran?.qrisFileId?.isNotBlank() == true &&
        qrisBytes != null && !qrisLoading && qrisError == null

    Page(modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Heading(
                stringResource(R.string.screen_copy_105),
                stringResource(R.string.kasir_payment_subtitle),
            )
            Panel {
                Text(stringResource(R.string.screen_copy_106), color = Muted, fontSize = 12.sp)
                Text(money(total), fontWeight = FontWeight.ExtraBold, color = Red, fontSize = 22.sp)
            }
            Text(stringResource(R.string.screen_copy_107), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            listOf(
                MetodePembayaran.TUNAI to R.string.kasir_method_cash,
                MetodePembayaran.TRANSFER to R.string.kasir_method_transfer,
                MetodePembayaran.QRIS to R.string.kasir_method_qris,
            ).forEach { (value, label) ->
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable(enabled = requestTertunda == null && !isSaving) {
                            methodName = value.name
                            paymentConfirmed = false
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = method == value,
                        enabled = requestTertunda == null && !isSaving,
                        onClick = {
                            methodName = value.name
                            paymentConfirmed = false
                        },
                    )
                    Text(stringResource(label), fontWeight = FontWeight.SemiBold)
                }
            }

            if (isLoadingMetode) {
                Text(stringResource(R.string.kasir_payment_settings_loading), color = Muted)
            }
            if (metodeError != null) {
                Panel {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(metodeError, color = Red)
                        TextButton(onClick = retryMetode) {
                            Text(stringResource(R.string.kasir_retry))
                        }
                    }
                }
            }

            when (method) {
                MetodePembayaran.TUNAI -> {
                    if (requestTertunda == null) {
                        Input(
                            stringResource(R.string.screen_copy_108),
                            received,
                            { received = it },
                            number = true,
                        )
                    } else {
                        Panel {
                            Text(
                                stringResource(
                                    R.string.kasir_pending_cash_received,
                                    money(finalReceivedAmount),
                                ),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Text(
                        stringResource(
                            R.string.kasir_change_format,
                            money((finalReceivedAmount - total).coerceAtLeast(0)),
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Green,
                    )
                }
                MetodePembayaran.TRANSFER -> {
                    val recipient = metodePembayaran?.namaPenerima?.takeIf(String::isNotBlank)
                    Panel {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.kasir_transfer_info_title), fontWeight = FontWeight.Bold)
                            if (recipient != null) Text(recipient)
                            if (transferInfo != null) {
                                Text(transferInfo)
                                TextButton(
                                    onClick = { copyInfoRekening(transferInfo) },
                                    enabled = requestTertunda == null && !isSaving,
                                ) {
                                    Text(stringResource(R.string.kasir_copy_account), color = Red)
                                }
                            } else {
                                Text(stringResource(R.string.kasir_transfer_info_missing), color = Muted)
                            }
                        }
                    }
                    ConfirmPaymentButton(
                        confirmed = paymentConfirmed,
                        disabled = isSaving || requestTertunda != null || !transferReady,
                    ) { paymentConfirmed = true }
                }
                MetodePembayaran.QRIS -> {
                    val fileId = metodePembayaran?.qrisFileId?.takeIf(String::isNotBlank)
                    Panel {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.kasir_qris_title), fontWeight = FontWeight.Bold)
                            if (fileId == null) {
                                Text(stringResource(R.string.kasir_qris_missing), color = Muted)
                            } else {
                                Text(stringResource(R.string.kasir_qris_reminder), color = Muted)
                                ServerFileImage(
                                    fileId = fileId,
                                    bytes = qrisBytes,
                                    gagalDimuat = qrisError != null,
                                    onLoad = loadQris,
                                    modifier = Modifier.fillMaxWidth().size(240.dp),
                                    contentDescription = stringResource(R.string.kasir_qris_title),
                                )
                                if (qrisLoading) Text(stringResource(R.string.kasir_qris_loading), color = Muted)
                                if (qrisError != null) {
                                    Text(qrisError, color = Red)
                                    TextButton(onClick = retryQris) {
                                        Text(stringResource(R.string.kasir_retry))
                                    }
                                }
                            }
                        }
                    }
                    ConfirmPaymentButton(
                        confirmed = paymentConfirmed,
                        disabled = isSaving || requestTertunda != null || !qrisReady,
                    ) { paymentConfirmed = true }
                }
            }

            if (checkoutError != null) {
                Panel { Text(checkoutError, color = Red) }
            }
            val canFinish = when (method) {
                MetodePembayaran.TUNAI -> finalReceivedAmount >= total
                MetodePembayaran.TRANSFER ->
                    paymentConfirmed && (requestTertunda != null || transferReady)
                MetodePembayaran.QRIS ->
                    paymentConfirmed && (requestTertunda != null || qrisReady)
            }
            Primary(
                stringResource(R.string.kasir_finish_transaction),
                { checkout(method, finalReceivedAmount, paymentConfirmed) },
                enabled = canFinish && !isSaving,
            )
            Secondary(stringResource(R.string.screen_copy_026), back)
        }
    }
}

@Composable
private fun ConfirmPaymentButton(
    confirmed: Boolean,
    disabled: Boolean,
    onConfirm: () -> Unit,
) {
    TextButton(onClick = onConfirm, enabled = !confirmed && !disabled) {
        Text(
            stringResource(
                if (confirmed) R.string.kasir_payment_received else R.string.kasir_confirm_payment_received,
            ),
            color = if (confirmed) Green else Red,
        )
    }
}
