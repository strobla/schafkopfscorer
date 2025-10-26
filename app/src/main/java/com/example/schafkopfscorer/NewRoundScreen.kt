package com.example.schafkopfscorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewRoundScreen(
    allPlayers: List<Player>,
    onDismiss: () -> Unit,
    onSave: (gameType: GameType, player: Player, partner: Player?, won: Boolean, points: Int, laufende: Int, kontra: Boolean, re: Boolean, activePlayers: List<Player>) -> Unit,
    onSaveRamsch: (scores: Map<Player, Int>, jungfrauPlayers: List<Player>, activePlayers: List<Player>) -> Unit
) {
    var selectedGameType by remember { mutableStateOf(GameType.RUFSPIEL) }
    var activePlayers by remember { mutableStateOf<Set<Player>>(emptySet()) }

    // NEU: Filtert nur aktive Spieler für die Auswahl
    val activePlayersInTable = allPlayers.filter { it.isActive }

    // NEU: Wenn 4 aktive Spieler, sind alle aktiv.
    LaunchedEffect(activePlayersInTable) {
        if (activePlayersInTable.size == 4) {
            activePlayers = activePlayersInTable.toSet()
        }
    }

    val isPlayerSelectionValid = activePlayersInTable.size == 4 || activePlayers.size == 4
    val currentRoundPlayers = if (activePlayersInTable.size == 4) activePlayersInTable else activePlayers.toList()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Neue Runde erfassen",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                )

                // NEU: Sektion zur Spielerauswahl, wenn > 4 aktive Spieler
                if (activePlayersInTable.size > 4) {
                    Text("Wer spielt diese Runde? (Genau 4 auswählen)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // GEÄNDERT: Iteriert nur über aktive Spieler
                        activePlayersInTable.forEach { player ->
                            FilterChip(
                                selected = player in activePlayers,
                                onClick = {
                                    activePlayers = if (player in activePlayers) {
                                        activePlayers - player
                                    } else {
                                        activePlayers + player
                                    }
                                },
                                label = { Text(player.name) }
                            )
                        }
                    }
                    if (!isPlayerSelectionValid) {
                        Text(
                            "Bitte genau 4 Spieler auswählen (${activePlayers.size}/4)",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                }

                // Der Rest des Formulars ist nur aktiv, wenn 4 Spieler ausgewählt sind
                if (isPlayerSelectionValid) {
                    Text("Spielart", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GameType.values().forEach { gameType ->
                            FilterChip(
                                selected = selectedGameType == gameType,
                                onClick = { selectedGameType = gameType },
                                label = { Text(gameType.displayName) }
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    if (selectedGameType == GameType.RAMSCH) {
                        RamschInputForm(
                            // GEÄNDERT: Übergibt nur die 4 aktiven Spieler
                            players = currentRoundPlayers,
                            onDismiss = onDismiss,
                            onSave = { scores, jungfrauPlayers ->
                                // GEÄNDERT: Übergibt 'activePlayers'
                                onSaveRamsch(scores, jungfrauPlayers, currentRoundPlayers)
                            }
                        )
                    } else {
                        StandardInputForm(
                            // GEÄNDERT: Übergibt nur die 4 aktiven Spieler
                            players = currentRoundPlayers,
                            selectedGameType = selectedGameType,
                            onDismiss = onDismiss,
                            onSave = { gameType, player, partner, won, points, laufende, kontra, re ->
                                // GEÄNDERT: Übergibt 'activePlayers'
                                onSave(gameType, player, partner, won, points, laufende, kontra, re, currentRoundPlayers)
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun RamschInputForm(
    players: List<Player>, // Erhält jetzt nur die 4 aktiven Spieler
    onDismiss: () -> Unit,
    onSave: (scores: Map<Player, Int>, jungfrauPlayers: List<Player>) -> Unit
) {
    var scores by remember { mutableStateOf(players.associateWith { "" }) }
    var jungfrauPlayers by remember { mutableStateOf<Set<Player>>(emptySet()) }
    val scoreInts = scores.mapValues { it.value.toIntOrNull() ?: 0 }
    val totalPoints = scoreInts.values.sum()

    val isSaveEnabled = totalPoints in 1..120

    Column {
        Text("Augen der Spieler (nur Verlierer eintragen genügt)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Baut UI nur für die 4 aktiven Spieler
        players.forEach { player ->
            OutlinedTextField(
                value = scores[player] ?: "",
                onValueChange = { scores = scores.toMutableMap().apply { put(player, it.filter { char -> char.isDigit() }) } },
                label = { Text(player.name) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Summe der eingetragenen Augen: $totalPoints",
            style = MaterialTheme.typography.bodyMedium,
            color = if (totalPoints > 120) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Jungfrau (keinen Stich gemacht)", style = MaterialTheme.typography.titleMedium)
        // Baut UI nur für die 4 aktiven Spieler
        players.forEach { player ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        jungfrauPlayers = if (player in jungfrauPlayers) {
                            jungfrauPlayers - player
                        } else {
                            jungfrauPlayers + player
                        }
                    }
                    .padding(vertical = 2.dp)
            ) {
                Checkbox(
                    checked = player in jungfrauPlayers,
                    onCheckedChange = null
                )
                Spacer(Modifier.width(8.dp))
                Text(player.name)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSave(scoreInts, jungfrauPlayers.toList()) },
                enabled = isSaveEnabled
            ) {
                Text("Speichern")
            }
        }
    }
}

// ** HIER WAR DER FEHLER - DIESE FUNKTION HAT GEFEHLT **
@Composable
private fun StandardInputForm(
    players: List<Player>, // Erhält jetzt nur die 4 aktiven Spieler
    selectedGameType: GameType,
    onDismiss: () -> Unit,
    onSave: (gameType: GameType, player: Player, partner: Player?, won: Boolean, points: Int, laufende: Int, kontra: Boolean, re: Boolean) -> Unit
) {
    var declaringPlayer by remember { mutableStateOf(players.first()) }
    var partnerPlayer by remember { mutableStateOf<Player?>(null) }
    var playerPartyWon by remember { mutableStateOf(true) }
    var playerPartyPoints by remember { mutableStateOf("") }
    var laufende by remember { mutableStateOf("") }
    var kontra by remember { mutableStateOf(false) }
    var re by remember { mutableStateOf(false) }

    LaunchedEffect(selectedGameType, players) {
        // Stellt sicher, dass der Spieler zurückgesetzt wird, wenn sich die aktiven Spieler ändern
        declaringPlayer = players.first()
        if (selectedGameType != GameType.RUFSPIEL) {
            partnerPlayer = null
        }
    }

    Text("Spieler", style = MaterialTheme.typography.titleMedium)
    PlayerDropdown(
        players = players,
        selectedPlayer = declaringPlayer,
        onPlayerSelected = { declaringPlayer = it }
    )
    Spacer(modifier = Modifier.height(16.dp))

    if (selectedGameType == GameType.RUFSPIEL) {
        Text("Partner", style = MaterialTheme.typography.titleMedium)
        PlayerDropdown(
            players = players.filter { it.id != declaringPlayer.id },
            selectedPlayer = partnerPlayer,
            onPlayerSelected = { partnerPlayer = it },
            label = "Partner auswählen"
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    Text("Ergebnis", style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Spielerpartei hat gewonnen?")
        Spacer(modifier = Modifier.weight(1f))
        Switch(checked = playerPartyWon, onCheckedChange = { playerPartyWon = it })
    }

    if (selectedGameType != GameType.BETTEL) {
        OutlinedTextField(
            value = playerPartyPoints,
            onValueChange = { playerPartyPoints = it.filter { char -> char.isDigit() } },
            label = { Text("Augen der Spielerpartei") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = laufende,
            onValueChange = { laufende = it.filter { char -> char.isDigit() } },
            label = { Text("Anzahl Laufende") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text("Boni", style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Kontra")
        Spacer(modifier = Modifier.weight(1f))
        Checkbox(checked = kontra, onCheckedChange = { kontra = it })
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Re")
        Spacer(modifier = Modifier.weight(1f))
        Checkbox(checked = re, onCheckedChange = { re = it }, enabled = kontra)
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) {
            Text("Abbrechen")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = {
                onSave(
                    selectedGameType,
                    declaringPlayer,
                    partnerPlayer,
                    playerPartyWon,
                    playerPartyPoints.toIntOrNull() ?: 0,
                    laufende.toIntOrNull() ?: 0,
                    kontra,
                    re
                )
            },
            enabled = if (selectedGameType == GameType.RUFSPIEL) partnerPlayer != null else true
        ) {
            Text("Speichern")
        }
    }
}

// ** HIER WAR DER FEHLER - DIESE FUNKTION HAT GEFEHLT **
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerDropdown(
    players: List<Player>,
    selectedPlayer: Player?,
    onPlayerSelected: (Player) -> Unit,
    label: String = "Spieler auswählen"
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedPlayer?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            players.forEach { player ->
                DropdownMenuItem(
                    text = { Text(player.name) },
                    onClick = {
                        onPlayerSelected(player)
                        expanded = false
                    }
                )
            }
        }
    }
}

