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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import com.aura.app.data.CartLineData
import com.aura.app.data.ProductEntity
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.Primary
import com.aura.app.ui.components.Secondary
import com.aura.app.ui.components.money
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Purple
import com.aura.app.ui.theme.Red
import kotlinx.coroutines.delay

@Composable
internal fun CashierScreen(
    items: List<ProductEntity>,
    search: String,
    setSearch: (String) -> Unit,
    add: (ProductEntity) -> Unit,
    cart: () -> Unit,
    history: () -> Unit,
    avatar: () -> Unit,
    modifier: Modifier = Modifier,
    cartItems: List<CartLineData> = emptyList(),
    onChangeQty: ((String, Int) -> Unit)? = null
) {
    var category by rememberSaveable { mutableStateOf("Semua") }
    val view = LocalView.current

    val fallbackQuantities = remember { mutableStateMapOf<String, Int>() }

    val activeQuantities: Map<String, Int> = remember(cartItems, onChangeQty, fallbackQuantities.toMap()) {
        if (onChangeQty != null) {
            cartItems.associate { it.productId to it.quantity }
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

    fun applyQuantityChange(product: ProductEntity, newQty: Int, showBanner: Boolean) {
        val clamped = newQty.coerceIn(0, product.stock)
        val oldQty = activeQuantities[product.id] ?: 0
        val delta = clamped - oldQty
        if (delta == 0) return

        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

        if (onChangeQty != null) {
            if (oldQty == 0 && clamped > 0) {
                // Item belum ada di keranjang -> buat entry baru via addToCart dulu
                add(product)
                if (clamped > 1) {
                    onChangeQty(product.id, clamped - 1)
                }
            } else {
                // Item sudah ada di keranjang -> kirim selisih (+ atau -) lewat changeCart
                onChangeQty(product.id, delta)
            }
        } else {
            if (clamped == 0) fallbackQuantities.remove(product.id)
            else fallbackQuantities[product.id] = clamped
            if (delta > 0) repeat(delta) { add(product) }
        }

        if (showBanner && delta > 0) {
            announcementText = "Barang berhasil ditambahkan (${product.name} • ${clamped}x)"
            announcementTick++
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Page(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        Heading(
                            stringResource(R.string.screen_copy_099),
                            "Pilih barang untuk transaksi"
                        )
                    }
                    Text(
                        stringResource(R.string.screen_copy_100),
                        Modifier
                            .clickable { avatar() }
                            .padding(8.dp),
                        fontSize = 23.sp,
                        color = Purple
                    )
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = setSearch,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.screen_copy_067)) },
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Semua", "Makanan", "Sembako", "Minuman").forEach {
                        FilterChip(
                            selected = category == it,
                            onClick = { category = it },
                            label = { Text(it, fontSize = 10.sp) }
                        )
                    }
                }

                items
                    .filter {
                        (it.name.contains(search, true) || it.sku.contains(search, true)) &&
                                (category == "Semua" || it.categories.split(",")
                                    .any { c -> c.trim().equals(category, true) })
                    }
                    .forEach { product ->
                        val currentQty = activeQuantities[product.id] ?: 0
                        val remainingStock = (product.stock - currentQty).coerceAtLeast(0)

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    product.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${product.sku} • Stok $remainingStock",
                                    fontSize = 10.sp,
                                    color = if (remainingStock > 0) Muted else Red
                                )
                                Text(
                                    money(product.sellPrice),
                                    color = Red,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (currentQty == 0) {
                                Button(
                                    onClick = { applyQuantityChange(product, 1, showBanner = true) },
                                    enabled = product.stock > 0,
                                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                                ) {
                                    Text(stringResource(R.string.screen_copy_095))
                                }
                            } else {
                                QuantityStepper(
                                    quantity = currentQty,
                                    maxStock = product.stock,
                                    onQuantityChange = { updated: Int ->
                                        applyQuantityChange(
                                            product = product,
                                            newQty = updated,
                                            showBanner = updated > currentQty
                                        )
                                    }
                                )
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Primary(stringResource(R.string.screen_copy_101), cart)
                    Secondary(stringResource(R.string.screen_copy_102), history)
                }
            }
        }

        AnimatedVisibility(
            visible = announcementText != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 120.dp)
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