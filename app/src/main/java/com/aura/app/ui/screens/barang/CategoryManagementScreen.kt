package com.aura.app.ui.screens.barang

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.ui.barang.KategoriTampilan
import com.aura.app.ui.theme.Bg
import com.aura.app.ui.theme.Red
import com.aura.app.ui.theme.Soft

@Composable
internal fun CategoryManagementScreen(
    modifier: Modifier = Modifier,
    categories: List<KategoriTampilan>,
    save: (String?, String) -> Unit,
    delete: (String) -> Unit,
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var deleteTarget by rememberSaveable { mutableStateOf<KategoriTampilan?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Bg).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                stringResource(R.string.katalog_manage_categories),
                Modifier.padding(top = 18.dp),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Soft).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.katalog_category_name)) },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            save(editingId, name.trim())
                            editingId = null
                            name = ""
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                    ) {
                        Text(if (editingId == null) stringResource(R.string.katalog_add_category) else stringResource(R.string.screen_copy_083))
                    }
                    if (editingId != null) {
                        TextButton(onClick = { editingId = null; name = "" }) {
                            Text(stringResource(R.string.screen_copy_043))
                        }
                    }
                }
            }
        }
        items(categories, key = { it.kategori.id }) { item ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(androidx.compose.ui.graphics.Color.White).padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.kategori.nama, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.katalog_category_product_count, item.jumlahBarang),
                        color = androidx.compose.ui.graphics.Color(0xFF757575),
                    )
                }
                TextButton(onClick = { editingId = item.kategori.id; name = item.kategori.nama }) {
                    Text(stringResource(R.string.screen_copy_083), color = Red)
                }
                TextButton(onClick = { deleteTarget = item }) {
                    Text(stringResource(R.string.screen_copy_084), color = Red)
                }
            }
        }
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.katalog_delete_category_title)) },
            text = { Text(stringResource(R.string.katalog_delete_category_confirmation, item.jumlahBarang)) },
            confirmButton = {
                TextButton(onClick = { delete(item.kategori.id); deleteTarget = null }) {
                    Text(stringResource(R.string.screen_copy_084), color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.screen_copy_043)) }
            },
        )
    }
}
