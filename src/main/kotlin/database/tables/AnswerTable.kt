package com.example.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object AnswerTable : IntIdTable() {
    val question = reference("question", QuestionTable, ReferenceOption.CASCADE)
    val correct_answer = varchar("correct_answer", 50)
    val option2 = varchar("option2", 50)
    val option3 = varchar("option3", 50).nullable()
    val option4 = varchar("option4", 50).nullable()
}