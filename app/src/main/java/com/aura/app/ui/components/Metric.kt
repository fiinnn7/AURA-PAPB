package com.aura.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aura.app.ui.theme.Green
import com.aura.app.ui.theme.Ink
import com.aura.app.ui.theme.Muted

@Composable
internal fun Metric(label: String, value: String, detail: String? = null) {
    Panel {
        Text(label, fontSize = 11.sp, color = Muted)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        if (detail != null) Text(detail, fontSize = 10.sp, color = Green)
    }
}
