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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.dailyassistant.data.Goal
import com.dailyassistant.util.Dates
import com.dailyassistant.util.Money
import com.dailyassistant.vm.MainViewModel

@Composable
fun GoalsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val goals by vm.goals.collectAsState()
    val currency = settings.currency

    var showNew by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Goal?>(null) }
    var contributing by remember { mutableStateOf<Goal?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Savings goals") },
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
                Button(onClick = { showNew = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("  New goal")
                }
            }

            if (goals.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🎯",
                        title = "No goals yet",
                        text = "A phone, a trip, an emergency fund. Set a target and the app tells you how much to put aside each day."
                    )
                }
            }

            items(items = goals, key = { it.id }) { goal ->
                val fraction = if (goal.targetMinor > 0) {
                    (goal.savedMinor.toDouble() / goal.targetMinor).toFloat()
                } else 0f
                val left = (goal.targetMinor - goal.savedMinor).coerceAtLeast(0)
                SectionCard {
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(goal.emoji, style = MaterialTheme.typography.headlineSmall)
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                            ) {
                                Text(
                                    goal.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    Money.format(goal.savedMinor, currency) + " of " +
                                        Money.format(goal.targetMinor, currency),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Pill(
                                if (goal.targetMinor > 0) "${(fraction * 100).toInt()}%" else "0%",
                                if (fraction >= 1f) Tones.good else Tones.info
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        ProgressBlock(
                            label = if (left == 0L) "Reached 🎉" else Money.format(left, currency) + " to go",
                            fraction = fraction,
                            caption = goal.deadlineEpochDay?.let { "by " + Dates.shortDate(Dates.of(it)) } ?: "no deadline",
                            color = if (fraction >= 1f) Tones.good else MaterialTheme.colorScheme.primary
                        )
                        goal.deadlineEpochDay?.let { deadline ->
                            val daysLeft = Dates.daysUntil(Dates.today(), Dates.of(deadline))
                            if (daysLeft > 0 && left > 0) {
                                Text(
                                    "Put aside " + Money.format(left / daysLeft, currency) + " a day to make it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { contributing = goal }, modifier = Modifier.weight(1f)) {
                                Text("Add money")
                            }
                            OutlinedButton(onClick = { editing = goal }, modifier = Modifier.weight(1f)) {
                                Text("Edit")
                            }
                            IconButton(onClick = { vm.deleteGoal(goal) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete goal")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (showNew) {
        GoalDialog(
            goal = Goal(name = "", targetMinor = 0),
            currency = currency,
            onSave = { vm.saveGoal(it) },
            onDismiss = { showNew = false }
        )
    }

    editing?.let { goal ->
        GoalDialog(
            goal = goal,
            currency = currency,
            onSave = { vm.saveGoal(it) },
            onDismiss = { editing = null }
        )
    }

    contributing?.let { goal ->
        ContributeDialog(
            goal = goal,
            currency = currency,
            onConfirm = { amount -> vm.contribute(goal, amount) },
            onDismiss = { contributing = null }
        )
    }
}

@Composable
private fun GoalDialog(
    goal: Goal,
    currency: String,
    onSave: (Goal) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(goal.name) }
    var emoji by remember { mutableStateOf(goal.emoji) }
    var targetText by remember { mutableStateOf(if (goal.targetMinor > 0) Money.plain(goal.targetMinor) else "") }
    var savedText by remember { mutableStateOf(Money.plain(goal.savedMinor)) }
    var deadline by remember { mutableStateOf(goal.deadlineEpochDay?.let { Dates.of(it) }) }

    val openDatePicker = rememberDatePicker(deadline ?: Dates.today().plusMonths(3)) { deadline = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal.id == 0L) "New goal" else "Edit goal") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it.take(2) },
                        label = { Text("Icon") },
                        singleLine = true,
                        modifier = Modifier.weight(0.35f)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Goal") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                AmountField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    currency = currency,
                    label = "Target"
                )
                AmountField(
                    value = savedText,
                    onValueChange = { savedText = it },
                    currency = currency,
                    label = "Already saved"
                )
                OutlinedButton(onClick = openDatePicker, modifier = Modifier.fillMaxWidth()) {
                    Text(deadline?.let { "Deadline: " + Dates.shortDate(it) } ?: "Pick a deadline (optional)")
                }
                if (deadline != null) {
                    TextButton(onClick = { deadline = null }) { Text("Clear deadline") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val target = Money.parse(targetText) ?: 0L
                if (name.isNotBlank() && target > 0) {
                    onSave(
                        goal.copy(
                            name = name.trim(),
                            emoji = emoji.ifBlank { "🎯" },
                            targetMinor = target,
                            savedMinor = Money.parse(savedText) ?: 0L,
                            deadlineEpochDay = deadline?.toEpochDay()
                        )
                    )
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ContributeDialog(
    goal: Goal,
    currency: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to " + goal.name) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountField(value = amountText, onValueChange = { amountText = it }, currency = currency)
                Text(
                    "This is logged as spending too, so your daily budget stays honest.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Money.parse(amountText)?.let { if (it > 0) onConfirm(it) }
                onDismiss()
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
