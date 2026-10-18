@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dailyassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailyassistant.data.Category
import com.dailyassistant.data.Entry
import com.dailyassistant.util.Dates
import com.dailyassistant.util.Money

@Composable
fun EntryRow(
    entry: Entry,
    category: Category?,
    currency: String,
    showDate: Boolean = true,
    onClick: () -> Unit = {},
    onDelete: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tint = category?.let { Color(it.colorArgb.toInt()) }
                ?: if (entry.isIncome) Tones.good else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                Modifier
                    .size(38.dp)
                    .background(tint.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (entry.isIncome) "💵" else (category?.emoji ?: "💸"))
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    entry.note.ifBlank { category?.name ?: if (entry.isIncome) "Income" else "Expense" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
                val meta = buildString {
                    if (category != null) append(category.name)
                    if (showDate) {
                        if (isNotEmpty()) append(" · ")
                        append(Dates.dayLabel(Dates.of(entry.epochDay)))
                    }
                    if (entry.method != "cash") {
                        if (isNotEmpty()) append(" · ")
                        append(entry.method)
                    }
                }
                if (meta.isNotEmpty()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (entry.isIncome) "+" else "-") + Money.format(entry.amountMinor, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (entry.isIncome) Tones.good else MaterialTheme.colorScheme.onSurface
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Wrapping category chooser built from fixed rows, so it behaves the same on every screen size. */
@Composable
fun CategoryPicker(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    perRow: Int = 3
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(perRow).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { category ->
                    val selected = category.id == selectedId
                    val color = Color(category.colorArgb.toInt())
                    Surface(
                        onClick = { onSelect(if (selected) null else category.id) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.emoji)
                            Text(
                                category.name,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 6.dp),
                                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                repeat(perRow - rowItems.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}
