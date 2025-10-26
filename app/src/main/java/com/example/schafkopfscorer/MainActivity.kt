package com.example.schafkopfscorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed // GEÄNDERT: Import
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive // NEU: Import
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Unarchive // NEU: Import
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha // NEU: Import
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.schafkopfscorer.ui.theme.SchafkopfScorerTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SchafkopfScorerTheme {
                var showNewRoundDialog by remember { mutableStateOf(false) }
                var showResetDialog by remember { mutableStateOf(false) }
                var showPlayerManagementDialog by remember { mutableStateOf(false) }
                // NEU: Zustand für die Bearbeitung einer Runde
                var roundToEdit by remember { mutableStateOf<RoundResult?>(null) }

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
                                // NEU: Button für Spieler-Verwaltung
                                IconButton(onClick = { showPlayerManagementDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = "Spieler verwalten"
                                    )
                                }
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
                        viewModel = gameViewModel,
                        // NEU: Lambda übergeben, um Dialog zu öffnen
                        onRoundClick = { round ->
                            roundToEdit = round
                        }
                    )
                }

                if (showNewRoundDialog) {
                    NewRoundScreen(
                        // GEÄNDERT: Übergibt 'allPlayers'
                        allPlayers = gameState.players,
                        onDismiss = { showNewRoundDialog = false },
                        // GEÄNDERT: Lambda-Signaturen
                        onSave = { gameType, player, partner, won, points, laufende, kontra, re, activePlayers ->
                            gameViewModel.addRound(gameType, player, partner, won, points, laufende, kontra, re, activePlayers)
                            showNewRoundDialog = false
                        },
                        onSaveRamsch = { scores, jungfrauPlayers, activePlayers ->
                            gameViewModel.addRamschRound(scores, jungfrauPlayers, activePlayers)
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

                // NEU: Aufruf des Spieler-Verwaltungs-Dialogs
                if (showPlayerManagementDialog) {
                    PlayerManagementDialog(
                        players = gameState.players,
                        onDismiss = { showPlayerManagementDialog = false },
                        onAddPlayer = { name -> gameViewModel.addPlayer(name) },
                        // GEÄNDERT: Ruft de/activate auf
                        onDeactivatePlayer = { player -> gameViewModel.deactivatePlayer(player) },
                        onActivatePlayer = { player -> gameViewModel.activatePlayer(player) }
                    )
                }

                // NEU: Dialog zur Rundenbearbeitung
                roundToEdit?.let { round ->
                    EditRoundDialog(
                        round = round,
                        allPlayers = gameState.players,
                        onDismiss = { roundToEdit = null },
                        onSave = { roundId, newPoints ->
                            gameViewModel.updateRound(roundId, newPoints)
                            roundToEdit = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    gameState: GameState,
    viewModel: GameViewModel,
    onRoundClick: (RoundResult) -> Unit // NEU: Callback für Klick
) {
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
        // GEÄNDERT: Übergibt 'players' und 'onRoundClick'
        RoundHistory(
            rounds = gameState.rounds,
            players = gameState.players,
            onRoundClick = onRoundClick
        )
    }
}

// GEÄNDERT: Verwendet FlowRow für dynamisches Layout
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScoreHeader(
    scores: Map<Player, Int>,
    players: List<Player>,
    onPlayerClick: (Player) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // GEÄNDERT: Row -> FlowRow
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            players.forEach { player ->
                // NEU: Modifier, um inaktive Spieler auszugrauen
                val modifier = if (player.isActive) Modifier else Modifier.alpha(0.6f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = modifier // GEÄNDERT: Modifier hier angewendet
                        .clickable { onPlayerClick(player) }
                        .padding(horizontal = 8.dp)
                        .width(IntrinsicSize.Min)
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = (scores[player] ?: 0).toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if ((scores[player] ?: 0) >= 0) Color(0xFF008000) else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun RoundHistory(
    rounds: List<RoundResult>,
    players: List<Player>,
    onRoundClick: (RoundResult) -> Unit // NEU: Callback für Klick
) {
    Text("Protokoll (Runde anklicken zum Bearbeiten)", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) // HINWEIS HINZUGEFÜGT
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
                    Text("Nr.", modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Text("Spiel", modifier = Modifier.weight(2f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    // GEÄNDERT: Dynamische Header-Spalten
                    players.forEach { player ->
                        // NEU: Inaktive Spalten ausgrauen
                        val modifier = if (player.isActive) Modifier.weight(1f) else Modifier.weight(1f).alpha(0.6f)
                        Text(
                            text = player.name.take(3),
                            modifier = modifier, // GEÄNDERT
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
                itemsIndexed(rounds.reversed()) { index, round ->
                    val roundNumber = rounds.size - index
                    // GEÄNDERT: Übergibt 'players' und 'onRoundClick'
                    RoundRow(
                        roundNumber = roundNumber,
                        round = round,
                        players = players,
                        onClick = { onRoundClick(round) } // NEU
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun RoundRow(
    roundNumber: Int,
    round: RoundResult,
    players: List<Player>,
    onClick: () -> Unit // NEU
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // NEU: Macht die Zeile klickbar
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$roundNumber",
            modifier = Modifier.weight(0.7f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Column(modifier = Modifier.weight(2f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(round.gameType.displayName, fontSize = 14.sp)

            if (round.gameType == GameType.RAMSCH && round.jungfrauPlayers.isNotEmpty()) {
                Text(
                    text = "(mit Jungfrau)",
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // NEU: Zeigt an, ob korrigiert
            if (round.gameType == GameType.KORREKTUR) {
                Text(
                    text = "(Korrigiert)",
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            round.declaringPlayer?.let {
                Text(
                    "von ${it.name}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // GEÄNDERT: Dynamische Punkte-Spalten
        players.forEach { player ->
            // Holt Punkte; 0 für Aussetzer
            val points = round.points[player] ?: 0
            // NEU: Inaktive Spalten ausgrauen (basierend auf globalem Status)
            val modifier = if (player.isActive) Modifier.weight(1f) else Modifier.weight(1f).alpha(0.6f)
            Text(
                text = if (points > 0) "+$points" else "$points",
                modifier = modifier, // GEÄNDERT
                textAlign = TextAlign.Center,
                color = when {
                    points > 0 -> Color(0xFF008000)
                    points < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant // Aussetzer sind grau
                },
                fontWeight = if(points != 0) FontWeight.SemiBold else FontWeight.Normal
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

// NEU: Dialog zur manuellen Korrektur einer Runde
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoundDialog(
    round: RoundResult,
    allPlayers: List<Player>,
    onDismiss: () -> Unit,
    onSave: (roundId: Long, newPoints: Map<Player, Int>) -> Unit
) {
    // Initialisiert die Scores für alle *aktuellen* Spieler,
    // nimmt den Wert aus der Runde, falls vorhanden, sonst 0.
    var scores by remember {
        mutableStateOf(
            allPlayers.associateWith { player ->
                (round.points.entries.find { it.key.id == player.id }?.value ?: 0).toString()
            }
        )
    }

    // Konvertiert die Text-Scores in Integer
    val scoreInts = allPlayers.associateWith {
        scores[it]?.toIntOrNull() ?: 0
    }
    // Berechnet die Summe aller eingetragenen Punkte
    val totalSum = scoreInts.values.sum()

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.extraLarge) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Runde korrigieren",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Spiel: ${round.gameType.displayName} (von ${round.declaringPlayer?.name ?: "N/A"})",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )


                allPlayers.forEach { player ->
                    // Spieler war in der *originalen* Runde aktiv?
                    // Prüft anhand der ID, falls sich Namen geändert haben
                    val playerWasActiveInRound = round.activePlayers.any { it.id == player.id }

                    OutlinedTextField(
                        value = scores[player] ?: "0",
                        onValueChange = { newValue ->
                            // Erlaube nur Zahlen und ein optionales Minus am Anfang
                            if (newValue.matches(Regex("-?\\d*"))) {
                                scores = scores.toMutableMap().apply { put(player, newValue) }
                            }
                        },
                        label = { Text(player.name) },
                        // Feld ist nur editierbar, wenn der Spieler in der originalen Runde aktiv war
                        enabled = playerWasActiveInRound,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .alpha(if (playerWasActiveInRound) 1f else 0.6f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        // Zeige 0 an, wenn der Spieler nicht aktiv war
                        placeholder = { if (!playerWasActiveInRound) Text("0") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Summe: $totalSum",
                    color = if (totalSum != 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (totalSum != 0) {
                    Text(
                        "Die Summe aller Punkte muss 0 ergeben.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

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
                            // Übergibt die Map mit den Player-Objekten als Key
                            onSave(round.id, scoreInts)
                        },
                        // Speichern nur erlauben, wenn die Summe 0 ist
                        enabled = totalSum == 0
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}


@Composable
fun PlayerManagementDialog(
    players: List<Player>,
    onDismiss: () -> Unit,
    onAddPlayer: (String) -> Unit,
    // GEÄNDERT: Funktionen zum De/Aktivieren
    onDeactivatePlayer: (Player) -> Unit,
    onActivatePlayer: (Player) -> Unit
) {
    var newPlayerName by remember { mutableStateOf("") }
    var isAddError by remember { mutableStateOf(false) }
    // NEU: Zählt aktive Spieler
    val activePlayerCount = players.count { it.isActive }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Spieler verwalten",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text("Spieler hinzufügen (Max. 7)", style = MaterialTheme.typography.titleMedium) // GEÄNDERT: Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPlayerName,
                        onValueChange = {
                            newPlayerName = it
                            isAddError = it.isBlank()
                        },
                        label = { Text("Neuer Name") },
                        modifier = Modifier.weight(1f),
                        isError = isAddError,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newPlayerName.isNotBlank()) {
                                onAddPlayer(newPlayerName)
                                newPlayerName = ""
                                isAddError = false
                            } else {
                                isAddError = true
                            }
                        },
                        enabled = players.size < 7 // GEÄNDERT: Limit auf 7
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Spieler verwalten (Min. 4 aktiv)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    // BUGFIX: itemsIndexed statt items
                    itemsIndexed(players) { _, player ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = player.name,
                                modifier = Modifier
                                    .weight(1f)
                                    .alpha(if (player.isActive) 1f else 0.6f)
                            )
                            // NEU: Logik für De/Aktivieren-Button
                            if (player.isActive) {
                                IconButton(
                                    onClick = { onDeactivatePlayer(player) },
                                    enabled = activePlayerCount > 4 // Deaktivieren nur > 4
                                ) {
                                    Icon(
                                        Icons.Default.Archive,
                                        contentDescription = "Archivieren",
                                        tint = if (activePlayerCount > 4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { onActivatePlayer(player) },
                                    enabled = activePlayerCount < 7 // GEÄNDERT: Limit auf 7
                                ) {
                                    Icon(
                                        Icons.Default.Unarchive,
                                        contentDescription = "Reaktivieren",
                                        tint = if (activePlayerCount < 6) Color(0xFF008000) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            // KORREKTUR: Diese Klammer war zu viel und wurde entfernt (war Zeile 516)
                        }
                    }

                    // KORREKTUR: Spacer und Row in 'item' Blöcke verpackt
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Schließen")
                            }
                        }
                    }
                }
            }
        }
    }
}

