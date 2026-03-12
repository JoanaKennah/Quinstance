package com.example

import com.example.database.ProjectDatabase
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


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(PebbleContent("homepage.peb", mapOf<String, Any>()))
        }

        get("/login") {
            call.respond(PebbleContent("login.peb", mapOf<String, Any>()))
        }

        get("/signup") {
            call.respond(PebbleContent("signup.peb", mapOf<String, Any>()))
        }
    }
}