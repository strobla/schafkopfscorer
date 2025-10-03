package com.example.schafkopfscorer

data class Player(
    val id: Int,
    val name: String
)

enum class GameType(val displayName: String) {
    RUFSPIEL("Rufspiel"),
    WENZ("Wenz"),
    FARBSOLO("Farbsolo"),
    BETTEL("Bettel"),
    RAMSCH("Ramsch")
}

data class RoundResult(
    val gameType: GameType,
    val declaringPlayer: Player?,
    val partnerPlayer: Player?,
    val points: Map<Player, Int>,
    // NEU: Liste für Spieler, die "Jungfrau" geblieben sind
    val jungfrauPlayers: List<Player> = emptyList()
)

