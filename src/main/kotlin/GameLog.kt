package data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class MoveType {
    PLAYER_ADDED,
    GAME_STARTED,
    CHARACTER_CALLED,
    CHARACTER_SKIPPED,
    ABILITY_USED,
    GOLD_TAKEN,
    CARD_DRAWN,
    DISTRICT_BUILT,
    DISTRICT_DESTROYED,
    HAND_SWAPPED,
    PLAYER_ROBBED,
    PLAYER_KILLED,
    GAME_OVER
}

 data class GameLogEntry(
    val round: Int,
    val characterName: String?,
    val playerName: String?,
    val type: MoveType,
    val description: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    fun formatTime(): String =
        timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
}

