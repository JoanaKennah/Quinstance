package com.example.database.objects

import com.example.database.tables.QuestionTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class Question(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<Question>(QuestionTable)

    var question by QuestionTable.question
    var keyStage by QuestionTable.key_stage

    override fun toString() = question
}
