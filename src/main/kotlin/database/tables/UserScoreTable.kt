package com.example.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object UserScoreTable : IntIdTable() {
    val game = reference("game", GameTable, ReferenceOption.CASCADE)
    val user = reference("user", UserTable, ReferenceOption.CASCADE)
    val score = integer("score")
    val isGhost = bool("isGhost").default(false)
}
