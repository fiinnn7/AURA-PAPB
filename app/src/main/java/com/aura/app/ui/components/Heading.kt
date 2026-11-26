package com.aura.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.aura.app.ui.theme.Ink
import com.aura.app.ui.theme.Muted

@Composable
internal fun Heading(title: String, subtitle: String? = null) {
    Column(Modifier.padding(top = 4.dp, bottom = 12.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        if (subtitle != null) {
            Text(subtitle, fontSize = 12.sp, color = Muted)
        }
    }
}
