package com.example.database.tables

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object QuestionTable : IntIdTable() {
    val question = varchar("question", 256) // temp length
    val key_stage = varchar("key_stage", 50) // KS1..KS4, etc
}