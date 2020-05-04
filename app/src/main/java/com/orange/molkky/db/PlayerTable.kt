package com.orange.molkky.db

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(
    tableName = "player_table"
)

data class PlayerTable(
    @PrimaryKey(autoGenerate = true)
    var playerId: Long = 0L,
    var name: String = "",
    var order: Int = 0,
    var scores: MutableList<Int?> = mutableListOf(),
    @Ignore var total: Int = 0,
    @Ignore var playingState: PlayingState = PlayingState.PLAYING,
    @Ignore var active: Boolean = false,
    @Ignore var roundWon: Int = 0,
    @Ignore var rank: Int = 0
) {
    enum class PlayingState {
        PLAYING,
        WON,
        LOST
    }
}