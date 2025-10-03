// =================================================================================
// GameViewModel.kt - Geschäftslogik und Zustandsverwaltung
// =================================================================================

package com.example.schafkopfscorer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Hält den gesamten Zustand des aktuellen Spiels
data class GameState(
    val players: List<Player> = emptyList(),
    val rounds: List<RoundResult> = emptyList(),
    val totalScores: Map<Player, Int> = emptyMap()
)

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    init {
        // Initializes the game with four default players
        resetGame()
    }

    // Resets the game to its initial state
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

    // Fügt eine neue Runde hinzu und berechnet die Punkte
    fun addRound(
        gameType: GameType,
        declaringPlayer: Player,
        partnerPlayer: Player?, // kann null sein, wenn kein Rufspiel
        playerPartyWon: Boolean, // NEU: Gewinnlogik wird jetzt als Parameter übergeben
        playerPartyPoints: Int, // Wird für die Anzeige im Protokoll beibehalten
        laufende: Int,
        kontra: Boolean,
        re: Boolean
    ) {
        // 1. Initialisierung der Boni
        var isPlayerPartySchneider = false
        var isPlayerPartySchwarz = false

        // 2. Grundwert und Boni berechnen
        var roundValue = gameType.baseTariff
        val bonusTariff = 10 // Fester Tarif für Schneider/Schwarz/Laufende im MVP

        // NEU: Boni wie Schneider, Schwarz und Laufende gelten nicht für Bettel
        if (gameType != GameType.BETTEL) {
            val opponentPartyPoints = 120 - playerPartyPoints
            val isOpponentPartySchneider = !playerPartyWon && playerPartyPoints < 31
            val isOpponentPartySchwarz = !playerPartyWon && playerPartyPoints == 0

            isPlayerPartySchneider = playerPartyWon && opponentPartyPoints < 30
            isPlayerPartySchwarz = playerPartyWon && opponentPartyPoints == 0

            if (isPlayerPartySchneider || isOpponentPartySchneider) {
                roundValue += bonusTariff
            }
            if (isPlayerPartySchwarz || isOpponentPartySchwarz) {
                // Annahme: Schwarz beinhaltet Schneider
                roundValue += bonusTariff
            }
            // Annahme: Laufende ab 3 (bei Solo/Ruf) bzw. 2 (bei Wenz)
            val laufendeThreshold = if (gameType == GameType.WENZ) 2 else 3
            if (laufende >= laufendeThreshold) {
                roundValue += laufende * bonusTariff
            }
        }

        // 3. Multiplikatoren anwenden (gelten auch für Bettel)
        if (kontra) roundValue *= 2
        if (re) roundValue *= 2

        // 4. Punkte verteilen
        val finalPoints = mutableMapOf<Player, Int>()
        val allPlayers = _gameState.value.players

        if (gameType.isSolo) {
            // Solo-Spiel (inkl. Bettel): 1 gegen 3
            val soloPlayerValue = if (playerPartyWon) roundValue * 3 else -roundValue * 3
            val opponentValue = if (playerPartyWon) -roundValue else roundValue

            allPlayers.forEach { player ->
                finalPoints[player] = if (player == declaringPlayer) soloPlayerValue else opponentValue
            }
        } else {
            // Partnerspiel (Rufspiel): 2 gegen 2
            val winnerValue = if (playerPartyWon) roundValue else -roundValue
            val loserValue = -winnerValue

            val playerParty = listOf(declaringPlayer, partnerPlayer)

            allPlayers.forEach { player ->
                finalPoints[player] = if (player in playerParty) winnerValue else loserValue
            }
        }

        // 5. Neuen Spielzustand erstellen und UI aktualisieren
        val newRound = RoundResult(
            roundNumber = _gameState.value.rounds.size + 1,
            gameType = gameType,
            declaringPlayer = declaringPlayer,
            partnerPlayer = partnerPlayer,
            playerPartyPoints = playerPartyPoints,
            laufende = laufende,
            isPlayerPartySchneider = isPlayerPartySchneider || isPlayerPartySchwarz,
            isPlayerPartySchwarz = isPlayerPartySchwarz,
            kontra = kontra,
            re = re,
            points = finalPoints
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

    /**
     * Updates the name of a player across the entire game state.
     * It rebuilds players, scores, and rounds to maintain data integrity.
     */
    fun updatePlayerName(playerToUpdate: Player, newName: String) {
        _gameState.update { currentState ->
            val updatedPlayer = playerToUpdate.copy(name = newName)

            // Create the new list of players
            val newPlayers = currentState.players.map {
                if (it.id == playerToUpdate.id) updatedPlayer else it
            }

            // Rebuild the totalScores map with the new player object as key
            val newTotalScores = currentState.totalScores.mapKeys { (player, _) ->
                if (player.id == playerToUpdate.id) updatedPlayer else player
            }

            // Rebuild the rounds list, updating player objects within each RoundResult
            val newRounds = currentState.rounds.map { round ->
                round.copy(
                    declaringPlayer = if (round.declaringPlayer.id == playerToUpdate.id) updatedPlayer else round.declaringPlayer,
                    partnerPlayer = if (round.partnerPlayer?.id == playerToUpdate.id) updatedPlayer else round.partnerPlayer,
                    points = round.points.mapKeys { (player, _) ->
                        if (player.id == playerToUpdate.id) updatedPlayer else player
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
