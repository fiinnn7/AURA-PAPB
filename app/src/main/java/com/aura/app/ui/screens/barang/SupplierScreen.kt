package com.aura.app.ui.screens.barang

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.app.R
import com.aura.app.core.model.StatusAkun
import com.aura.app.data.local.entity.SupplierEntity
import com.aura.app.ui.theme.Bg
import com.aura.app.ui.theme.Muted
import com.aura.app.ui.theme.Red
import com.aura.app.ui.theme.Soft

@Composable
internal fun SupplierScreen(
    modifier: Modifier = Modifier,
    items: List<SupplierEntity>,
    save: (String?, String, String?, String?) -> Unit,
    delete: (String) -> Unit,
    setStatus: (String, String) -> Unit,
    detail: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var link by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Bg).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                stringResource(R.string.katalog_manage_suppliers),
                Modifier.padding(top = 18.dp),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = {
                    editing = null
                    showForm = true
                    name = ""
                    link = ""
                    note = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = Red),
            ) {
                Text(stringResource(R.string.katalog_add_supplier))
            }
        }
        if (showForm) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Soft).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        name,
                        { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.katalog_supplier_name)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        link,
                        { link = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.katalog_supplier_link)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        note,
                        { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.screen_copy_041)) },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                save(editing, name.trim(), link.trim().takeIf(String::isNotEmpty), note.trim().takeIf(String::isNotEmpty))
                                showForm = false
                            },
                            enabled = name.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Red),
                        ) {
                            Text(stringResource(R.string.screen_copy_081))
                        }
                        TextButton(onClick = { showForm = false }) {
                            Text(stringResource(R.string.screen_copy_043))
                        }
                    }
                }
            }
        }
        if (items.isEmpty()) {
            item { Text(stringResource(R.string.katalog_supplier_empty), color = Muted, modifier = Modifier.padding(vertical = 24.dp)) }
        } else {
            items(items, key = { it.id }) { supplier ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(androidx.compose.ui.graphics.Color.White)
                        .clickable { detail(supplier.id) }.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(supplier.nama, fontWeight = FontWeight.Bold)
                    Text(
                        if (supplier.status == StatusAkun.AKTIF) stringResource(R.string.katalog_supplier_active)
                        else stringResource(R.string.katalog_supplier_inactive),
                        color = Muted,
                        fontSize = 12.sp,
                    )
                    Text(supplier.link ?: stringResource(R.string.katalog_supplier_no_link), color = Muted, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!supplier.link.isNullOrBlank()) {
                            TextButton(onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(supplier.link))
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    onMessage(context.getString(R.string.katalog_link_unavailable))
                                }
                            }) { Text(stringResource(R.string.katalog_contact_supplier), color = Red) }
                        }
                        TextButton(onClick = {
                            editing = supplier.id
                            showForm = true
                            name = supplier.nama
                            link = supplier.link.orEmpty()
                            note = supplier.catatan.orEmpty()
                        }) { Text(stringResource(R.string.screen_copy_083), color = Red) }
                        TextButton(
                            onClick = {
                                setStatus(
                                    supplier.id,
                                    if (supplier.status == StatusAkun.AKTIF) "NONAKTIF" else "AKTIF",
                                )
                            },
                        ) {
                            Text(
                                if (supplier.status == StatusAkun.AKTIF) stringResource(R.string.katalog_deactivate)
                                else stringResource(R.string.katalog_activate),
                                color = Red,
                            )
                        }
                        TextButton(onClick = { deleteId = supplier.id }) {
                            Text(stringResource(R.string.screen_copy_084), color = Red)
                        }
                    }
                }
            }
        }
    }

    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text(stringResource(R.string.katalog_delete_supplier_title)) },
            text = { Text(stringResource(R.string.katalog_delete_supplier_confirmation)) },
            confirmButton = {
                TextButton(onClick = { delete(id); deleteId = null }) {
                    Text(stringResource(R.string.screen_copy_084), color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteId = null }) { Text(stringResource(R.string.screen_copy_043)) }
            },
        )
    }
}
