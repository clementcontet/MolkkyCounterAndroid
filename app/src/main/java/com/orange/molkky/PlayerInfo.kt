package com.orange.molkky

data class PlayerInfo(
    var name: String,
    var scores: MutableList<Int?> = mutableListOf(),
    var total: Int = 0,
    var playingState: PlayingState = PlayingState.PLAYING,
    var active: Boolean = false
) {
    enum class PlayingState {
        PLAYING,
        WON,
        LOST
    }
}
