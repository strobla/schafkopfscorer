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
        val initialPlayers = listOf(
            Player(1, "Andi"),
            Player(2, "Babsi"),
            Player(3, "Chris"),
            Player(4, "Dani")
        )
        _gameState.value = GameState(
            players = initialPlayers,
            rounds = emptyList(),
            totalScores = initialPlayers.associateWith { 0 }
        )
    }

    // GEÄNDERT: Die Berechnungslogik für die Punkteverteilung ist neu
    fun addRamschRound(scores: Map<Player, Int>, jungfrauPlayers: List<Player>) {
        val tarif = 20 // Fester Tarif für Ramsch

        val maxScore = scores.values.maxOrNull() ?: 0
        val losers = scores.filter { it.value == maxScore }.keys.toList()

        // Ein "Durchmarsch" (ein Spieler bekommt alle 120 Punkte) ist ein Gewinn.
        // Die Jungfrau-Regel greift hier nicht.
        val isDurchmarsch = maxScore == 120 && losers.size == 1

        val finalPoints = mutableMapOf<Player, Int>()

        if (isDurchmarsch) {
            val winner = losers.first()
            _gameState.value.players.forEach { player ->
                finalPoints[player] = if (player.id == winner.id) {
                    tarif * 3 // Gewinner bekommt +60
                } else {
                    -tarif // Verlierer bekommen -20
                }
            }
        } else {
            // NEUE LOGIK: Punkte werden basierend auf dem Jungfrau-Status der Gewinner verteilt.
            val winners = _gameState.value.players.filter { it !in losers }
            var totalLoss = 0

            winners.forEach { winner ->
                val pointsWon = if (winner in jungfrauPlayers) {
                    tarif * 2 // Jungfrau-Gewinner erhält doppelte Punkte
                } else {
                    tarif // Normaler Gewinner erhält einfache Punkte
                }
                finalPoints[winner] = pointsWon
                totalLoss += pointsWon
            }

            if (losers.isNotEmpty()) {
                val pointsPerLoser = -totalLoss / losers.size
                losers.forEach { loser ->
                    finalPoints[loser] = pointsPerLoser
                }
            }
            // Falls alle Spieler Verlierer sind (z.B. alle 30), werden 0 Punkte vergeben.
            else if (winners.isEmpty() && losers.size == 4) {
                _gameState.value.players.forEach { finalPoints[it] = 0 }
            }
        }

        val newRound = RoundResult(
            gameType = GameType.RAMSCH,
            declaringPlayer = null, // Kein einzelner Spieler bei Ramsch
            partnerPlayer = null,
            points = finalPoints,
            jungfrauPlayers = jungfrauPlayers // Speichert die Liste der Jungfrau-Spieler
        )

        _gameState.update { currentState ->
            val newRounds = currentState.rounds + newRound
            val newTotalScores = mutableMapOf<Player, Int>()
            currentState.players.forEach { player ->
                newTotalScores[player] = (currentState.totalScores[player] ?: 0) + (finalPoints[player] ?: 0)
            }
            currentState.copy(
                rounds = newRounds,
                totalScores = newTotalScores
            )
        }
    }


    fun addRound(
        gameType: GameType,
        declaringPlayer: Player,
        partnerPlayer: Player?,
        playerPartyWon: Boolean,
        playerPartyPoints: Int,
        laufende: Int,
        kontra: Boolean,
        re: Boolean
    ) {
        if (gameType == GameType.RAMSCH) return // Sollte von addRamschRound behandelt werden

        val tarif = when (gameType) {
            GameType.RUFSPIEL -> 20
            GameType.FARBSOLO -> 60
            GameType.WENZ -> 60
            GameType.BETTEL -> 40
            GameType.RAMSCH -> 0 // Sollte nicht vorkommen
        }

        var grundtarif = tarif
        if (gameType != GameType.BETTEL) {
            if (playerPartyWon) {
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

        val opponentParty = _gameState.value.players.filter { it !in playerParty }

        val winners = if (playerPartyWon) playerParty else opponentParty
        val losers = if (playerPartyWon) opponentParty else playerParty

        val finalPoints = mutableMapOf<Player, Int>()
        _gameState.value.players.forEach { player ->
            finalPoints[player] = when (player) {
                in winners -> if (winners.size == 1) grundtarif * 3 else grundtarif
                in losers -> if (losers.size == 1) -grundtarif * 3 else -grundtarif
                else -> 0
            }
        }

        val newRound = RoundResult(
            gameType = gameType,
            declaringPlayer = declaringPlayer,
            partnerPlayer = partnerPlayer,
            points = finalPoints,
            jungfrauPlayers = emptyList() // Nicht anwendbar für Standardspiele
        )

        _gameState.update { currentState ->
            val newRounds = currentState.rounds + newRound
            val newTotalScores = mutableMapOf<Player, Int>()
            currentState.players.forEach { player ->
                newTotalScores[player] = (currentState.totalScores[player] ?: 0) + (finalPoints[player] ?: 0)
            }
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
                    jungfrauPlayers = round.jungfrauPlayers.map {
                        if (it.id == playerToUpdate.id) updatedPlayer else it
                    }
                )
            }

            currentState.copy(
                players = newPlayers,
                totalScores = newTotalScores,
                rounds = newRounds
            )
        }
    }
}

