package com.orange.molkky

import androidx.lifecycle.ViewModel

class PlayersViewModel : ViewModel() {
    var players: MutableList<PlayerInfo> = mutableListOf()
}