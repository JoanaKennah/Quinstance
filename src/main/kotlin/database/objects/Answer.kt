package com.example.database.objects

import com.example.database.tables.AnswerTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class Answer(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<Answer>(AnswerTable)

    var question by Question referencedOn AnswerTable.question
    var correctAnswer by AnswerTable.correct_answer
    var option2 by AnswerTable.option2
    var option3 by AnswerTable.option3
    var option4 by AnswerTable.option4
}
