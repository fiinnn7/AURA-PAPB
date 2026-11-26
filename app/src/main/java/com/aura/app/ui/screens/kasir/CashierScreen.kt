package com.aura.app.ui.screens.kasir

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.core.format.RupiahFormatter
import com.aura.app.core.model.StatusStok
import com.aura.app.data.local.entity.BarangEntity
import com.aura.app.ui.barang.BarangTampilan
import com.aura.app.ui.components.ServerFileImage
import com.aura.app.ui.kasir.BarisKeranjang
import com.aura.app.ui.theme.Bg
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Red
import com.aura.app.ui.theme.Soft
import kotlinx.coroutines.delay

@Composable
internal fun CashierScreen(
    modifier: Modifier = Modifier,
    items: List<BarangTampilan>,
    search: String,
    setSearch: (String) -> Unit,
    fileBytes: Map<String, ByteArray>,
    failedFileIds: Set<String>,
    loadFile: (String) -> Unit,
    add: (BarangEntity) -> Unit,
    cart: () -> Unit,
    history: () -> Unit,
    avatar: () -> Unit,
    cartItems: List<BarisKeranjang> = emptyList(),
    onChangeQty: ((String, Int) -> Unit)? = null,
) {
    var category by rememberSaveable { mutableStateOf("Semua") }
    val view = LocalView.current

    val fallbackQuantities = remember { mutableStateMapOf<String, Int>() }

    val activeQuantities: Map<String, Int> = remember(cartItems, onChangeQty, fallbackQuantities.toMap()) {
        if (onChangeQty != null) {
            cartItems.associate { it.barangId to it.jumlah }
        } else {
            fallbackQuantities.toMap()
        }
    }

    var announcementText by remember { mutableStateOf<String?>(null) }
    var announcementTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(announcementTick) {
        if (announcementTick > 0) {
            delay(1500)
            announcementText = null
        }
    }

    fun applyQuantityChange(barang: BarangEntity, newQty: Int, showBanner: Boolean) {
        val clamped = newQty.coerceIn(0, barang.stokSaatIni)
        val oldQty = activeQuantities[barang.id] ?: 0
        val delta = clamped - oldQty
        if (delta == 0) return

        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

        if (onChangeQty != null) {
            if (oldQty == 0 && clamped > 0) {
                add(barang)
                if (clamped > 1) {
                    onChangeQty(barang.id, clamped - 1)
                }
            } else {
                onChangeQty(barang.id, delta)
            }
        } else {
            if (clamped == 0) fallbackQuantities.remove(barang.id)
            else fallbackQuantities[barang.id] = clamped
            if (delta > 0) repeat(delta) { add(barang) }
        }

        if (showBanner && delta > 0) {
            announcementText = "Barang berhasil ditambahkan (${barang.nama} • ${clamped}x)"
            announcementTick++
        }
    }

    val categories = items.flatMap { it.kategori }.distinct().sorted()
    val visible = items.filter { item ->
        val barang = item.barang
        (barang.nama.contains(search, true) || barang.sku.orEmpty().contains(search, true)) &&
                (category == "Semua" || category in item.kategori)
    }

    Box(modifier = modifier.fillMaxSize().background(Bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
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
                        val currentQty = activeQuantities[barang.id] ?: 0
                        val remainingStock = (barang.stokSaatIni - currentQty).coerceAtLeast(0)
                        val habis = item.statusStok == StatusStok.HABIS || barang.stokSaatIni <= 0

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (habis) Soft else Color.White)
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
                                .clickable(enabled = !habis && currentQty == 0) {
                                    applyQuantityChange(barang, 1, showBanner = true)
                                }
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
                            Text(barang.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(item.kategori.joinToString(" • ").ifBlank { barang.satuan }, color = Muted, fontSize = 10.sp)
                            Text(RupiahFormatter.format(barang.hargaJual), color = Red, fontWeight = FontWeight.Bold)
                            Text(
                                if (habis) stringResource(R.string.katalog_status_empty)
                                else stringResource(R.string.kasir_stock_count, remainingStock, barang.satuan),
                                color = if (remainingStock > 0 && !habis) Muted else Red,
                                fontSize = 11.sp,
                            )

                            if (currentQty == 0) {
                                Button(
                                    onClick = { applyQuantityChange(barang, 1, showBanner = true) },
                                    enabled = !habis,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                                ) {
                                    Text(if (habis) stringResource(R.string.katalog_status_empty) else stringResource(R.string.screen_copy_095))
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    QuantityStepper(
                                        quantity = currentQty,
                                        maxStock = barang.stokSaatIni,
                                        onQuantityChange = { updated ->
                                            applyQuantityChange(
                                                barang = barang,
                                                newQty = updated,
                                                showBanner = updated > currentQty
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAF7F5))
            ) {
                HorizontalDivider(color = Color(0xFFE6E0DC), thickness = 1.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = cart,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                    ) {
                        Text(stringResource(R.string.screen_copy_101))
                    }
                    TextButton(onClick = history, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.screen_copy_102))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = announcementText != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 116.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1F2937))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = announcementText.orEmpty(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
internal fun QuantityStepper(
    quantity: Int,
    maxStock: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(quantity) { mutableStateOf(quantity.toString()) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(1.dp, Red.copy(alpha = 0.55f), CircleShape)
                .clickable { onQuantityChange(quantity - 1) }
        ) {
            Text(
                text = "−",
                color = Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        BasicTextField(
            value = textValue,
            onValueChange = { raw ->
                val digitsOnly = raw.filter { it.isDigit() }
                if (digitsOnly.isEmpty()) {
                    textValue = ""
                } else {
                    val parsed = digitsOnly.toIntOrNull()?.coerceAtMost(maxStock) ?: quantity
                    textValue = parsed.toString()
                    onQuantityChange(parsed)
                }
            },
            singleLine = true,
            cursorBrush = SolidColor(Red),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center
            ),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .widthIn(min = 34.dp, max = 52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF6F6F6))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    if (textValue.isEmpty()) {
                        Text(
                            text = "0",
                            fontSize = 13.sp,
                            color = Muted,
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
            }
        )

        val canIncrement = quantity < maxStock
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(
                    1.dp,
                    if (canIncrement) Red.copy(alpha = 0.55f) else Color(0xFFD0D0D0),
                    CircleShape
                )
                .clickable(enabled = canIncrement) { onQuantityChange(quantity + 1) }
        ) {
            Text(
                text = "+",
                color = if (canIncrement) Red else Color(0xFFBDBDBD),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}