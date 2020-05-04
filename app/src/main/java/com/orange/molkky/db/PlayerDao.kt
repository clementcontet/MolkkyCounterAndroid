package com.orange.molkky.db

import androidx.room.*
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface PlayerDao {
    @Insert
    fun insertPlayer(player: PlayerTable): Single<Long>

    @Query("SELECT * FROM player_table")
    fun getPlayers(): Flowable<MutableList<PlayerTable>>

    @Update
    fun updatePlayer(player: PlayerTable): Single<Int>

    @Transaction
    @Update
    fun updatePlayers(players: List<PlayerTable>): Single<Int>
    
    @Query("DELETE FROM player_table WHERE playerId = :playerId")
    fun deletePlayer(playerId: Long): Single<Int>
}