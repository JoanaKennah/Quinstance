package com.example.database.tables

import com.example.database.tables.QuizTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object QuinstanceTable : IntIdTable() {
    val quiz = reference("quiz", QuizTable, ReferenceOption.CASCADE)
    val question = reference("question", QuestionTable, ReferenceOption.CASCADE)
}
