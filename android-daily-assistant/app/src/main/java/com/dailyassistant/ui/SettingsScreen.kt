@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dailyassistant.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.dailyassistant.data.CURRENCIES
import com.dailyassistant.util.Dates
import com.dailyassistant.util.Money
import com.dailyassistant.vm.MainViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val budget by vm.budget.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var salaryText by remember(settings.salaryMinor) {
        mutableStateOf(if (settings.salaryMinor > 0) Money.plain(settings.salaryMinor) else "")
    }
    var paydayText by remember(settings.paydayDay) { mutableStateOf(settings.paydayDay.toString()) }
    var savingsPercent by remember(settings.savingsPercent) { mutableStateOf(settings.savingsPercent.toFloat()) }
    var showPinDialog by remember { mutableStateOf(false) }

    val openNudgeTimePicker = rememberTimePicker(settings.dailyNudgeMinute) { minute ->
        vm.setDailyNudge(settings.dailyNudgeEnabled, minute)
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = vm.backupJson()
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }.onSuccess { vm.say("Backup saved") }
                    .onFailure { vm.say("Could not write the backup") }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                }.getOrNull()
                if (text.isNullOrBlank()) vm.say("Could not read that file") else vm.restoreJson(text)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = "Salary") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AmountField(
                        value = salaryText,
                        onValueChange = { salaryText = it },
                        currency = settings.currency,
                        label = "Monthly salary (take-home)"
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = paydayText,
                            onValueChange = { paydayText = it.filter { ch -> ch.isDigit() }.take(2) },
                            label = { Text("Payday (day of month)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            vm.updateSettings {
                                it.copy(
                                    salaryMinor = Money.parse(salaryText) ?: 0L,
                                    paydayDay = (paydayText.toIntOrNull() ?: 1).coerceIn(1, 31)
                                )
                            }
                            vm.say("Salary updated")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save salary") }
                    Text(
                        "Current cycle: " +
                            budget.periodStart.format(DateTimeFormatter.ofPattern("d MMM", Locale.US)) + " → " +
                            budget.periodEnd.format(DateTimeFormatter.ofPattern("d MMM", Locale.US)) +
                            " (" + budget.daysInPeriod + " days)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard(title = "Save first, spend after") {
                Column {
                    Text(
                        "Keep " + savingsPercent.toInt() + "% of the salary out of the daily budget — " +
                            Money.format(settings.salaryMinor * savingsPercent.toInt() / 100, settings.currency) +
                            " a month.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = savingsPercent,
                        onValueChange = { savingsPercent = it },
                        onValueChangeFinished = {
                            vm.updateSettings { it.copy(savingsPercent = savingsPercent.toInt()) }
                        },
                        valueRange = 0f..60f,
                        steps = 11
                    )
                    LabeledSwitch(
                        label = "Carry unspent money forward",
                        checked = settings.carryOver,
                        onCheckedChange = { value -> vm.updateSettings { it.copy(carryOver = value) } },
                        caption = "A quiet day today means a bigger allowance tomorrow."
                    )
                }
            }

            SectionCard(title = "Currency") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CURRENCIES.chunked(5).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { symbol ->
                                FilterChip(
                                    selected = settings.currency == symbol,
                                    onClick = { vm.updateSettings { it.copy(currency = symbol) } },
                                    label = { Text(symbol, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            SectionCard(title = "Daily nudge") {
                Column {
                    LabeledSwitch(
                        label = "Remind me to log spending",
                        checked = settings.dailyNudgeEnabled,
                        onCheckedChange = { vm.setDailyNudge(it, settings.dailyNudgeMinute) },
                        caption = "Every day at " + Dates.minuteOfDayToText(settings.dailyNudgeMinute)
                    )
                    OutlinedButton(onClick = openNudgeTimePicker, modifier = Modifier.fillMaxWidth()) {
                        Text("Change the time")
                    }
                }
            }

            SectionCard(title = "Look") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("System" to 0, "Light" to 1, "Dark" to 2).forEach { (label, mode) ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { vm.updateSettings { it.copy(themeMode = mode) } },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            SectionCard(title = "App lock") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (settings.pinHash == null) "No PIN set. Anyone opening the app sees your money."
                        else "A PIN is required when the app starts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showPinDialog = true }, modifier = Modifier.weight(1f)) {
                            Text(if (settings.pinHash == null) "Set a PIN" else "Change PIN")
                        }
                        if (settings.pinHash != null) {
                            OutlinedButton(onClick = { vm.setPin(null) }, modifier = Modifier.weight(1f)) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }

            SectionCard(title = "Backup & restore") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your data lives only on this phone. Save a backup file somewhere you trust " +
                            "(your own storage, SD card, or a copy you move yourself) and restore it on a new phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            backupLauncher.launch(
                                "daily-assistant-backup-" +
                                    Dates.today().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)) +
                                    ".json"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save a backup file") }
                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Restore from a backup file") }
                    Text(
                        "Restoring replaces everything currently in the app.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showPinDialog) {
        PinDialog(
            onConfirm = { pin -> vm.setPin(pin) },
            onDismiss = { showPinDialog = false }
        )
    }
}

@Composable
private fun PinDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val matches = pin.length >= 4 && pin == confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a PIN") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(8) },
                    label = { Text("PIN (4-8 digits)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it.filter { ch -> ch.isDigit() }.take(8) },
                    label = { Text("Repeat it") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "There is no way to recover a forgotten PIN — it is stored hashed on the phone and nowhere else.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(enabled = matches, onClick = { onConfirm(pin); onDismiss() }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
