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

import com.password4j.Password
import com.example.retrieveUser

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(PebbleContent("homepage.peb", mapOf<String, Any>()))
        }

        get("/login") {
            call.respond(PebbleContent("login.peb", mapOf<String, Any>()))
        }

        post("/login") { // I think this will only work for the real database, so we can't test it just yet
            val parameters = call.receiveParameters()
            val username = parameters["username"]
            val password = parameters["password"]

            if (username == null || password == null) {
                call.respondText("Missing input(s)")
                return@post
            }

            val user = retrieveUser(username)

            if (user == null) {
                call.respondText("Unexisting user")
                return@post
            }

            val checkPassword = Password.check(password, user.password).withScrypt()

            if (checkPassword) {
                when (user.role.lowercase()) {
                    "student" -> {
                        call.respondRedirect("/student_dashboard")
                    }
                    "teacher", "admin" -> {
                        call.respondRedirect("/teacher_dashboard")
                    }
                }
            } else {
                call.respondText("Incorrect password")
            }
        }

        get("/signup") {
            call.respond(PebbleContent("signup.peb", mapOf<String, Any>()))
        }

        get("/student_dashboard") {
            call.respond(PebbleContent("student_dashboard.peb", mapOf()))
        }

        get("/teacher_dashboard") {
            call.respond(PebbleContent("teacher_dashboard.peb", mapOf()))
        }
    }
}