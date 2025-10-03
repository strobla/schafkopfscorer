package com.example.schafkopfscorer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewRoundScreen(
    players: List<Player>,
    onDismiss: () -> Unit,
    onSave: (GameType, Player, Player?, Boolean, Int, Int, Boolean, Boolean) -> Unit
) {
    var selectedGameType by remember { mutableStateOf(GameType.RUFSPIEL) }
    var declaringPlayer by remember { mutableStateOf(players.first()) }
    // Ensure partnerPlayer is initialized safely, avoiding selecting the declaringPlayer
    var partnerPlayer by remember(declaringPlayer) {
        mutableStateOf(players.firstOrNull { it != declaringPlayer } ?: players.first())
    }
    var playerPartyPoints by remember { mutableStateOf("61") }
    var laufende by remember { mutableStateOf("0") }
    var kontra by remember { mutableStateOf(false) }
    var re by remember { mutableStateOf(false) }
    var bettelWon by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Neue Runde erfassen", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))

                SectionTitle("Spielart")
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameType.values().forEach { game ->
                        FilterChip(
                            selected = selectedGameType == game,
                            onClick = { selectedGameType = game },
                            label = { Text(game.displayName) }
                        )
                    }
                }

                SectionTitle("Spieler (Ansager)")
                DropdownSelector(
                    label = "Spieler",
                    items = players,
                    selectedItem = declaringPlayer,
                    onItemSelected = { declaringPlayer = it }
                )

                if (selectedGameType == GameType.RUFSPIEL) {
                    SectionTitle("Partner (Mitspieler)")
                    DropdownSelector(
                        label = "Partner",
                        items = players.filter { it != declaringPlayer },
                        selectedItem = partnerPlayer,
                        onItemSelected = { partnerPlayer = it }
                    )
                }

                SectionTitle("Ergebnis")
                if (selectedGameType == GameType.BETTEL) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        FilterChip(selected = bettelWon, onClick = { bettelWon = true }, label = { Text("Gewonnen") })
                        FilterChip(selected = !bettelWon, onClick = { bettelWon = false }, label = { Text("Verloren") })
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = playerPartyPoints,
                            onValueChange = { playerPartyPoints = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Augen Spielerpartei") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = laufende,
                            onValueChange = { laufende = it.filter { c -> c.isDigit() }.take(1) },
                            label = { Text("Laufende") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                SectionTitle("Multiplikatoren")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = kontra, onCheckedChange = { kontra = it })
                    Text("Kontra / Stoß")
                    Spacer(Modifier.width(16.dp))
                    Checkbox(checked = re, onCheckedChange = { re = it }, enabled = kontra)
                    Text("Re / Retour")
                }

                Spacer(Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val pointsInt = if (selectedGameType != GameType.BETTEL) playerPartyPoints.toIntOrNull() ?: 0 else 0
                        val laufendeInt = if (selectedGameType != GameType.BETTEL) laufende.toIntOrNull() ?: 0 else 0
                        val playerPartyWon = if (selectedGameType == GameType.BETTEL) {
                            bettelWon
                        } else {
                            pointsInt > 60
                        }
                        val finalPartner = if (selectedGameType == GameType.RUFSPIEL) partnerPlayer else null

                        onSave(selectedGameType, declaringPlayer, finalPartner, playerPartyWon, pointsInt, laufendeInt, kontra, re)
                    }) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownSelector(
    label: String,
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            readOnly = true,
            value = selectedItem.toString().substringAfter("name=").substringBefore(")"),
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption.toString().substringAfter("name=").substringBefore(")")) },
                    onClick = {
                        onItemSelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

