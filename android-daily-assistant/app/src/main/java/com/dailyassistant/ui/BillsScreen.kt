@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dailyassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.dailyassistant.data.Bill
import com.dailyassistant.data.Budget
import com.dailyassistant.util.Dates
import com.dailyassistant.util.Money
import com.dailyassistant.vm.MainViewModel

@Composable
fun BillsScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    val bills by vm.bills.collectAsState()
    val categories by vm.categories.collectAsState()
    val currency = settings.currency

    var editing by remember { mutableStateOf<Bill?>(null) }
    var showNew by remember { mutableStateOf(false) }

    val today = Dates.today()
    val monthlyTotal = bills.filter { it.active }.sumOf { it.amountMinor }
    val unpaid = bills.filter { it.active && !Budget.isPaidThisCycle(it, today) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Monthly bills",
                    value = Money.format(monthlyTotal, currency),
                    modifier = Modifier.weight(1f),
                    accent = Tones.info,
                    caption = "${bills.count { it.active }} active"
                )
                StatTile(
                    label = "Still to pay",
                    value = Money.format(unpaid.sumOf { it.amountMinor }, currency),
                    modifier = Modifier.weight(1f),
                    accent = if (unpaid.isEmpty()) Tones.good else Tones.warn,
                    caption = if (unpaid.isEmpty()) "All paid 🎉" else "${unpaid.size} left"
                )
            }
        }

        item {
            Button(onClick = { showNew = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("  Add a bill or subscription")
            }
        }

        if (bills.isEmpty()) {
            item {
                EmptyState(
                    emoji = "📄",
                    title = "No bills yet",
                    text = "Add rent, internet, phone or any subscription. The app reminds you before each one is due and subtracts them from your spendable money."
                )
            }
        }

        items(items = bills, key = { it.id }) { bill ->
            val dueDate = Budget.nextDueDate(bill, today)
            val paid = Budget.isPaidThisCycle(bill, today)
            val daysLeft = Dates.daysUntil(today, dueDate)
            SectionCard {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RoundIcon(
                            Icons.Default.Receipt,
                            tint = when {
                                paid -> Tones.good
                                daysLeft <= 2 -> Tones.bad
                                else -> Tones.info
                            }
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                bill.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Day ${bill.dueDayOfMonth} each month · next " + Dates.shortDate(dueDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            bill.categoryId?.let { id ->
                                categories.firstOrNull { it.id == id }?.let { category ->
                                    Text(
                                        category.emoji + " " + category.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                Money.format(bill.amountMinor, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (paid) {
                                Pill("Paid", Tones.good)
                            } else {
                                Pill(
                                    when {
                                        daysLeft < 0 -> "Late"
                                        daysLeft == 0L -> "Today"
                                        daysLeft == 1L -> "Tomorrow"
                                        else -> "in ${daysLeft}d"
                                    },
                                    if (daysLeft <= 2) Tones.bad else Tones.warn
                                )
                            }
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!paid) {
                            Button(onClick = { vm.payBill(bill) }, modifier = Modifier.weight(1f)) {
                                Text("Mark paid")
                            }
                        }
                        OutlinedButton(onClick = { editing = bill }, modifier = Modifier.weight(1f)) {
                            Text("Edit")
                        }
                        IconButton(onClick = { vm.deleteBill(bill) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete bill")
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showNew) {
        BillDialog(
            bill = Bill(name = "", amountMinor = 0, dueDayOfMonth = 1),
            currency = currency,
            categories = categories.map { it.id to (it.emoji + " " + it.name) },
            onSave = { vm.saveBill(it) },
            onDismiss = { showNew = false }
        )
    }

    editing?.let { bill ->
        BillDialog(
            bill = bill,
            currency = currency,
            categories = categories.map { it.id to (it.emoji + " " + it.name) },
            onSave = { vm.saveBill(it) },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun BillDialog(
    bill: Bill,
    currency: String,
    categories: List<Pair<Long, String>>,
    onSave: (Bill) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(bill.name) }
    var amountText by remember { mutableStateOf(if (bill.amountMinor > 0) Money.plain(bill.amountMinor) else "") }
    var dueDay by remember { mutableStateOf(bill.dueDayOfMonth.toString()) }
    var remindBefore by remember { mutableStateOf(bill.remindDaysBefore.toString()) }
    var categoryId by remember { mutableStateOf(bill.categoryId) }
    var active by remember { mutableStateOf(bill.active) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (bill.id == 0L) "New bill" else "Edit bill") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (rent, internet, gym...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AmountField(value = amountText, onValueChange = { amountText = it }, currency = currency)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dueDay,
                        onValueChange = { dueDay = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Due day") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = remindBefore,
                        onValueChange = { remindBefore = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Warn days") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { (id, label) ->
                                val selected = categoryId == id
                                OutlinedButton(
                                    onClick = { categoryId = if (selected) null else id },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        (if (selected) "✓ " else "") + label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                LabeledSwitch(label = "Active", checked = active, onCheckedChange = { active = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minor = Money.parse(amountText) ?: 0L
                if (name.isNotBlank() && minor > 0) {
                    onSave(
                        bill.copy(
                            name = name.trim(),
                            amountMinor = minor,
                            dueDayOfMonth = (dueDay.toIntOrNull() ?: 1).coerceIn(1, 31),
                            remindDaysBefore = (remindBefore.toIntOrNull() ?: 2).coerceIn(0, 30),
                            categoryId = categoryId,
                            active = active
                        )
                    )
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
