package com.example

data class PotionsGameState(
    val ghostPotion: Int = (1..4).random(),
    val playerPicks: MutableMap<String, Int> = LinkedHashMap(),
    var isFinished: Boolean = false,
    var newGhosts: List<String> = emptyList()
)