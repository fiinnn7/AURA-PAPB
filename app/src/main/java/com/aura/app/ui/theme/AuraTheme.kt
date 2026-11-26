package com.aura.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aura.app.core.model.StatusStok

@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Red,
            onPrimary = Color.White,
            secondary = DarkRed,
            onSecondary = Color.White,
            tertiary = Green,
            background = Bg,
            onBackground = Ink,
            surface = Color.White,
            onSurface = Ink,
            surfaceVariant = Soft,
            onSurfaceVariant = Muted,
            outline = Border,
            error = Red,
            onError = Color.White,
        ),
        content = content,
    )
}

object StatusColors {
    fun forStatus(status: StatusStok): Color = when (status) {
        StatusStok.AMAN -> Green
        StatusStok.MENIPIS, StatusStok.HABIS, StatusStok.DIARSIPKAN -> Red
    }
}
