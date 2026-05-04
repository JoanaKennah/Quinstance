package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import java.io.InputStream

class CsvQuestionDataTest :
    StringSpec({
        "Reading normal.csv returns list of CsvQuestionData" {
            val questionData =
                listOf(
                    listOf("What is my name?", "Ethan", "Joana", "Minghao", "Emily", "0", "furthereducation"),
                    listOf("Is this a good quiz?", "Yes", "NO!", "", "", "1", "furthereducation")
                )

            val dataStream: InputStream = File("normal.csv").inputStream()
            dataStream shouldNotBe null

            val csvData = readCsv(dataStream)
            csvData shouldNotBe null

            for (i in 0..1) {
                csvData!![i].question shouldBe questionData[i][0]
                csvData[i].option1 shouldBe questionData[i][1]
                csvData[i].option2 shouldBe questionData[i][2]
                csvData[i].option3 shouldBe questionData[i][3]
                csvData[i].option4 shouldBe questionData[i][4]
                csvData[i].correctOption shouldBe questionData[i][5]
                csvData[i].keyStage shouldBe questionData[i][6]
            }
        }

        "Reading empty.csv (empty file) returns an empty list" {
            val dataStream: InputStream = File("empty.csv").inputStream()
            dataStream shouldNotBe null

            val csvData = readCsv(dataStream)
            csvData shouldNotBe null
            csvData!! shouldHaveSize 0
        }

        "Reading tiny_header.csv (header < 7) returns null" {
            val dataStream: InputStream = File("tiny_header.csv").inputStream()
            dataStream shouldNotBe null

            val csvData = readCsv(dataStream)
            csvData shouldBe null
        }

        "Reading huge_header.csv (header > 7) returns null" {
            val dataStream: InputStream = File("huge_header.csv").inputStream()
            dataStream shouldNotBe null

            val csvData = readCsv(dataStream)
            csvData shouldBe null
        }

        "Reading invalid_header.csv (header) returns null" {
            val dataStream: InputStream = File("invalid_header.csv").inputStream()
            dataStream shouldNotBe null

            val csvData = readCsv(dataStream)
            csvData shouldBe null
        }
    })