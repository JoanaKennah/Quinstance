package com.example

import java.io.InputStream

/**
 * Question data from a .csv file,
 * @param question Question text
 * @param option1 First possible answer for a question, cannot be empty
 * @param option2 Second possible answer for a question, can be empty
 * @param option3 Third possible answer for a question, can be empty
 * @param option4 Fourth possible answer for a question, can be empty
 * @param correctOption Value from 0 to 3 indicating which option is correct. This is saved as a String but is converted to an Integer later
 */
data class CsvQuestionData(
    val question: String,
    val option1: String,
    val option2: String,
    val option3: String?,
    val option4: String?,
    val correctOption: String,
    val keyStage: String,
)

/**
 * Reads file input and converts data into ReadQuestionData so they can be turned into Questions. Returns null
 * if data cannot be read correctly or file is empty. Returns a DISTINCT list of ReadQuestionData
 * @param inputStream Stream of data retrieved from the User
 * @return List of CsvQuestionData objects or null if there was an error in reading
 */
fun readCsv(inputStream: InputStream): List<CsvQuestionData>? {
    val reader = inputStream.bufferedReader()

    // If file is empty (aka no header), return an empty list.
    val header = reader.readLine() ?: return emptyList()

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
    try {
        val questionList =
            reader
                .lineSequence()
                .filter { it.isNotBlank() }
                .map {
                    // Had to split data into two due to an error with there not being a 7th component for splitting data.
                    val data = it.split(',', ignoreCase = false, limit = 7)
                    val (question, option1, option2, option3) = data.chunked(4)[0]
                    val (option4, correctOption, keyStage, _) = data.chunked(4)[1]

                    // Ensures key stage is in the correct format as best as I could think of.
                    val formattedKeyStage =
                        when (keyStage.trim().uppercase()) {
                            "KS1", "Key Stage 1" -> "keystage1"
                            "KS2", "Key Stage 2" -> "keystage2"
                            "KS3", "Key Stage 3" -> "keystage3"
                            "KS4", "Key Stage 4" -> "keystage4"
                            "KS5", "Key Stage 5" -> "keystage5"
                            "Further Education" -> "Further Education"
                            "nonapplicable" -> "N/A"
                            else -> "nonapplicable"
                        }

                    // Creates ReadQuestionData object to make storing this information simpler.
                    CsvQuestionData(
                        question.trim(),
                        option1.trim(),
                        option2.trim(),
                        option3.trim(),
                        option4.trim(),
                        correctOption.trim(),
                        formattedKeyStage,
                    )
                }.toList()
                .distinct()
        return questionList
    } catch (e: Exception) {
        println("Exception while reading CSV")
        return null
    }
}
