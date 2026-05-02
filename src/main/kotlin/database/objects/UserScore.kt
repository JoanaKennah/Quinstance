package com.example.database.objects

import com.example.database.tables.UserScoreTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class UserScore(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<UserScore>(UserScoreTable)

    var game by Game referencedOn UserScoreTable.game
    var user by User referencedOn UserScoreTable.user
    var score by UserScoreTable.score
    var isGhost by UserScoreTable.isGhost

    override fun toString() = score.toString()
}
