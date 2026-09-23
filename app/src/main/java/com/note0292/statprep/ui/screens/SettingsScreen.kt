package com.note0292.statprep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    includeOptional: Boolean,
    onIncludeOptionalChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = { BackTopBar("設定", onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(onClick = { onIncludeOptionalChange(!includeOptional) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("発展分野を学習に含める", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "「効果測定・A/Bテスト」「因果推論」の章と問題を追加します。" +
                                "オンにすると、テキスト・分野別学習・ランダム出題・模擬試験・学習記録の対象になります。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = includeOptional, onCheckedChange = onIncludeOptionalChange)
                }
            }
        }
    }
}
