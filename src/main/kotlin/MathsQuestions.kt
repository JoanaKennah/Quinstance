package com.example 

data class MathsGameState(
    val questions: List<MathsQuestion> = MathsQuestions.shuffleAnswers(),
    val questionIndex: MutableMap<String, Int> = mutableMapOf(),
    val mathsScores: MutableMap<String, Int> = mutableMapOf(),
    var ghostPlayers: List<String> = emptyList(),
    var isStarted: Boolean = false,
    var isFinished: Boolean = false
)

data class MathsQuestion(
    val question: String,
    val correctAnswer: String,
    val option2: String,
    val option3: String,
    val option4: String
)

object MathsQuestions {
    val questions: List<MathsQuestion> by lazy {
        val stream = object {}.javaClass.getResourceAsStream("/maths_questions.csv") ?: error("Unable to find maths questions file")
        stream.bufferedReader().useLines { lines -> 
            lines.drop(1).filter { it.isNotBlank() }.map { line -> 
                val options = line.split(",")
                MathsQuestion(
                    question = options[0].trim(),
                    correctAnswer = options[1].trim(),
                    option2 = options[2].trim(),
                    option3 = options[3].trim(),
                    option4 = options[4].trim()
                )
            }.toList()
        }
    }

    fun shuffleAnswers(): List<MathsQuestion> = questions.shuffled()
}