package com.example.database.objects

import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import com.example.database.tables.GameTable

class Game(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Game>(GameTable)

    var quiz by Quiz referencedOn GameTable.quiz
    var timeStamp by GameTable.timeStamp

    override fun toString() = id.toString()
}