package com.example.timetable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.timetable.ui.theme.CrimsonRed
import com.example.timetable.ui.theme.Gold
import com.example.timetable.ui.theme.GradientOrange
import com.example.timetable.ui.theme.NearBlack
import com.example.timetable.ui.theme.OrangeRed
import com.example.timetable.ui.theme.TimetableTheme
import com.example.timetable.ui.theme.WineRed

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimetableTheme {
                ThemePreviewScreen()
            }
        }
    }
}

@Composable
fun ThemePreviewScreen() {
    var clicks by rememberSaveable { mutableIntStateOf(0) }
    var switchOn by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { clicks++ }) {
                Text(text = "+", style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Timetable", style = MaterialTheme.typography.headlineMedium)
            Text("превью темы", style = MaterialTheme.typography.bodyMedium)
            Text("fab нажат: $clicks", style = MaterialTheme.typography.bodySmall)

            Button(onClick = { clicks++ }) { Text("Filled") }
            FilledTonalButton(onClick = { clicks++ }) { Text("Tonal") }
            OutlinedButton(onClick = { clicks++ }) { Text("Outlined") }
            TextButton(onClick = { clicks++ }) { Text("Text") }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = switchOn, onCheckedChange = { switchOn = it })
                Text(
                    text = if (switchOn) "  включено" else "  выключено",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Card", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "обычная карточка",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ElevatedCard", style = MaterialTheme.typography.titleMedium)
                    Text("карточка с тенью", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // градиенты прозапас, посмотреть как смотрятся
            GradientBlock(
                colors = listOf(WineRed, NearBlack),
                label = "wine -> black",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientChip(listOf(GradientOrange, CrimsonRed))
                GradientChip(listOf(Gold, OrangeRed))
                GradientChip(listOf(WineRed, NearBlack))
            }
        }
    }
}

@Composable
private fun GradientBlock(colors: List<Color>, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(colors))
            .padding(16.dp),
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun GradientChip(colors: List<Color>) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(colors)),
    )
}

@Preview(showBackground = true)
@Composable
fun ThemePreviewScreenPreview() {
    TimetableTheme {
        ThemePreviewScreen()
    }
}
