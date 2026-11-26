package com.aura.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.aura.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ServerFileImage(
    fileId: String?,
    bytes: ByteArray?,
    gagalDimuat: Boolean,
    onLoad: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    LaunchedEffect(fileId, bytes, gagalDimuat) {
        if (fileId != null && bytes == null && !gagalDimuat) onLoad(fileId)
    }
    val bitmap = produceState<android.graphics.Bitmap?>(initialValue = null, bytes) {
        value = bytes?.let { raw ->
            withContext(Dispatchers.IO) { BitmapFactory.decodeByteArray(raw, 0, raw.size) }
        }
    }.value

    Box(modifier.background(Color(0xFFF3F4F6)), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(stringResource(R.string.katalog_foto_placeholder), color = Color(0xFF757575))
        }
    }
}
