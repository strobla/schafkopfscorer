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

    // NEU: Spieler hinzufügen (bis max. 7)
    fun addPlayer(name: String) {
        _gameState.update { currentState ->
            if (currentState.players.size >= 7) return // GEÄNDERT: Limit auf 7
            val newId = (currentState.players.maxOfOrNull { it.id } ?: 0) + 1
            val newPlayer = Player(newId, name, isActive = true)
            currentState.copy(
                players = currentState.players + newPlayer,
                totalScores = currentState.totalScores + (newPlayer to 0)
            )
        }
    }

    // GEÄNDERT: Spieler wird nur noch deaktiviert, nicht entfernt
    fun deactivatePlayer(playerToDeactivate: Player) {
        _gameState.update { currentState ->
            // Prüft, ob noch 4 Spieler aktiv bleiben
            val activePlayersCount = currentState.players.count { it.isActive }
            if (activePlayersCount <= 4) return // Verhindert, dass weniger als 4 Spieler aktiv sind

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
        val isDurchmarsch = maxScore == 120 && losers.size == 1

        val finalPoints = mutableMapOf<Player, Int>()

        if (isDurchmarsch) {
            val winner = losers.first()
            _gameState.value.players.forEach { player ->
                finalPoints[player] = when {
                    player.id == winner.id -> tarif * 3
                    player in activePlayers -> -tarif
                    else -> 0 // Aussetzer oder Inaktive
                }
            }
        } else {
            // ** START DER KORREKTUR **
            // Normale Ramsch-Runde (kein Durchmarsch)

            // 1. Finde die Gewinner
            val winners = activePlayers.filter { it !in losers }

            // 2. Berechne den Gesamtverlust basierend auf den Gewinnern
            var totalLoss = 0
            winners.forEach { winner ->
                totalLoss += if (winner in jungfrauPlayers) tarif * 2 else tarif
            }

            // 3. Berechne die Punkte pro Verlierer (sicherstellen, dass nicht durch 0 geteilt wird)
            val pointsPerLoser = if (losers.isNotEmpty()) -totalLoss / losers.size else 0

            // 4. Weise JEDEM Spieler Punkte zu (robustere Methode)
            _gameState.value.players.forEach { player ->
                finalPoints[player] = when {
                    player in losers -> pointsPerLoser
                    player in winners -> if (player in jungfrauPlayers) tarif * 2 else tarif
                    else -> 0 // Aussetzer oder Inaktive
                }
            }
            // ** ENDE DER KORREKTUR **
        }

        val newRound = RoundResult(
            gameType = GameType.RAMSCH,
            declaringPlayer = null,
            partnerPlayer = null,
            points = finalPoints,
            jungfrauPlayers = jungfrauPlayers
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


    // GEÄNDERT: Benötigt jetzt 'activePlayers'
    fun addRound(
        gameType: GameType,
        declaringPlayer: Player,
        partnerPlayer: Player?,
        playerPartyWon: Boolean,
        playerPartyPoints: Int,
        laufende: Int,
        kontra: Boolean,
        re: Boolean,
        activePlayers: List<Player>
    ) {
        if (gameType == GameType.RAMSCH) return

        val tarif = when (gameType) {
            GameType.RUFSPIEL -> 20
            GameType.FARBSOLO -> 60
            GameType.WENZ -> 60
            GameType.BETTEL -> 40
            GameType.RAMSCH -> 0
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

        // GEÄNDERT: Gegner werden aus 'activePlayers' bestimmt
        val opponentParty = activePlayers.filter { it !in playerParty }

        val winners = if (playerPartyWon) playerParty else opponentParty
        val losers = if (playerPartyWon) opponentParty else playerParty

        val finalPoints = mutableMapOf<Player, Int>()
        _gameState.value.players.forEach { player ->
            finalPoints[player] = when (player) {
                in winners -> if (winners.size == 1) grundtarif * 3 else grundtarif
                in losers -> if (losers.size == 1) -grundtarif * 3 else -grundtarif
                else -> 0 // Aussetzer oder Inaktive
            }
        }

        val newRound = RoundResult(
            gameType = gameType,
            declaringPlayer = declaringPlayer,
            partnerPlayer = partnerPlayer,
            points = finalPoints,
            jungfrauPlayers = emptyList()
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


