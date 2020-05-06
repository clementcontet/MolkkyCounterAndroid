package com.orange.molkky.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromArray(scoresList: List<Int?>): String {
        return scoresList.joinToString(",") { it?.toString() ?: "-" }
    }

    @TypeConverter
    fun toArray(scoresString: String): MutableList<Int?> {
        return if (scoresString.isEmpty()) mutableListOf()
        else scoresString.split(",").map { it.toIntOrNull() }.toMutableList()
    }
}