package com.aura.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.ui.theme.Bg
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Red

@Composable
fun AuraBottomBar(
    currentRoute: String,
    onSelect: (route: String, isRoot: Boolean) -> Unit,
) {
    val selectedColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Red,
        selectedTextColor = Red,
        unselectedIconColor = Muted,
        unselectedTextColor = Muted,
        indicatorColor = Color(0xFFFFEEEE),
    )
    NavigationBar(
        containerColor = Color.White,
        contentColor = Red,
        tonalElevation = 5.dp,
    ) {
        NavigationBarItem(
            selected = currentRoute == Routes.DASHBOARD,
            onClick = { onSelect(Routes.DASHBOARD, true) },
            icon = { Text("⌂", fontSize = 21.sp) },
            label = { Text("Dashboard", fontSize = 9.sp, fontWeight = FontWeight.Medium) },
            colors = selectedColors,
        )
        NavigationBarItem(
            selected = currentRoute == Routes.MENU_MANAJEMEN || currentRoute == Routes.MANAJEMEN_BARANG,
            onClick = { onSelect(Routes.MENU_MANAJEMEN, true) },
            icon = { Text("▦", fontSize = 19.sp) },
            label = { Text("Manajemen", fontSize = 9.sp, fontWeight = FontWeight.Medium) },
            colors = selectedColors,
        )
        NavigationBarItem(
            selected = currentRoute in setOf(Routes.K1_BERANDA_KASIR, Routes.K2_CATAT_TRANSAKSI, Routes.K3_PEMBAYARAN),
            onClick = { onSelect(Routes.K1_BERANDA_KASIR, false) },
            icon = {
                Text(
                    "+",
                    fontSize = 23.sp,
                    color = Color.White,
                    modifier = Modifier.clip(CircleShape).background(Red).padding(horizontal = 12.dp, vertical = 1.dp),
                )
            },
            label = { Text("Kasir", fontSize = 9.sp, fontWeight = FontWeight.Medium) },
            colors = selectedColors,
        )
        NavigationBarItem(
            selected = currentRoute == Routes.ANALISIS,
            onClick = { onSelect(Routes.ANALISIS, true) },
            icon = { Text("▥", fontSize = 19.sp) },
            label = { Text("Analisis", fontSize = 9.sp, fontWeight = FontWeight.Medium) },
            colors = selectedColors,
        )
        NavigationBarItem(
            selected = currentRoute == Routes.PROFIL,
            onClick = { onSelect(Routes.PROFIL, true) },
            icon = { Text("●", fontSize = 18.sp) },
            label = { Text("Profil", fontSize = 9.sp, fontWeight = FontWeight.Medium) },
            colors = selectedColors,
        )
    }
}
