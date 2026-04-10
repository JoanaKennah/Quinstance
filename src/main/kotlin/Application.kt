package com.example

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import com.example.database.ProjectDatabase
import com.example.database.TestDatabase

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    TransactionManager.defaultDatabase = ProjectDatabase.database
    configureSessions()
    configureDatabase()
    configureTemplating()
    configureRouting()
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
    configureTemplating()
    configureRouting()
    configureSessions()
}
