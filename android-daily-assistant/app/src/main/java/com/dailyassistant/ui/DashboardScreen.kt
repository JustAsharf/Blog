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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailyassistant.data.Budget
import com.dailyassistant.data.Insights
import com.dailyassistant.data.Tone
import com.dailyassistant.util.Dates
import com.dailyassistant.util.Money
import com.dailyassistant.vm.MainViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(vm: MainViewModel, onNavigate: (String) -> Unit) {
    val settings by vm.settings.collectAsState()
    val budget by vm.budget.collectAsState()
    val insights by vm.insights.collectAsState()
    val categories by vm.categories.collectAsState()
    val recent by vm.recentEntries.collectAsState()
    val bills by vm.bills.collectAsState()
    val currency = settings.currency

    val today = Dates.today()
    val todayEntries = recent.filter { it.epochDay == today.toEpochDay() }
    val weekBars = Insights.dailyTotals(recent, 7, today).map { (date, value) ->
        date.format(DateTimeFormatter.ofPattern("EEE", Locale.US)) to value
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Column {
                Text(
                    "Hello 👋",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.US)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Today's allowance — the one number that matters most.
        item {
            SectionCard {
                Column {
                    Text(
                        "Safe to spend today",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        Money.format(budget.todayRemainingMinor, currency),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            !settings.hasSalary -> MaterialTheme.colorScheme.onSurface
                            budget.todayRemainingMinor < 0 -> Tones.bad
                            else -> Tones.good
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    ProgressBlock(
                        label = "Spent today",
                        fraction = budget.todayFraction,
                        caption = Money.format(budget.spentTodayMinor, currency) + " of " +
                            Money.format(budget.dailyAllowanceMinor, currency),
                        color = if (budget.overspentToday) Tones.bad else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    ProgressBlock(
                        label = "This salary cycle",
                        fraction = budget.spentFraction,
                        caption = Money.format(budget.spentPeriodMinor, currency) + " of " +
                            Money.format(budget.spendableMinor, currency),
                        color = if (budget.onTrack) MaterialTheme.colorScheme.primary else Tones.warn
                    )
                    if (!settings.hasSalary) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { onNavigate("settings") }) {
                            Text("Set your salary to switch the budget on")
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Left this cycle",
                    value = Money.format(budget.remainingMinor, currency),
                    modifier = Modifier.weight(1f),
                    accent = if (budget.remainingMinor >= 0) Tones.good else Tones.bad,
                    caption = "${budget.daysToPayday} day(s) to payday"
                )
                StatTile(
                    label = "Projected cycle",
                    value = Money.format(budget.projectedPeriodMinor, currency),
                    modifier = Modifier.weight(1f),
                    accent = if (budget.onTrack) Tones.info else Tones.warn,
                    caption = if (budget.onTrack) "On track" else "Over budget"
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Savings set aside",
                    value = Money.format(budget.savingsTargetMinor, currency),
                    modifier = Modifier.weight(1f),
                    accent = Tones.info,
                    caption = "${settings.savingsPercent}% of salary"
                )
                StatTile(
                    label = "Bills still to pay",
                    value = Money.format(budget.billsUnpaidMinor, currency),
                    modifier = Modifier.weight(1f),
                    accent = if (budget.billsUnpaidMinor > 0) Tones.warn else Tones.good,
                    caption = "This cycle"
                )
            }
        }

        item {
            SectionCard(title = "Quick actions") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { onNavigate("add") },
                        label = { Text("Spend") },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = { onNavigate("reminders") },
                        label = { Text("Remind") },
                        leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = { onNavigate("goals") },
                        label = { Text("Goals") },
                        leadingIcon = { Icon(Icons.Default.Savings, contentDescription = null) }
                    )
                }
            }
        }

        item {
            SectionCard(title = "Last 7 days") {
                Column {
                    BarChart(bars = weekBars)
                    Spacer(Modifier.height(8.dp))
                    val weekTotal = weekBars.sumOf { it.second }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Week total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(Money.format(weekTotal, currency), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (insights.isNotEmpty()) {
            item {
                SectionCard(title = "What your money is doing") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        insights.take(6).forEach { insight ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(insight.emoji, modifier = Modifier.padding(end = 10.dp))
                                Text(
                                    insight.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when (insight.tone) {
                                        Tone.GOOD -> Tones.good
                                        Tone.WARN -> Tones.warn
                                        Tone.BAD -> Tones.bad
                                        Tone.INFO -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        val upcoming = bills.filter { it.active && !Budget.isPaidThisCycle(it) }
            .map { it to Budget.nextDueDate(it) }
            .sortedBy { it.second }
            .take(3)
        if (upcoming.isNotEmpty()) {
            item {
                SectionCard(
                    title = "Coming up",
                    trailing = { TextButton(onClick = { onNavigate("bills") }) { Text("All bills") } }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        upcoming.forEach { (bill, dueDate) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RoundIcon(Icons.Default.TrendingUp, tint = Tones.warn)
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(bill.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        Dates.dayLabel(dueDate) + " · " + Dates.shortDate(dueDate),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    Money.format(bill.amountMinor, currency),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Today") {
                if (todayEntries.isEmpty()) {
                    EmptyState(
                        emoji = "🧾",
                        title = "Nothing logged yet",
                        text = "Tap + and add what you spent. Ten seconds a day keeps the whole thing accurate."
                    )
                } else {
                    Column {
                        todayEntries.forEach { entry ->
                            EntryRow(
                                entry = entry,
                                category = categories.firstOrNull { it.id == entry.categoryId },
                                currency = currency,
                                showDate = false,
                                onDelete = { vm.deleteEntry(entry) }
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(90.dp)) }
    }
}
