package com.example.schafkopfscorer

data class Player(
    val id: Int,
    val name: String,
    val isActive: Boolean = true // NEU: Status des Spielers
)

enum class GameType(val displayName: String) {
    RUFSPIEL("Rufspiel"),
    WENZ("Wenz"),
    FARBSOLO("Farbsolo"),
    BETTEL("Bettel"),
    RAMSCH("Ramsch"),
    KORREKTUR("Korrektur") // NEU: Für manuelle Änderungen
}

data class RoundResult(
    val id: Long = System.currentTimeMillis(), // NEU: Eindeutige ID
    val gameType: GameType,
    val declaringPlayer: Player?,
    val partnerPlayer: Player?,
    val points: Map<Player, Int>,
    val activePlayers: List<Player>, // NEU: Speichert, wer gespielt hat
    val jungfrauPlayers: List<Player> = emptyList()
)
