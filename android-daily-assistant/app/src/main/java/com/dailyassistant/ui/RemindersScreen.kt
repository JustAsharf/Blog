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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.dailyassistant.data.Reminder
import com.dailyassistant.data.RepeatMode
import com.dailyassistant.util.Dates
import com.dailyassistant.work.Scheduler
import com.dailyassistant.vm.MainViewModel
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RemindersScreen(vm: MainViewModel, onBack: () -> Unit) {
    val reminders by vm.reminders.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Reminder?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Reminders") },
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
                    Text("  New reminder")
                }
            }

            item {
                SectionCard {
                    Text(
                        "Reminders fire on this phone alone — no account, no internet. " +
                            "Keep notifications allowed for Daily Assistant and they will arrive even after a restart.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (reminders.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "⏰",
                        title = "No reminders",
                        text = "Pay rent, call the family, take medicine, log today's spending — set it once and forget it."
                    )
                }
            }

            items(items = reminders, key = { it.id }) { reminder ->
                val next = Scheduler.nextOccurrence(reminder)
                SectionCard {
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RoundIcon(Icons.Default.Alarm, tint = if (reminder.enabled) Tones.info else MaterialTheme.colorScheme.onSurfaceVariant)
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    reminder.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    Dates.minuteOfDayToText(reminder.minuteOfDay) + " · " + repeatLabel(reminder),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (reminder.enabled && next != null) {
                                    Text(
                                        "Next: " + next.format(DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", Locale.US)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Tones.good
                                    )
                                }
                                if (reminder.system) {
                                    Text(
                                        "Built-in daily nudge",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = reminder.enabled,
                                onCheckedChange = { vm.toggleReminder(reminder, it) }
                            )
                        }
                        if (reminder.note.isNotBlank()) {
                            Text(
                                reminder.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = { editing = reminder }, modifier = Modifier.weight(1f)) {
                                Text("Edit")
                            }
                            if (!reminder.system) {
                                IconButton(onClick = { vm.deleteReminder(reminder) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete reminder")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (showNew) {
        ReminderDialog(
            reminder = Reminder(title = "", startEpochDay = Dates.today().toEpochDay()),
            onSave = { vm.saveReminder(it) },
            onDismiss = { showNew = false }
        )
    }

    editing?.let { reminder ->
        ReminderDialog(
            reminder = reminder,
            onSave = { vm.saveReminder(it) },
            onDismiss = { editing = null }
        )
    }
}

private fun repeatLabel(reminder: Reminder): String = when (reminder.repeatMode) {
    RepeatMode.DAILY -> "every day"
    RepeatMode.WEEKLY -> {
        val days = DayOfWeek.values().filter { reminder.daysOfWeekMask and (1 shl (it.value - 1)) != 0 }
        if (days.isEmpty()) "weekly" else days.joinToString(", ") { Dates.weekdayInitial(it) }
    }
    RepeatMode.MONTHLY -> "day " + Dates.of(reminder.startEpochDay).dayOfMonth + " each month"
    else -> "once on " + Dates.shortDate(Dates.of(reminder.startEpochDay))
}

@Composable
private fun ReminderDialog(
    reminder: Reminder,
    onSave: (Reminder) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(reminder.title) }
    var note by remember { mutableStateOf(reminder.note) }
    var minuteOfDay by remember { mutableStateOf(reminder.minuteOfDay) }
    var repeatMode by remember { mutableStateOf(reminder.repeatMode) }
    var mask by remember { mutableStateOf(reminder.daysOfWeekMask) }
    var startDate by remember { mutableStateOf(Dates.of(reminder.startEpochDay)) }

    val openTimePicker = rememberTimePicker(minuteOfDay) { minuteOfDay = it }
    val openDatePicker = rememberDatePicker(startDate) { startDate = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (reminder.id == 0L) "New reminder" else "Edit reminder") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What should I remind you about?") },
                    singleLine = true,
                    enabled = !reminder.system,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = openTimePicker, modifier = Modifier.fillMaxWidth()) {
                    Text("Time: " + Dates.minuteOfDayToText(minuteOfDay))
                }
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RepeatMode.all.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { mode ->
                                FilterChip(
                                    selected = repeatMode == mode,
                                    onClick = { repeatMode = mode },
                                    label = { Text(mode.lowercase(Locale.US)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if (repeatMode == RepeatMode.WEEKLY) {
                    Text("Days", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        DayOfWeek.values().forEach { day ->
                            val bit = 1 shl (day.value - 1)
                            val selected = mask and bit != 0
                            FilterChip(
                                selected = selected,
                                onClick = { mask = if (selected) mask and bit.inv() else mask or bit },
                                label = {
                                    Text(
                                        Dates.weekdayInitial(day).take(1),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                OutlinedButton(onClick = openDatePicker, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        (if (repeatMode == RepeatMode.ONCE) "Date: " else "Starting: ") + Dates.shortDate(startDate)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    onSave(
                        reminder.copy(
                            title = title.trim(),
                            note = note.trim(),
                            minuteOfDay = minuteOfDay,
                            repeatMode = repeatMode,
                            daysOfWeekMask = mask,
                            startEpochDay = startDate.toEpochDay(),
                            enabled = true
                        )
                    )
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
