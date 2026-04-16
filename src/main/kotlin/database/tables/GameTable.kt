package com.example.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object GameTable : IntIdTable() {
    val quiz = reference("quiz", QuizTable, ReferenceOption.CASCADE)
    val timeStamp = varchar("created_at", 50).default(java.time.LocalDateTime.now().toString())
}