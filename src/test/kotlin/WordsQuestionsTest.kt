package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class WordsQuestionsTest :
    StringSpec({

        "WordsQuestions.generateGrid returns exactly 25 letters - grid size is correct" {
            val grid = WordsQuestions.generateGrid()
            grid.size shouldBe 25
        }

        "WordsQuestions.generateGrid all entries are single uppercase letters - grid contents valid" {
            val grid = WordsQuestions.generateGrid()
            val validLetters = ('A'..'Z').map { it.toString() }
            for (letter in grid) {
                letter.length shouldBe 1
                validLetters.contains(letter) shouldBe true
            }
        }

        "WordsQuestions.generateGrid no letter exceeds its Scrabble frequency - letter pool weighted correctly" {
            val grid = WordsQuestions.generateGrid()
            val maxFrequencies =
                mapOf(
                    "A" to 9,
                    "B" to 2,
                    "C" to 2,
                    "D" to 4,
                    "E" to 12,
                    "F" to 2,
                    "G" to 3,
                    "H" to 2,
                    "I" to 9,
                    "J" to 1,
                    "K" to 1,
                    "L" to 4,
                    "M" to 2,
                    "N" to 6,
                    "O" to 8,
                    "P" to 2,
                    "Q" to 1,
                    "R" to 6,
                    "S" to 4,
                    "T" to 6,
                    "U" to 4,
                    "V" to 2,
                    "W" to 2,
                    "X" to 1,
                    "Y" to 2,
                    "Z" to 1,
                )
            val gridCounts = grid.groupingBy { it }.eachCount()
            for ((letter, count) in gridCounts) {
                (count <= (maxFrequencies[letter] ?: Int.MAX_VALUE)) shouldBe true
            }
        }
    })
