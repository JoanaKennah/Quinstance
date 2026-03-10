package com.example.database.tables

import com.example.database.tables.SessionTable
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.ReferenceOption

object UserScoreTable : IntIdTable() {
    val sessionID = reference("sessionID", SessionTable.id, ReferenceOption.CASCADE)
    val userID = reference("userID", UserTable.id, ReferenceOption.CASCADE)
    val score = integer("score")
}