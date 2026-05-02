package com.example.database.objects

import com.example.database.tables.QuinstanceTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class Quinstance(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<Quinstance>(QuinstanceTable)

    var quiz by Quiz referencedOn QuinstanceTable.quiz
    var question by Question referencedOn QuinstanceTable.question

    override fun toString() = question.question
}
