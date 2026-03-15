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
            call.respond(PebbleContent("homepage.peb", mapOf<String, Any>()))
        }

        get("/home") {
            call.respond(PebbleContent("homepage.peb", mapOf<String, Any>()))
        }

        get("/login") {
            call.respond(PebbleContent("login.peb", mapOf<String, Any>()))
        }

        post("/login") {
            val params = call.receiveParameters()
            val username = params["username"] ?: ""
            //add - look up in db and check role
            val role = "student" //placeholder

            if (role == "teacher") {
                call.respondRedirect("/teacher-dashboard")
            } else {
                call.respondRedirect("/student-dashboard")
            }
        }

        get("/signup") {
            call.respond(PebbleContent("signup.peb", mapOf<String, Any>()))
        }

        post("/signup") {
            val params = call.receiveParameters()
            val username = params["username"] ?: ""
            val role = params["role"] ?: "student"
            //add - insert user in db

            if (role == "teacher") {
                call.respondRedirect("/teacher-dashboard")
            } else {
                call.respondRedirect("/student-dashboard")
            }
        }

        get("/teacher-dashboard") {
            val username = "Teacher" // TODO: get from session
            call.respond(PebbleContent("teacher_dashboard.peb", mapOf("username" to username)))
        }

        get("/student-dashboard") {
            val username = "Student" // TODO: get from session
            call.respond(PebbleContent("student_dashboard.peb", mapOf("username" to username)))
        }
    }
}