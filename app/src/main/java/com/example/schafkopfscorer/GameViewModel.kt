package com.example.schafkopfscorer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Holds the entire state of the current game
data class GameState(
    val players: List<Player> = emptyList(),
    val rounds: List<RoundResult> = emptyList(),
    val totalScores: Map<Player, Int> = emptyMap()
)

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    init {
        resetGame()
    }

    fun resetGame() {
        _gameState.value = GameState()
    }

    // NEU: Spieler hinzufügen (bis max. 7)
    fun addPlayer(name: String) {
        _gameState.update { currentState ->
            if (currentState.players.size >= 7) return@update currentState // GEÄNDERT: Limit auf 7
            val newId = (currentState.players.maxOfOrNull { it.id } ?: 0) + 1
            val newPlayer = Player(newId, name, isActive = true)
            // Berechnet Scores neu, falls Spieler hinzugefügt wird
            val newTotalScores = recalculateTotalScores(currentState.rounds, currentState.players + newPlayer)
            currentState.copy(
                players = currentState.players + newPlayer,
                totalScores = newTotalScores // Fügt neuen Spieler mit Score 0 hinzu
            )
        }
    }

    // GEÄNDERT: Spieler wird nur noch deaktiviert, nicht entfernt
    fun deactivatePlayer(playerToDeactivate: Player) {
        _gameState.update { currentState ->
            // Prüft, ob noch 4 Spieler aktiv bleiben
            val activePlayersCount = currentState.players.count { it.isActive }
            if (activePlayersCount <= 4) return@update currentState // Verhindert, dass weniger als 4 Spieler aktiv sind

            val newPlayers = currentState.players.map {
                if (it.id == playerToDeactivate.id) it.copy(isActive = false) else it
            }
            currentState.copy(players = newPlayers)
        }
    }

    // NEU: Reaktiviert einen Spieler
    fun activatePlayer(playerToActivate: Player) {
        _gameState.update { currentState ->
            val newPlayers = currentState.players.map {
                if (it.id == playerToActivate.id) it.copy(isActive = true) else it
            }
            currentState.copy(players = newPlayers)
        }
    }

    // NEU: Private Hilfsfunktion zur Neuberechnung aller Scores
    // Wird aufgerufen, wenn Runden hinzugefügt, bearbeitet oder Spieler hinzugefügt werden.
    private fun recalculateTotalScores(rounds: List<RoundResult>, players: List<Player>): Map<Player, Int> {
        val newTotalScores = players.associateWith { 0 }.toMutableMap()
        rounds.forEach { round ->
            round.points.forEach { (playerFromRound, points) ->
                // Finde den Spieler in der aktuellen Spielerliste anhand der ID
                // Wichtig, falls sich der Name geändert hat
                val currentPlayerState = players.find { it.id == playerFromRound.id }
                if (currentPlayerState != null) {
                    newTotalScores[currentPlayerState] = (newTotalScores[currentPlayerState] ?: 0) + points
                }
            }
        }
        return newTotalScores
    }


    // GEÄNDERT: Benötigt jetzt 'activePlayers'
    fun addRamschRound(
        scores: Map<Player, Int>,
        jungfrauPlayers: List<Player>,
        activePlayers: List<Player>
    ) {
        val tarif = 20

        val maxScore = scores.values.maxOrNull() ?: 0
        // Sollte nicht passieren, da "Speichern" deaktiviert ist, wenn totalPoints < 1
        if (maxScore == 0) return

        val losers = scores.filter { it.value == maxScore }.keys.toList()
        // GEÄNDERT: Durchmarsch-Grenze von 120 auf >= 90 gesenkt
        val isDurchmarsch = maxScore >= 90 && losers.size == 1

        val finalPoints = mutableMapOf<Player, Int>()

        if (isDurchmarsch) {
            val winner = losers.first()
            // GEÄNDERT: Nutzt die komplette Spielerliste
            _gameState.value.players.forEach { player ->
                finalPoints[player] = when {
                    player.id == winner.id -> tarif * 3
                    player in activePlayers -> -tarif
                    else -> 0 // Aussetzer oder Inaktive
                }
            }
        } else {
            val winners = activePlayers.filter { it !in losers }
            var totalLoss = 0
            winners.forEach { winner ->
                totalLoss += if (winner in jungfrauPlayers) tarif * 2 else tarif
            }
            val pointsPerLoser = if (losers.isNotEmpty()) -totalLoss / losers.size else 0

            // GEÄNDERT: Nutzt die komplette Spielerliste
            _gameState.value.players.forEach { player ->
                finalPoints[player] = when {
                    player in losers -> pointsPerLoser
                    player in winners -> if (player in jungfrauPlayers) tarif * 2 else tarif
                    else -> 0 // Aussetzer oder Inaktive
                }
            }
        }

        val newRound = RoundResult(
            id = System.currentTimeMillis(), // NEU
            gameType = GameType.RAMSCH,
            declaringPlayer = null,
            partnerPlayer = null,
            points = finalPoints,
            jungfrauPlayers = jungfrauPlayers,
            activePlayers = activePlayers // NEU
        )

        _gameState.update { currentState ->
            val newRounds = currentState.rounds + newRound
            // GEÄNDERT: Ruft Neuberechnung auf
            val newTotalScores = recalculateTotalScores(newRounds, currentState.players)
            currentState.copy(
                rounds = newRounds,
                totalScores = newTotalScores
            )
        }
    }


    // GEÄNDERT: Benötigt jetzt 'activePlayers'
    fun addRound(
        gameType: GameType,
        declaringPlayer: Player,
        partnerPlayer: Player?,
        playerPartyWon: Boolean, // Kommt von der UI (Switch)
        playerPartyPoints: Int,
        laufende: Int,
        kontra: Boolean,
        re: Boolean,
        activePlayers: List<Player>
    ) {
        if (gameType == GameType.RAMSCH) return

        val tarif = when (gameType) {
            GameType.RUFSPIEL -> 20
            GameType.FARBSOLO -> 50
            GameType.WENZ -> 50
            GameType.BETTEL -> 50
            GameType.RAMSCH -> 0
            GameType.KORREKTUR -> 0 // Korrektur sollte hier nicht erstellt werden
        }

        val actualPlayerPartyWon = if (gameType != GameType.BETTEL) {
            playerPartyPoints > 60
        } else {
            playerPartyWon
        }

        var grundtarif = tarif
        if (gameType != GameType.BETTEL) {
            if (actualPlayerPartyWon) {
                if (playerPartyPoints > 90) grundtarif += 10 // Schneider
                if (playerPartyPoints == 120) grundtarif += 10 // Schwarz
            } else {
                if (playerPartyPoints < 30) grundtarif += 10 // Schneider
                if (playerPartyPoints == 0) grundtarif += 10 // Schwarz
            }
            grundtarif += (laufende * 10)
        }

        if (kontra) grundtarif *= 2
        if (re) grundtarif *= 2

        val playerParty = when (gameType) {
            GameType.RUFSPIEL -> listOf(declaringPlayer, partnerPlayer)
            else -> listOf(declaringPlayer)
        }.filterNotNull()

        val opponentParty = activePlayers.filter { it !in playerParty }

        val winners = if (actualPlayerPartyWon) playerParty else opponentParty
        val losers = if (actualPlayerPartyWon) opponentParty else playerParty

        val finalPoints = mutableMapOf<Player, Int>()
        // GEÄNDERT: Nutzt die komplette Spielerliste
        _gameState.value.players.forEach { player ->
            finalPoints[player] = when (player) {
                in winners -> if (winners.size == 1) grundtarif * 3 else grundtarif
                in losers -> if (losers.size == 1) -grundtarif * 3 else -grundtarif
                else -> 0 // Aussetzer oder Inaktive
            }
        }

        val newRound = RoundResult(
            id = System.currentTimeMillis(), // NEU
            gameType = gameType,
            declaringPlayer = declaringPlayer,
            partnerPlayer = partnerPlayer,
            points = finalPoints,
            jungfrauPlayers = emptyList(),
            activePlayers = activePlayers // NEU
        )

        _gameState.update { currentState ->
            val newRounds = currentState.rounds + newRound
            // GEÄNDERT: Ruft Neuberechnung auf
            val newTotalScores = recalculateTotalScores(newRounds, currentState.players)
            currentState.copy(
                rounds = newRounds,
                totalScores = newTotalScores
            )
        }
    }

    // NEU: Funktion zum Bearbeiten einer existierenden Runde
    fun updateRound(roundId: Long, newManualPoints: Map<Player, Int>) {
        _gameState.update { currentState ->
            val roundToUpdate = currentState.rounds.find { it.id == roundId } ?: return@update currentState

            // Stellt sicher, dass die Map alle Spieler enthält
            val completeNewPoints = currentState.players.associateWith { player ->
                newManualPoints[player] ?: 0 // Nimm den neuen Punktwert oder 0
            }

            val updatedRound = roundToUpdate.copy(
                points = completeNewPoints,
                gameType = GameType.KORREKTUR // Setzt Spieltyp auf "Korrektur"
            )

            val newRounds = currentState.rounds.map {
                if (it.id == roundId) updatedRound else it
            }
            // GEÄNDERT: Ruft Neuberechnung auf
            val newTotalScores = recalculateTotalScores(newRounds, currentState.players)

            currentState.copy(
                rounds = newRounds,
                totalScores = newTotalScores
            )
        }
    }

    fun updatePlayerName(playerToUpdate: Player, newName: String) {
        _gameState.update { currentState ->
            val updatedPlayer = playerToUpdate.copy(name = newName)
            val newPlayers = currentState.players.map {
                if (it.id == playerToUpdate.id) updatedPlayer else it
            }

            // WICHTIG: Muss die Player-Objekte in allen Listen ersetzen

            val newTotalScores = currentState.totalScores.mapKeys { (player, _) ->
                if (player.id == playerToUpdate.id) updatedPlayer else player
            }
            val newRounds = currentState.rounds.map { round ->
                round.copy(
                    declaringPlayer = if (round.declaringPlayer?.id == playerToUpdate.id) updatedPlayer else round.declaringPlayer,
                    partnerPlayer = if (round.partnerPlayer?.id == playerToUpdate.id) updatedPlayer else round.partnerPlayer,
                    points = round.points.mapKeys { (player, _) ->
                        if (player.id == playerToUpdate.id) updatedPlayer else player
                    },
                    activePlayers = round.activePlayers.map {
                        if (it.id == playerToUpdate.id) updatedPlayer else it
                    },
                    jungfrauPlayers = round.jungfrauPlayers.map {
                        if (it.id == playerToUpdate.id) updatedPlayer else it
                    }
                )
            }

            // Berechnet Scores neu, um Konsistenz sicherzustellen
            val finalTotalScores = recalculateTotalScores(newRounds, newPlayers)

            currentState.copy(
                players = newPlayers,
                totalScores = finalTotalScores,
                rounds = newRounds
            )
        }
    }
}

