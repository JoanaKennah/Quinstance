package com.example.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object AnswerTable : IntIdTable() {
    val questionID = reference("questionID", QuestionTable.id, ReferenceOption.CASCADE)
    val correct_answer = varchar("correct_answer", 50)
    val option2 = varchar("option2", 50)
    val option3 = varchar("option3", 50)
    val option4 = varchar("option4", 50)
}