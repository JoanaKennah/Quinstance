package com.example.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object SessionTable : IntIdTable() {
    val quizID = reference("quiz_id", QuizTable.id, ReferenceOption.CASCADE)
}