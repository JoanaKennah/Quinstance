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
                }
            }


        }
    }
}
