package com.example.database.objects

import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import com.example.database.tables.SessionTable
import com.example.database.tables.UserTable
import com.example.database.tables.UserScoreTable

class UserScore(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserScore>(UserScoreTable)

    var sessionID by Session referencedOn SessionTable.id
    var userID by User referencedOn UserTable.id
    var score by UserScoreTable.score

    override fun toString() = score.toString()
}