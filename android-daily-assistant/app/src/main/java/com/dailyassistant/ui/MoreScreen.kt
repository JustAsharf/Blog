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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Everything else",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        MoreRow("Savings goals", "Targets, deadlines, progress", Icons.Default.Savings) { onNavigate("goals") }
        MoreRow("Reminders", "Daily nudges and one-off alarms", Icons.Default.Alarm) { onNavigate("reminders") }
        MoreRow("Debts", "Money you owe and money owed to you", Icons.Default.People) { onNavigate("debts") }
        MoreRow("Categories & budgets", "Rename, recolour, set limits", Icons.Default.Label) { onNavigate("categories") }
        MoreRow("Settings", "Salary, payday, backup, app lock", Icons.Default.Settings) { onNavigate("settings") }

        SectionCard(title = "About") {
            Text(
                "Daily Assistant keeps your salary, spending, bills, goals and reminders on this phone only. " +
                    "There is no server, no sign-in and no internet permission — which is also why your backup file " +
                    "is the only way to move data to a new phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun MoreRow(title: String, caption: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(icon)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}
