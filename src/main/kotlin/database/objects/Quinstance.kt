package com.example.database.objects

import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import com.example.database.tables.QuinstanceTable

class Quinstance(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Quinstance>(QuinstanceTable)

    var quiz by Quiz referencedOn QuinstanceTable.quiz
    var question by Question referencedOn QuinstanceTable.question

    override fun toString() = question.question
}