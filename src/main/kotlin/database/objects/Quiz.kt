package com.example.database.objects

import com.example.database.tables.QuizTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class Quiz(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<Quiz>(QuizTable)

    var user by User referencedOn QuizTable.user // NEW FIX
    var quiz_name by QuizTable.quiz_name
    var num_of_questions by QuizTable.num_of_questions
    var description by QuizTable.description
    var category by QuizTable.category
    var key_stage by QuizTable.key_stage

    override fun toString() = quiz_name
}
