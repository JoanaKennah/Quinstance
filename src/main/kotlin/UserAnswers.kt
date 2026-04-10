package com.example

object UserAnswers {
    private val userAnswers = mutableListOf<SentAnswerData>()

    fun allUserAnswers(): List<SentAnswerData> = userAnswers

    fun addAnswer(data: SentAnswerData) {
        if (!userAnswers.contains(data)) userAnswers.add(data)
        else throw IllegalArgumentException("Cannot add duplicate user answers")
    }

    fun clearAnswers() = userAnswers.clear()
}