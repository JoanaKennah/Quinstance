package com.example.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object QuizTable : IntIdTable() {
    val userID = reference("userID", AnswerTable.id, ReferenceOption.CASCADE)
    val quiz_name = varchar("quiz_name", 50)
    val num_of_questions = integer("num_of_questions")
    val description = varchar("description", 50)
    val category = varchar("category", 50)
}