@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dailyassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyassistant.data.Category
import com.dailyassistant.data.Entry
import com.dailyassistant.util.Dates
import com.dailyassistant.util.Money

@Composable
fun EntryEditorDialog(
    entry: Entry,
    categories: List<Category>,
    currency: String,
    onSave: (Entry) -> Unit,
    onDelete: (Entry) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(Money.plain(entry.amountMinor)) }
    var note by remember { mutableStateOf(entry.note) }
    var categoryId by remember { mutableStateOf(entry.categoryId) }
    var date by remember { mutableStateOf(Dates.of(entry.epochDay)) }

    val openDatePicker = rememberDatePicker(date) { date = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry.isIncome) "Edit income" else "Edit expense") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AmountField(value = amountText, onValueChange = { amountText = it }, currency = currency)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = openDatePicker) {
                    Text(Dates.dayLabel(date) + " · " + Dates.shortDate(date))
                }
                if (!entry.isIncome) {
                    CategoryPicker(categories = categories, selectedId = categoryId, onSelect = { categoryId = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minor = Money.parse(amountText)
                if (minor != null && minor > 0) {
                    onSave(
                        entry.copy(
                            amountMinor = minor,
                            note = note.trim(),
                            categoryId = categoryId,
                            epochDay = date.toEpochDay()
                        )
                    )
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { onDelete(entry); onDismiss() }) { Text("Delete") }
        }
    )
}
