package com.example

import kotlinx.serialization.Serializable

data class PotionsGameState(
    val ghostPotion: Int = (1..4).random(),
    val playerPicks: MutableMap<String, Int> = LinkedHashMap(),
    var isStarted: Boolean = false,
    var isFinished: Boolean = false,
    var newGhosts: List<String> = emptyList()
)

@Serializable
data class PotionsPicksResponse(
    val column1: List<String>,
    val column2: List<String>,
    val column3: List<String>,
    val ghostUsers: List<String>,
    val isFinished: Boolean
)