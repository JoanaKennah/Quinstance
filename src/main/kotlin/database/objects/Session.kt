package com.example.database.objects

import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import com.example.database.tables.QuizTable
import com.example.database.tables.SessionTable

class Session(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Session>(SessionTable)

    var quizID by Quiz referencedOn QuizTable.id

    override fun toString() = id.toString()
}