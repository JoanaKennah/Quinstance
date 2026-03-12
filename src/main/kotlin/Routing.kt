package com.example

import com.example.database.ProjectDatabase
import com.example.database.tables.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import io.pebbletemplates.pebble.loader.ClasspathLoader
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
            transaction(ProjectDatabase.database) {
                val user = addUser("ethxn87",
                    "test123",
                    "Ethan Harris",
                    "ethan@gmail.com",
                    "N/A",
                    "Admin")
                if (user != null) {
                    println("user made yay")
                    val quiz = addQuiz(user, "My Quiz", "yay", "Cool", 1)
                    val question1 = addQuestion(quiz, "What is my name?", "KS1")
                    println("Question 1 ID: ${question1.id}")

                    // test 2
                    val allQuiz = retrieveUserQuizzes(user.username)
                    if (allQuiz != null) {
                        for (quiz in allQuiz) {
                            println(quiz.quiz_name)
                        }
                    }

                    // test3
                    val question2 = addQuestion(quiz, "How old am I?", "KS1")
                    val question3 = addQuestion(quiz, "What university do I go to?", "KS1")
                    val questions = retrieveQuizQuestions(quiz)
                    for (i in questions) {
                        println("Question ID: ${i.id}: Question: ${i.question}")
                    }

                    // test 4
                    val answer1 = addAnswer(question1, "Ethan", "Emily", "Joana", "Minghao")
                    val answer2 = addAnswer(question2, "20", "24", "21")
                    val answer3 = addAnswer(question3, "Uni of Leeds", "Uni of Sheffield")

                    for (question in questions) {
                        println("Question: ${question.id}")
                        val ans = retrieveQuestionAnswer(question)
                        if (ans != null) {
                            println("Correct answer: ${ans.correct_answer}")
                        }
                    }
                }
            }
        }
    }
}
