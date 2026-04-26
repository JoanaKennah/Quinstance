package com.example.database.objects

import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import com.example.database.tables.UserScoreTable

class UserScore(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserScore>(UserScoreTable)

    var game by Game referencedOn UserScoreTable.game
    var user by User referencedOn UserScoreTable.user
    var score by UserScoreTable.score
    //for end leaderboard purposes
    var ghost by UserScoreTable.ghost

    override fun toString() = score.toString()
}