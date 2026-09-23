package com.note0292.statprep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.note0292.statprep.data.Category
import com.note0292.statprep.data.Progress
import com.note0292.statprep.data.Question

@Composable
fun CategoryScreen(
    categories: List<Category>,
    questions: List<Question>,
    progress: Progress,
    onSelect: (Category) -> Unit,
    onChapter: (Category) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = { BackTopBar("分野別に学習", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories) { category ->
                val qs = questions.filter { it.category == category.id }
                val stats = qs.map { progress.stat(it.id) }
                val answered = stats.count { it.attempts > 0 }
                val attempts = stats.sumOf { it.attempts }
                val correct = stats.sumOf { it.correct }
                Card(onClick = { onSelect(category) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(category.label, style = MaterialTheme.typography.titleMedium)
                            LinearProgressIndicator(
                                progress = { if (qs.isEmpty()) 0f else answered.toFloat() / qs.size },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                "解答済み $answered / ${qs.size}問　正答率 " +
                                    if (attempts == 0) "−" else percent(correct.toDouble() / attempts),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onChapter(category) }) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "${category.label}のテキスト")
                        }
                    }
                }
            }
        }
    }
}
