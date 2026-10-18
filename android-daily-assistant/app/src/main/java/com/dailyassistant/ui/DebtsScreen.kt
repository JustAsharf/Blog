@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dailyassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dailyassistant.data.Debt
import com.dailyassistant.util.Dates
import com.dailyassistant.util.Money
import com.dailyassistant.vm.MainViewModel

@Composable
fun DebtsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val debts by vm.debts.collectAsState()
    val currency = settings.currency

    var showNew by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Debt?>(null) }

    val open = debts.filter { !it.settled }
    val iOwe = open.filter { it.iOwe }.sumOf { it.amountMinor }
    val owedToMe = open.filter { !it.iOwe }.sumOf { it.amountMinor }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Who owes who") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "I owe",
                        value = Money.format(iOwe, currency),
                        modifier = Modifier.weight(1f),
                        accent = Tones.bad
                    )
                    StatTile(
                        label = "Owed to me",
                        value = Money.format(owedToMe, currency),
                        modifier = Modifier.weight(1f),
                        accent = Tones.good
                    )
                }
            }

            item {
                Button(onClick = { showNew = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("  Add a debt")
                }
            }

            if (debts.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🤝",
                        title = "Nothing tracked",
                        text = "Lent a friend some cash? Borrowed for the week? Write it down here before everyone forgets."
                    )
                }
            }

            items(items = debts, key = { it.id }) { debt ->
                SectionCard {
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RoundIcon(Icons.Default.People, tint = if (debt.iOwe) Tones.bad else Tones.good)
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    debt.person,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = if (debt.settled) TextDecoration.LineThrough else null
                                )
                                Text(
                                    (if (debt.iOwe) "You owe" else "Owes you") +
                                        (debt.dueEpochDay?.let { " · by " + Dates.shortDate(Dates.of(it)) } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (debt.note.isNotBlank()) {
                                    Text(
                                        debt.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                Money.format(debt.amountMinor, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = { vm.settleDebt(debt) }, modifier = Modifier.weight(1f)) {
                                Text(if (debt.settled) "Reopen" else "Settled")
                            }
                            OutlinedButton(onClick = { editing = debt }, modifier = Modifier.weight(1f)) {
                                Text("Edit")
                            }
                            IconButton(onClick = { vm.deleteDebt(debt) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (showNew) {
        DebtDialog(
            debt = Debt(person = "", amountMinor = 0, iOwe = true),
            currency = currency,
            onSave = { vm.saveDebt(it) },
            onDismiss = { showNew = false }
        )
    }

    editing?.let { debt ->
        DebtDialog(
            debt = debt,
            currency = currency,
            onSave = { vm.saveDebt(it) },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun DebtDialog(
    debt: Debt,
    currency: String,
    onSave: (Debt) -> Unit,
    onDismiss: () -> Unit
) {
    var person by remember { mutableStateOf(debt.person) }
    var amountText by remember { mutableStateOf(if (debt.amountMinor > 0) Money.plain(debt.amountMinor) else "") }
    var iOwe by remember { mutableStateOf(debt.iOwe) }
    var note by remember { mutableStateOf(debt.note) }
    var due by remember { mutableStateOf(debt.dueEpochDay?.let { Dates.of(it) }) }

    val openDatePicker = rememberDatePicker(due ?: Dates.today().plusWeeks(2)) { due = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt.id == 0L) "New debt" else "Edit debt") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text("Person") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AmountField(value = amountText, onValueChange = { amountText = it }, currency = currency)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = iOwe,
                        onClick = { iOwe = true },
                        label = { Text("I owe them") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !iOwe,
                        onClick = { iOwe = false },
                        label = { Text("They owe me") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = openDatePicker, modifier = Modifier.fillMaxWidth()) {
                    Text(due?.let { "Due " + Dates.shortDate(it) } ?: "Pick a due date (optional)")
                }
                if (due != null) {
                    TextButton(onClick = { due = null }) { Text("Clear due date") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minor = Money.parse(amountText) ?: 0L
                if (person.isNotBlank() && minor > 0) {
                    onSave(
                        debt.copy(
                            person = person.trim(),
                            amountMinor = minor,
                            iOwe = iOwe,
                            note = note.trim(),
                            dueEpochDay = due?.toEpochDay()
                        )
                    )
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
