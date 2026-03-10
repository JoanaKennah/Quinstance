package com.example.database.tables

import com.example.database.tables.QuizTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object QuinstanceTable : IntIdTable() {
    val quizID = reference("quizID", QuizTable.id, ReferenceOption.CASCADE)
    val questionID = reference("questionID", QuestionTable.id, ReferenceOption.CASCADE)
}