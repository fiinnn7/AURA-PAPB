package com.aura.app.ui.screens.dashboard

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.data.CashRecordEntity
import com.aura.app.data.ProductEntity
import com.aura.app.data.TransactionEntity
import com.aura.app.data.local.entity.TokoEntity
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Metric
import com.aura.app.ui.components.Page
import com.aura.app.ui.components.Panel
import com.aura.app.ui.components.money
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Purple
import com.aura.app.ui.theme.Red

private enum class StockAlertType {
    LOW_STOCK,
    OUT_OF_STOCK,
}

private data class QuickAccessItem(
    val title: String,
    val subtitle: String,
    val route: String,
    val badgeSymbol: String,
    val accentColor: Color,
    val badgeBg: Color,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardScreen(
    store: TokoEntity?,
    products: List<ProductEntity>,
    tx: List<TransactionEntity>,
    records: List<CashRecordEntity>,
    hide: Boolean,
    toggle: () -> Unit,
    go: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRestockProduct: ((String) -> Unit)? = null,
) {
    val view = LocalView.current
    val today = tx.filter { it.status == "Selesai" }
    val expenses = records.filter { it.kind == "expense" && !it.isPrive }.sumOf { it.amount }

    val lowStockProducts = remember(products) {
        products.filter { it.stock in 1..it.minStock }.sortedBy { it.stock }
    }
    val outOfStockProducts = remember(products) {
        products.filter { it.stock == 0 }.sortedBy { it.name }
    }

    var activeAlertType by remember { mutableStateOf<StockAlertType?>(null) }

    val quickAccessItems = remember {
        listOf(
            QuickAccessItem(
                title = "Manajemen Barang",
                subtitle = "Katalog & harga",
                route = "manajemen_barang",
                badgeSymbol = "▤",
                accentColor = Purple,
                badgeBg = Color(0xFFF3E8FF),
            ),
            QuickAccessItem(
                title = "Catat Restock",
                subtitle = "Tambah stok masuk",
                route = "catat_restock",
                badgeSymbol = "+",
                accentColor = Color(0xFF059669),
                badgeBg = Color(0xFFECFDF5),
            ),
            QuickAccessItem(
                title = "Pengeluaran",
                subtitle = "Biaya operasional",
                route = "pengeluaran",
                badgeSymbol = "↗",
                accentColor = Red,
                badgeBg = Color(0xFFFEF2F2),
            ),
            QuickAccessItem(
                title = "Pemasukan",
                subtitle = "Kas masuk lainnya",
                route = "pemasukan",
                badgeSymbol = "↙",
                accentColor = Color(0xFF0284C7),
                badgeBg = Color(0xFFF0F9FF),
            ),
            QuickAccessItem(
                title = "Riwayat Hari Ini",
                subtitle = "Transaksi kasir",
                route = "riwayat_hari_ini_owner",
                badgeSymbol = "◷",
                accentColor = Color(0xFFD97706),
                badgeBg = Color(0xFFFFFBEB),
            ),
            QuickAccessItem(
                title = "Notifikasi",
                subtitle = "Info & peringatan",
                route = "notifikasi",
                badgeSymbol = "●",
                accentColor = Color(0xFF4F46E5),
                badgeBg = Color(0xFFEEF2FF),
            ),
        )
    }

    Page(modifier = modifier) {
        Text(stringResource(R.string.screen_copy_085), fontSize = 12.sp, color = Muted)
        Heading(
            store?.namaToko ?: "Ringkasan Toko",
            "${store?.jenisUsaha ?: "Retail"} • ${store?.idToko ?: ""}",
        )

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.screen_copy_086), fontSize = 11.sp, color = Muted)
                    Text(
                        if (hide) "Rp ••••••••" else money(store?.saldoKumulatif ?: 0),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Red,
                    )
                }
                Text(
                    if (hide) "◉" else "◎",
                    Modifier
                        .clickable { toggle() }
                        .padding(8.dp),
                    fontSize = 22.sp,
                    color = Red,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                Metric(
                    stringResource(R.string.screen_copy_087),
                    money(today.sumOf { it.total }),
                    "${today.size} transaksi",
                )
            }
            Box(Modifier.weight(1f)) {
                Metric(stringResource(R.string.screen_copy_088), money(expenses))
            }
        }

        // --- SECTION: PERLU PERHATIAN (URGENCY & CLICKABLE) ---
        Heading(stringResource(R.string.screen_copy_089))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StockAlertCard(
                title = stringResource(R.string.screen_copy_090),
                count = lowStockProducts.size,
                actionHint = if (lowStockProducts.isNotEmpty()) "Lihat barang ›" else "Stok aman",
                containerColor = if (lowStockProducts.isNotEmpty()) Color(0xFFFFFBEB) else Color.White,
                borderColor = if (lowStockProducts.isNotEmpty()) Color(0xFFF59E0B) else Color(0xFFE5E7EB),
                accentColor = if (lowStockProducts.isNotEmpty()) Color(0xFFD97706) else Muted,
                badgeText = if (lowStockProducts.isNotEmpty()) "MENIPIS" else "AMAN",
                modifier = Modifier.weight(1f),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    activeAlertType = StockAlertType.LOW_STOCK
                },
            )

            StockAlertCard(
                title = stringResource(R.string.screen_copy_091),
                count = outOfStockProducts.size,
                actionHint = if (outOfStockProducts.isNotEmpty()) "Segera isi ›" else "Tidak ada",
                containerColor = if (outOfStockProducts.isNotEmpty()) Color(0xFFFEF2F2) else Color.White,
                borderColor = if (outOfStockProducts.isNotEmpty()) Red else Color(0xFFE5E7EB),
                accentColor = if (outOfStockProducts.isNotEmpty()) Red else Muted,
                badgeText = if (outOfStockProducts.isNotEmpty()) "KRITIS" else "AMAN",
                modifier = Modifier.weight(1f),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    activeAlertType = StockAlertType.OUT_OF_STOCK
                },
            )
        }

        // --- SECTION: AKSES CEPAT (2x3 CARD GRID) ---
        Spacer(modifier = Modifier.height(16.dp))
        Heading(stringResource(R.string.screen_copy_092))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            quickAccessItems.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowItems.forEach { item ->
                        QuickAccessCard(
                            item = item,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                go(item.route)
                            },
                        )
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEET: SHOWCASE BARANG STOK MENIPIS / HABIS ---
    activeAlertType?.let { alertType ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val isOut = alertType == StockAlertType.OUT_OF_STOCK
        val targetProducts = if (isOut) outOfStockProducts else lowStockProducts
        val sheetTitle = if (isOut) "Daftar Stok Habis" else "Daftar Stok Menipis"
        val sheetSubtitle = if (isOut) {
            "Barang berikut sudah 0 dan tidak bisa dijual di kasir."
        } else {
            "Barang berikut mendekati batas minimum stok."
        }
        val highlightColor = if (isOut) Red else Color(0xFFD97706)
        val highlightBg = if (isOut) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)

        ModalBottomSheet(
            onDismissRequest = { activeAlertType = null },
            sheetState = sheetState,
            containerColor = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(highlightBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "${targetProducts.size} Barang",
                            color = highlightColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = sheetTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(text = sheetSubtitle, fontSize = 12.sp, color = Muted)
                HorizontalDivider(color = Color(0xFFF3F4F6))

                if (targetProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF9FAFB))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isOut) "Tidak ada barang yang habis saat ini." else "Tidak ada barang dengan stok menipis.",
                            color = Muted,
                            fontSize = 13.sp,
                        )
                    }
                } else {
                    targetProducts.forEach { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFAFAFA))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${product.sku} • Sisa ${product.stock} (Min. ${product.minStock})",
                                    fontSize = 11.sp,
                                    color = highlightColor,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Button(
                                onClick = {
                                    activeAlertType = null
                                    if (onRestockProduct != null) {
                                        onRestockProduct(product.id)
                                    } else {
                                        go("catat_restock")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Restock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockAlertCard(
    title: String,
    count: Int,
    actionHint: String,
    containerColor: Color,
    borderColor: Color,
    accentColor: Color,
    badgeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.14f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = badgeText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                )
            }
        }

        Text(
            text = count.toString(),
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor,
        )

        Text(
            text = actionHint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
        )
    }
}

@Composable
private fun QuickAccessCard(
    item: QuickAccessItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(item.badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.badgeSymbol,
                    color = item.accentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "›",
                color = Muted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                fontSize = 11.sp,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}