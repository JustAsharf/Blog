@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dailyassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dailyassistant.data.Category
import com.dailyassistant.util.Money
import com.dailyassistant.vm.MainViewModel

private val swatches = listOf(
    0xFFE4572E, 0xFF2E933C, 0xFF2F6FED, 0xFF8D6346, 0xFF6A4C93,
    0xFFF0A202, 0xFF00A6A6, 0xFFD64550, 0xFFEF6F6C, 0xFF9A48D0,
    0xFF3DA5D9, 0xFF6C757D
)

@Composable
fun CategoriesScreen(vm: MainViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val categories by vm.categories.collectAsState()
    val monthEntries by vm.periodEntries.collectAsState()
    val currency = settings.currency

    var showNew by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Categories & budgets") },
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
                    Text("  New category")
                }
            }

            item {
                SectionCard {
                    Text(
                        "Give a category a monthly budget and you get a bar showing how much of it is gone this cycle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(items = categories, key = { it.id }) { category ->
                val spent = monthEntries.filter { !it.isIncome && it.categoryId == category.id }
                    .sumOf { it.amountMinor }
                val color = Color(category.colorArgb.toInt())
                SectionCard {
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .background(color.copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text(category.emoji) }
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                            ) {
                                Text(category.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    Money.format(spent, currency) + " this cycle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { editing = category }) { Text("Edit") }
                            IconButton(onClick = { vm.archiveCategory(category) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove category")
                            }
                        }
                        if (category.monthlyBudgetMinor > 0) {
                            Spacer(Modifier.height(6.dp))
                            ProgressBlock(
                                label = "Budget",
                                fraction = (spent.toDouble() / category.monthlyBudgetMinor).toFloat(),
                                caption = Money.format(spent, currency) + " of " +
                                    Money.format(category.monthlyBudgetMinor, currency),
                                color = if (spent > category.monthlyBudgetMinor) Tones.bad else color
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (showNew) {
        CategoryDialog(
            category = Category(name = ""),
            currency = currency,
            onSave = { vm.saveCategory(it) },
            onDismiss = { showNew = false }
        )
    }

    editing?.let { category ->
        CategoryDialog(
            category = category,
            currency = currency,
            onSave = { vm.saveCategory(it) },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun CategoryDialog(
    category: Category,
    currency: String,
    onSave: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var emoji by remember { mutableStateOf(category.emoji) }
    var budgetText by remember {
        mutableStateOf(if (category.monthlyBudgetMinor > 0) Money.plain(category.monthlyBudgetMinor) else "")
    }
    var color by remember { mutableStateOf(category.colorArgb) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category.id == 0L) "New category" else "Edit category") },
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
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                AmountField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    currency = currency,
                    label = "Monthly budget (0 for none)"
                )
                Text("Colour", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    swatches.chunked(6).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { argb ->
                                val selected = argb == color
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .background(Color(argb.toInt()), CircleShape)
                                        .border(
                                            width = if (selected) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable { color = argb }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        category.copy(
                            name = name.trim(),
                            emoji = emoji.ifBlank { "💸" },
                            colorArgb = color,
                            monthlyBudgetMinor = Money.parse(budgetText) ?: 0L
                        )
                    )
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
