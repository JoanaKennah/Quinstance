package com.example

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import com.example.database.ProjectDatabase
import com.example.database.TestDatabase
import io.ktor.server.websocket.*
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    TransactionManager.defaultDatabase = ProjectDatabase.database
    configureSessions()
    configureDatabase()
    configureTemplating()
    configureRouting()
    configureSockets()
}

fun Application.configureSessions() {
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.httpOnly = true
        }
    }
}

// Used for testing :)

fun Application.testModule() {
    TestDatabase.create()
    TransactionManager.defaultDatabase = TestDatabase.db
    configureSessions()
    configureTemplating()
    configureRouting()
    configureSockets()
}
