package com.example.database.objects

import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import com.example.database.tables.AnswerTable
import com.example.database.tables.QuestionTable

class Answer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Answer>(AnswerTable)

    var questionID by Question referencedOn QuestionTable.id
    var correct_answer by AnswerTable.correct_answer
    var option2 by AnswerTable.option2
    var option3 by AnswerTable.option3
    var option4 by AnswerTable.option4
}