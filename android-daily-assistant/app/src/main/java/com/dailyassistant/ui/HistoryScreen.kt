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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.dailyassistant.data.Entry
import com.dailyassistant.util.Dates
import com.dailyassistant.util.Money
import com.dailyassistant.vm.MainViewModel

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    val entries by vm.monthEntries.collectAsState()
    val month by vm.selectedMonth.collectAsState()
    val categories by vm.categories.collectAsState()
    val currency = settings.currency

    var editing by remember { mutableStateOf<Entry?>(null) }

    val spent = entries.filter { !it.isIncome }.sumOf { it.amountMinor }
    val income = entries.filter { it.isIncome }.sumOf { it.amountMinor }
    val byDay = entries.groupBy { it.epochDay }.toList().sortedByDescending { it.first }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { vm.shiftMonth(-1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(
                    Dates.monthLabel(month),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { vm.shiftMonth(1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Spent",
                    value = Money.format(spent, currency),
                    modifier = Modifier.weight(1f),
                    accent = Tones.bad,
                    caption = "${entries.count { !it.isIncome }} entries"
                )
                StatTile(
                    label = "Received",
                    value = Money.format(income, currency),
                    modifier = Modifier.weight(1f),
                    accent = Tones.good,
                    caption = "Extra income"
                )
            }
        }

        if (byDay.isEmpty()) {
            item {
                EmptyState(
                    emoji = "📅",
                    title = "No entries this month",
                    text = "Use the + button to log spending. Past months stay here so you can look back any time."
                )
            }
        }

        items(items = byDay, key = { it.first }) { (epochDay, dayEntries) ->
            val dayTotal = dayEntries.filter { !it.isIncome }.sumOf { it.amountMinor }
            SectionCard {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            Dates.dayLabel(Dates.of(epochDay)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            Money.format(dayTotal, currency),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    dayEntries.forEach { entry ->
                        EntryRow(
                            entry = entry,
                            category = categories.firstOrNull { it.id == entry.categoryId },
                            currency = currency,
                            showDate = false,
                            onClick = { editing = entry },
                            onDelete = { vm.deleteEntry(entry) }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(90.dp)) }
    }

    editing?.let { entry ->
        EntryEditorDialog(
            entry = entry,
            categories = categories,
            currency = currency,
            onSave = { vm.updateEntry(it) },
            onDelete = { vm.deleteEntry(it) },
            onDismiss = { editing = null }
        )
    }
}
