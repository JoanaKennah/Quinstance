package com.example


data class RoomsGameState(
    val playerPicks: MutableMap<String, String> = LinkedHashMap(),      //same as pots, preserves insertion order
    var isFinished: Boolean = false, 
    var ghostRoom: String = "",                                         //just init here, decided after picks
    var newGhosts: List<String> = emptyList(),
    var allSafe: Boolean = false
)