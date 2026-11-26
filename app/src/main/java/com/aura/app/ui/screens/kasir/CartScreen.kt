package com.aura.app.ui.screens.kasir

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Panel
import com.aura.app.ui.components.Primary
import com.aura.app.ui.components.Secondary
import com.aura.app.ui.components.money
import com.aura.app.ui.kasir.BarisKeranjang
import com.aura.app.ui.theme.Bg
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Red

@Composable
internal fun CartScreen(
    cart: List<BarisKeranjang>,
    change: (String, Int) -> Unit,
    pay: () -> Unit,
    back: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtotal = cart.sumOf { it.hargaJual * it.jumlah }
    val jumlahItem = cart.sumOf { it.jumlah }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Bg).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Heading(
                stringResource(R.string.kasir_cart_title),
                stringResource(R.string.kasir_cart_subtitle, jumlahItem),
            )
        }
        if (cart.isEmpty()) {
            item { Text(stringResource(R.string.kasir_cart_empty), color = Muted) }
        }
        items(cart, key = { it.barangId }) { item ->
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.namaBarang, fontWeight = FontWeight.Bold)
                    Text(
                        "${money(item.hargaJual)} / ${item.satuan}",
                        fontSize = 11.sp,
                        color = Muted,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val canPlus = item.jumlah < item.stokSaatDipilih
                        Text(
                            stringResource(R.string.screen_copy_094),
                            modifier = Modifier.clickable(enabled = item.jumlah > 0) { change(item.barangId, -1) }.padding(8.dp),
                            fontSize = 20.sp,
                            color = if (item.jumlah > 0) Red else Muted,
                        )
                        Text(item.jumlah.toString(), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.screen_copy_095),
                            modifier = Modifier.clickable(enabled = canPlus) { change(item.barangId, 1) }.padding(8.dp),
                            fontSize = 20.sp,
                            color = if (canPlus) Red else Muted,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            money(item.hargaJual * item.jumlah),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        item {
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow(stringResource(R.string.kasir_summary_subtotal), money(subtotal))
                    SummaryRow(stringResource(R.string.kasir_summary_items), jumlahItem.toString())
                    SummaryRow(
                        stringResource(R.string.kasir_summary_total),
                        money(subtotal),
                        emphasize = true,
                    )
                }
            }
        }
        item {
            Primary(
                stringResource(R.string.screen_copy_097),
                pay,
                cart.isNotEmpty(),
            )
        }
        item { Secondary(stringResource(R.string.screen_copy_098), back) }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = FontWeight.Bold, color = if (emphasize) Red else androidx.compose.ui.graphics.Color.Unspecified)
    }
}
