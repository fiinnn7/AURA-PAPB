package com.aura.app.ui.screens.barang

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.core.format.TanggalFormatter
import com.aura.app.data.local.entity.PergerakanStokEntity
import com.aura.app.ui.components.Heading
import com.aura.app.ui.components.Panel
import com.aura.app.ui.theme.Bg
import com.aura.app.ui.theme.Green
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Red

@Composable
internal fun MovementScreen(
    modifier: Modifier = Modifier,
    items: List<PergerakanStokEntity>,
    nextCursor: String?,
    loading: Boolean,
    loadingMore: Boolean,
    error: String?,
    retry: () -> Unit,
    loadMore: () -> Unit,
) {
    var selectedType by rememberSaveable { mutableStateOf("SEMUA") }
    var selectedPeriod by rememberSaveable { mutableStateOf("SEMUA") }
    val since = when (selectedPeriod) {
        "7_HARI" -> System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        "30_HARI" -> System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        else -> null
    }
    val visible = items.filter {
        (selectedType == "SEMUA" || it.jenis.name == selectedType) &&
            (since == null || it.dibuatPada >= since)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(Bg).padding(horizontal = 18.dp),
        contentPadding = PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Heading(stringResource(R.string.screen_copy_044), stringResource(R.string.stock_movement_append_only))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "SEMUA" to R.string.stock_filter_all,
                    "MASUK" to R.string.stock_filter_in,
                    "KELUAR" to R.string.stock_filter_out,
                    "PENYESUAIAN" to R.string.stock_filter_adjustment,
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "SEMUA" to R.string.stock_period_all,
                    "7_HARI" to R.string.stock_period_week,
                    "30_HARI" to R.string.stock_period_month,
                ).forEach { (period, label) ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        }
        if (error != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Color.White).padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(error, color = Red)
                    TextButton(onClick = retry) { Text(stringResource(R.string.katalog_retry)) }
                }
            }
        }
        if (loading && visible.isEmpty()) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier.padding(28.dp).size(32.dp),
                    color = Red,
                )
            }
        } else if (error == null && visible.isEmpty()) {
            item { Text(stringResource(R.string.screen_copy_045), color = Muted) }
        } else {
            items(visible, key = { it.id }) { movement ->
                val difference = movement.stokSesudah - movement.stokSebelum
                Panel {
                    Text(
                        "${movement.jenis.name} • ${movement.sumber.name.replace('_', ' ')}",
                        fontWeight = FontWeight.Bold,
                    )
                    Text(movement.namaBarang, fontSize = 12.sp)
                    Text(
                        "${if (difference > 0) "+" else ""}$difference • ${movement.stokSebelum} → ${movement.stokSesudah}",
                        color = if (difference < 0) Red else Green,
                        fontWeight = FontWeight.Bold,
                    )
                    movement.alasan?.let { Text(stringResource(R.string.adjustment_reason_label, it.name.replace('_', ' '))) }
                    Text(
                        listOfNotNull(movement.namaAkun, TanggalFormatter.format(movement.dibuatPada))
                            .joinToString(" • "),
                        fontSize = 10.sp,
                        color = Muted,
                    )
                }
            }
        }
        if (nextCursor != null) {
            item {
                TextButton(onClick = loadMore, enabled = !loadingMore, modifier = Modifier.fillMaxWidth()) {
                    if (loadingMore) CircularProgressIndicator()
                    else Text(stringResource(R.string.stock_load_more))
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
