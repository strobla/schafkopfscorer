// =================================================================================
// DataModel.kt - Datenklassen für das Spiel
// =================================================================================

package com.example.schafkopfscorer

import androidx.compose.runtime.Immutable

// Enum für die verschiedenen Spielarten
enum class GameType(val displayName: String, val isSolo: Boolean, val baseTariff: Int) {
    RUFSPIEL("Rufspiel", isSolo = false, baseTariff = 20),
    BETTEL("Bettel", isSolo = true, baseTariff = 50),
    WENZ("Wenz", isSolo = true, baseTariff = 50),
    FARBSOLO("Farbsolo", isSolo = true, baseTariff = 50)
}

// Repräsentiert einen Spieler
@Immutable
data class Player(val id: Int, val name: String)

// Repräsentiert eine abgeschlossene Spielrunde
@Immutable
data class RoundResult(
    val roundNumber: Int,
    val gameType: GameType,
    val declaringPlayer: Player,
    val partnerPlayer: Player?, // Nur für Rufspiel relevant
    val playerPartyPoints: Int,
    val laufende: Int,
    val isPlayerPartySchneider: Boolean,
    val isPlayerPartySchwarz: Boolean,
    val kontra: Boolean,
    val re: Boolean,
    val points: Map<Player, Int> // Die Punktverteilung für diese Runde
)