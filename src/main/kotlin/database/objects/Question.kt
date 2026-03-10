package com.example.database.objects

import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import com.example.database.tables.QuestionTable

class Question(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Question>(QuestionTable)

    var question by QuestionTable.question
    var key_stage by QuestionTable.key_stage

    override fun toString() = question
}