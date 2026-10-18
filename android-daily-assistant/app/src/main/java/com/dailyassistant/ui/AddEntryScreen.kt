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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailyassistant.util.Dates
import com.dailyassistant.util.Money
import com.dailyassistant.vm.MainViewModel
import java.time.LocalDate

private val methods = listOf("cash", "card", "transfer", "wallet")

@Composable
fun AddEntryScreen(vm: MainViewModel, onDone: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val categories by vm.categories.collectAsState()
    val currency = settings.currency

    var amountText by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Dates.today()) }
    var method by remember { mutableStateOf("cash") }
    var error by remember { mutableStateOf<String?>(null) }

    val openDatePicker = rememberDatePicker(date) { picked: LocalDate -> date = picked }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(if (isIncome) "Add income" else "Add expense") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isIncome,
                    onClick = { isIncome = false },
                    label = { Text("Money out") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = isIncome,
                    onClick = { isIncome = true },
                    label = { Text("Money in") },
                    modifier = Modifier.weight(1f)
                )
            }

            AmountField(
                value = amountText,
                onValueChange = { amountText = it; error = null },
                currency = currency
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5L, 10L, 20L, 50L, 100L).forEach { preset ->
                    OutlinedButton(
                        onClick = {
                            val existing = Money.parse(amountText) ?: 0L
                            amountText = Money.plain(existing + preset * 100)
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("+$preset", style = MaterialTheme.typography.labelMedium) }
                }
            }

            if (!isIncome) {
                SectionCard(title = "Category") {
                    CategoryPicker(
                        categories = categories,
                        selectedId = categoryId,
                        onSelect = { categoryId = it }
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SectionCard(title = "Details") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = openDatePicker, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Text(
                            "  " + Dates.dayLabel(date) + " · " + Dates.shortDate(date),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        methods.forEach { option ->
                            FilterChip(
                                selected = method == option,
                                onClick = { method = option },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val minor = Money.parse(amountText)
                    if (minor == null || minor <= 0L) {
                        error = "Enter an amount greater than zero"
                    } else {
                        vm.addEntry(
                            amountMinor = minor,
                            categoryId = if (isIncome) null else categoryId,
                            note = note,
                            date = date,
                            isIncome = isIncome,
                            method = method
                        )
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }

            Spacer(Modifier.height(30.dp))
        }
    }
}
