@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dailyassistant.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailyassistant.data.Insights
import com.dailyassistant.util.Dates
import com.dailyassistant.util.Money
import com.dailyassistant.vm.MainViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReportsScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    val entries by vm.monthEntries.collectAsState()
    val month by vm.selectedMonth.collectAsState()
    val categories by vm.categories.collectAsState()
    val recent by vm.recentEntries.collectAsState()
    val currency = settings.currency

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = vm.csv()
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                }.onSuccess { vm.say("CSV exported") }
                    .onFailure { vm.say("Could not write the file") }
            }
        }
    }

    val spent = entries.filter { !it.isIncome }.sumOf { it.amountMinor }
    val income = entries.filter { it.isIncome }.sumOf { it.amountMinor }

    val byCategory = entries.filter { !it.isIncome }
        .groupBy { it.categoryId }
        .map { (categoryId, list) ->
            val category = categories.firstOrNull { it.id == categoryId }
            Triple(
                category?.let { it.emoji + " " + it.name } ?: "💸 Uncategorised",
                list.sumOf { it.amountMinor },
                category?.let { Color(it.colorArgb.toInt()) } ?: Color(0xFF6C757D)
            )
        }
        .sortedByDescending { it.second }

    val last30 = Insights.dailyTotals(recent, 30).map { (date, value) ->
        (if (date.dayOfMonth % 5 == 0) date.dayOfMonth.toString() else "") to value
    }

    val byMethod = entries.filter { !it.isIncome }
        .groupBy { it.method }
        .map { (method, list) -> method to list.sumOf { it.amountMinor } }
        .sortedByDescending { it.second }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            SectionCard(title = "Where it went") {
                if (byCategory.isEmpty()) {
                    EmptyState("📊", "Nothing to chart yet", "Log a few expenses and the breakdown appears here.")
                } else {
                    Column {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            DonutChart(
                                slices = byCategory.map { it.third to it.second },
                                centerLabel = Money.format(spent, currency),
                                centerCaption = "spent"
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        byCategory.forEach { (label, value, color) ->
                            BreakdownRow(
                                color = color,
                                label = label,
                                value = Money.format(value, currency) +
                                    "  (" + (if (spent > 0) value * 100 / spent else 0) + "%)",
                                fraction = if (spent > 0) (value.toDouble() / spent).toFloat() else 0f
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Last 30 days") {
                Column {
                    BarChart(bars = last30, maxHeight = 110)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Daily spending. Taller bar, heavier day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Month out",
                    value = Money.format(spent, currency),
                    modifier = Modifier.weight(1f),
                    accent = Tones.bad
                )
                StatTile(
                    label = "Month in",
                    value = Money.format(income, currency),
                    modifier = Modifier.weight(1f),
                    accent = Tones.good
                )
            }
        }

        if (byMethod.isNotEmpty()) {
            item {
                SectionCard(title = "How you paid") {
                    Column {
                        byMethod.forEach { (method, value) ->
                            BreakdownRow(
                                color = MaterialTheme.colorScheme.secondary,
                                label = method,
                                value = Money.format(value, currency),
                                fraction = if (spent > 0) (value.toDouble() / spent).toFloat() else 0f
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Export") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Everything stays on this phone. Export when you want a copy in a spreadsheet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            csvLauncher.launch(
                                "daily-assistant-" +
                                    Dates.today().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)) +
                                    ".csv"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Export all entries as CSV") }
                }
            }
        }

        item { Spacer(Modifier.height(90.dp)) }
    }
}
