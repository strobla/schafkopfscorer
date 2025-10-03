package com.example.schafkopfscorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.schafkopfscorer.ui.theme.SchafkopfScorerTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SchafkopfScorerTheme {
                var showNewRoundDialog by remember { mutableStateOf(false) }
                var showResetDialog by remember { mutableStateOf(false) }
                val gameState by gameViewModel.gameState.collectAsState()

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Schafkopf Schreiber") },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            actions = {
                                IconButton(onClick = { showResetDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Spiel zurücksetzen"
                                    )
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showNewRoundDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Neue Runde")
                        }
                    }
                ) { paddingValues ->
                    GameScreen(
                        modifier = Modifier.padding(paddingValues),
                        gameState = gameState,
                        viewModel = gameViewModel
                    )
                }

                if (showNewRoundDialog) {
                    NewRoundScreen(
                        players = gameState.players,
                        onDismiss = { showNewRoundDialog = false },
                        onSave = { gameType, player, partner, won, points, laufende, kontra, re ->
                            gameViewModel.addRound(gameType, player, partner, won, points, laufende, kontra, re)
                            showNewRoundDialog = false
                        }
                    )
                }

                if (showResetDialog) {
                    ResetConfirmationDialog(
                        onConfirm = {
                            gameViewModel.resetGame()
                            showResetDialog = false
                        },
                        onDismiss = { showResetDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun GameScreen(modifier: Modifier = Modifier, gameState: GameState, viewModel: GameViewModel) {
    var playerToEdit by remember { mutableStateOf<Player?>(null) }

    playerToEdit?.let { player ->
        EditPlayerNameDialog(
            player = player,
            onDismiss = { playerToEdit = null },
            onSave = { updatedPlayer, newName ->
                viewModel.updatePlayerName(updatedPlayer, newName)
                playerToEdit = null
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        ScoreHeader(
            scores = gameState.totalScores,
            players = gameState.players,
            onPlayerClick = { player ->
                playerToEdit = player
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        RoundHistory(rounds = gameState.rounds, players = gameState.players)
    }
}

@Composable
fun ScoreHeader(
    scores: Map<Player, Int>,
    players: List<Player>,
    onPlayerClick: (Player) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            players.forEach { player ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPlayerClick(player) }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = (scores[player] ?: 0).toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if ((scores[player] ?: 0) >= 0) Color(0xFF008000) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun RoundHistory(rounds: List<RoundResult>, players: List<Player>) {
    Text("Protokoll", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // NEU: Spalte für die Spielnummer
                    Text("Nr.", modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Text("Spiel", modifier = Modifier.weight(2f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    players.forEach { player ->
                        Text(
                            text = player.name.take(3),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Divider()
            }
            if (rounds.isEmpty()) {
                item {
                    Text(
                        text = "Noch keine Runden gespielt.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // GEÄNDERT: items zu itemsIndexed, um den Index zu erhalten
                itemsIndexed(rounds.reversed()) { index, round ->
                    // Die Spielnummer wird aus der Gesamtanzahl der Runden berechnet
                    val roundNumber = rounds.size - index
                    RoundRow(roundNumber = roundNumber, round = round, players = players)
                    Divider()
                }
            }
        }
    }
}

@Composable
fun RoundRow(roundNumber: Int, round: RoundResult, players: List<Player>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // NEU: Anzeige der Spielnummer
        Text(
            text = "$roundNumber",
            modifier = Modifier.weight(0.7f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Column(modifier = Modifier.weight(2f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(round.gameType.displayName, fontSize = 14.sp)
            Text(
                "von ${round.declaringPlayer.name}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        players.forEach { player ->
            val points = round.points[player] ?: 0
            Text(
                text = if (points > 0) "+$points" else "$points",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = when {
                    points > 0 -> Color(0xFF008000)
                    points < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EditPlayerNameDialog(
    player: Player,
    onDismiss: () -> Unit,
    onSave: (Player, String) -> Unit
) {
    var newName by remember { mutableStateOf(player.name) }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Namen ändern",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        isError = it.isBlank()
                    },
                    label = { Text("Spielername") },
                    singleLine = true,
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Name darf nicht leer sein")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onSave(player, newName)
                            } else {
                                isError = true
                            }
                        }
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}

@Composable
fun ResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Spiel zurücksetzen?") },
        text = { Text("Möchten Sie das aktuelle Spiel wirklich zurücksetzen? Alle Runden und Punktestände gehen verloren.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Zurücksetzen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

