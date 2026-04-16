package com.example

import java.io.InputStream

/**
 * Question data from a .csv file, includes Question info (question, keyStage) and Answer info (correctAnswer, option2,
 * option3, option4)
 */
data class ReadQuestionData (
    val question: String,
    val option1: String,
    val option2: String,
    val option3: String?,
    val option4: String?,
    val correctOption: String, // Must be converted to an integer when added to Answer.
    val keyStage: String
)

/**
 * Reads file input and converts data into ReadQuestionData so they can be turned into Questions. Returns null
 * if data cannot be read correctly or file is empty.
 * MAY REQUIRE MORE ERROR HANDLING
 */
fun readCsv(inputStream: InputStream): List<ReadQuestionData>? {
    val reader = inputStream.bufferedReader()
    val header = reader.readLine()

    // If there is no header (file is empty)
    if (header.isEmpty()) return null
    try {
        val headerData = header.split(',', ignoreCase = false, limit = 7)

        // If header has less than 7 items or isn't correct format
        if (headerData.size != 7) return null
    } catch (e: Exception) {
        // If header has more data than 7 items.
        println("Exception while reading CSV")
        return null
    }
    // Check to see if header is accurate.
    println("HELLO")
    try {
        val questionList = reader.lineSequence()
            .filter { it.isNotBlank() }
            .map {
                // Had to split data into two due to an error with there not being a 7th component for splitting data.
                val data = it.split(',', ignoreCase = false, limit = 7)
                val (question, option1, option2, option3) = data.chunked(4)[0]
                val (option4, correctOption, keyStage, _) = data.chunked(4)[1]

                // Creates ReadQuestionData object to make storing this information simpler.
                ReadQuestionData(
                    question.trim(),
                    option1.trim(),
                    option2.trim(),
                    option3.trim(),
                    option4.trim(),
                    correctOption.trim(),
                    keyStage.trim()
                )
            }.toList()
        return questionList
    } catch (e: Exception) {
        println("Exception while reading CSV")
        return null
    }
}