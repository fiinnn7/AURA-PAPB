package com.aura.app.ui.screens.kasir

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.core.format.RupiahFormatter
import com.aura.app.core.model.StatusStok
import com.aura.app.data.local.entity.BarangEntity
import com.aura.app.ui.barang.BarangTampilan
import com.aura.app.ui.components.AuraRedButton
import com.aura.app.ui.components.ServerFileImage
import com.aura.app.ui.theme.Bg
import com.aura.app.ui.theme.DisabledRed
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.PressedRed
import com.aura.app.ui.theme.PressedSoft
import com.aura.app.ui.theme.Red
import com.aura.app.ui.theme.Soft

@Composable
internal fun CashierScreen(
    modifier: Modifier = Modifier,
    items: List<BarangTampilan>,
    cartQuantities: Map<String, Int>,
    search: String,
    setSearch: (String) -> Unit,
    fileBytes: Map<String, ByteArray>,
    failedFileIds: Set<String>,
    loadFile: (String) -> Unit,
    add: (BarangEntity) -> Unit,
    decrement: (BarangEntity) -> Unit,
    cart: () -> Unit,
    history: () -> Unit,
    avatar: () -> Unit,
) {
    var category by rememberSaveable { mutableStateOf("Semua") }
    val categories = items.flatMap { it.kategori }.distinct().sorted()
    val visible = items.filter { item ->
        val barang = item.barang
        (barang.nama.contains(search, true) || barang.sku.orEmpty().contains(search, true)) &&
            (category == "Semua" || category in item.kategori)
    }
    val totalCartItems = cartQuantities.values.sum()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().background(Bg).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.kasir_catalog_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.kasir_catalog_subtitle), color = Muted, fontSize = 12.sp)
                }
                TextButton(onClick = avatar) { Text(stringResource(R.string.screen_copy_100), color = Red) }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = search,
                onValueChange = setSearch,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.katalog_search)) },
                singleLine = true,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (listOf("Semua") + categories).forEach { value ->
                    FilterChip(
                        selected = category == value,
                        onClick = { category = value },
                        label = { Text(value) },
                    )
                }
            }
        }
        if (visible.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.kasir_catalog_empty),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                    color = Muted,
                )
            }
        } else {
            items(visible, key = { it.barang.id }) { item ->
                val barang = item.barang
                val maxStock = barang.stokSaatIni.coerceAtLeast(0)
                val habis = item.statusStok == StatusStok.HABIS || maxStock <= 0
                val qtyInCart = (cartQuantities[barang.id] ?: 0).coerceIn(0, maxStock)

                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(if (habis) Soft else Color.White)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    ServerFileImage(
                        fileId = barang.fotoFileId,
                        bytes = barang.fotoFileId?.let(fileBytes::get),
                        gagalDimuat = barang.fotoFileId?.let(failedFileIds::contains) == true,
                        onLoad = loadFile,
                        modifier = Modifier.fillMaxWidth().size(92.dp).clip(RoundedCornerShape(12.dp)),
                        contentDescription = barang.nama,
                    )
                    Text(barang.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    Text(item.kategori.joinToString(" • ").ifBlank { barang.satuan }, color = Muted, fontSize = 10.sp, maxLines = 1)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(RupiahFormatter.format(barang.hargaJual), color = Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                if (habis) stringResource(R.string.katalog_status_empty)
                                else stringResource(R.string.kasir_stock_count, maxStock, barang.satuan),
                                color = if (habis) Red else Muted,
                                fontSize = 11.sp,
                            )
                        }
                        if (!habis) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (qtyInCart > 0) PressedSoft else Soft)
                                    .border(
                                        width = 1.dp,
                                        color = if (qtyInCart > 0) Red else Color(0xFFE0D5D3),
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = qtyInCart.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (qtyInCart > 0) Red else Muted,
                                )
                            }
                        }
                    }

                    if (habis) {
                        AuraRedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(stringResource(R.string.katalog_status_empty), fontSize = 12.sp)
                        }
                    } else {
                        CashierStepperPill(
                            canDecrement = qtyInCart > 0,
                            canIncrement = qtyInCart < maxStock,
                            onDecrement = { if (qtyInCart > 0) decrement(barang) },
                            onIncrement = { if (qtyInCart < maxStock) add(barang) },
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                AuraRedButton(
                    onClick = cart,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50),
                ) {
                    val baseLabel = stringResource(R.string.screen_copy_101)
                    Text(
                        text = if (totalCartItems > 0) "$baseLabel ($totalCartItems)" else baseLabel,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(onClick = history, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.screen_copy_102))
                }
            }
        }
    }
}

@Composable
private fun CashierStepperPill(
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val decInteraction = remember { MutableInteractionSource() }
    val incInteraction = remember { MutableInteractionSource() }
    val decPressed by decInteraction.collectIsPressedAsState()
    val incPressed by incInteraction.collectIsPressedAsState()

    val decBg by animateColorAsState(
        targetValue = when {
            !canDecrement -> Color(0xFF6B2A2A)
            decPressed -> PressedRed
            else -> Red
        },
        animationSpec = tween(70),
        label = "decBg",
    )
    val incBg by animateColorAsState(
        targetValue = when {
            !canIncrement -> DisabledRed
            incPressed -> PressedRed
            else -> Red
        },
        animationSpec = tween(70),
        label = "incBg",
    )
    val decScale by animateFloatAsState(if (decPressed && canDecrement) 0.94f else 1f, tween(70), label = "decScale")
    val incScale by animateFloatAsState(if (incPressed && canIncrement) 0.94f else 1f, tween(70), label = "incScale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(50)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(decBg)
                .clickable(
                    interactionSource = decInteraction,
                    indication = null,
                    enabled = canDecrement,
                    onClick = onDecrement,
                )
                .graphicsLayer { scaleX = decScale; scaleY = decScale },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "−",
                color = if (canDecrement) Color.White else Color.White.copy(alpha = 0.45f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.30f)),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(incBg)
                .clickable(
                    interactionSource = incInteraction,
                    indication = null,
                    enabled = canIncrement,
                    onClick = onIncrement,
                )
                .graphicsLayer { scaleX = incScale; scaleY = incScale },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
