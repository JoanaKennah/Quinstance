package com.example

data class WordsGameState(
    val grid: List<String> = WordsQuestions.generateGrid(),
    val playerWords: MutableMap<String, String> = mutableMapOf(),
    val validatedWords: MutableMap<String, String> = mutableMapOf(),
    var ghostPlayers: List<String> = emptyList(),
    var isFinished: Boolean = false
)

//build the letter pool for grid - scrabble weighting
object WordsQuestions {
    private val letterPool: List<String> = buildList {
        repeat(9)  { add("A") }
        repeat(2)  { add("B") }
        repeat(2)  { add("C") }
        repeat(4)  { add("D") }
        repeat(12) { add("E") }
        repeat(2)  { add("F") }
        repeat(3)  { add("G") }
        repeat(2)  { add("H") }
        repeat(9)  { add("I") }
        repeat(1)  { add("J") }
        repeat(1)  { add("K") }
        repeat(4)  { add("L") }
        repeat(2)  { add("M") }
        repeat(6)  { add("N") }
        repeat(8)  { add("O") }
        repeat(2)  { add("P") }
        repeat(1)  { add("Q") }
        repeat(6)  { add("R") }
        repeat(4)  { add("S") }
        repeat(6)  { add("T") }
        repeat(4)  { add("U") }
        repeat(2)  { add("V") }
        repeat(2)  { add("W") }
        repeat(1)  { add("X") }
        repeat(2)  { add("Y") }
        repeat(1)  { add("Z") }
    }
}

//generate a list of 25 letters to populate grid
fun generateGrid(): List<String> = letterPool.shuffled().take(25)