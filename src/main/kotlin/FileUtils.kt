package com.example

import java.io.InputStream

data class ReadQuestionData (
    val question: String,
    val correctAnswer: String,
    val option2: String,
    val option3: String,
    val option4: String
)

fun readCsv(inputStream: InputStream): List<ReadQuestionData> {
    val reader = inputStream.bufferedReader()
    val header = reader.readLine()
    return reader.lineSequence()
        .filter { it.isNotBlank() }
        .map {
            val (question, correctAnswer, option2, option3, option4) = it.split(',', ignoreCase = false, limit = 3)
            ReadQuestionData(
                question.trim(),
                correctAnswer.trim(),
                option2.trim(),
                option3.trim(),
                option4.trim()
            )
        }.toList()
}