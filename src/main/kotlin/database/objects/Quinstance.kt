package com.example.database.objects

import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import com.example.database.tables.QuestionTable
import com.example.database.tables.QuinstanceTable
import com.example.database.tables.QuizTable

class Quinstance(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Quinstance>(QuinstanceTable)

    var quizID by Quinstance referencedOn QuizTable.id
    var questionID by Question referencedOn QuestionTable.id

    override fun toString() = id.toString()
}