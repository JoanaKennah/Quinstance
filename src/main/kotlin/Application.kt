package com.example

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.util.hex
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import com.example.database.ProjectDatabase
import com.example.database.TestDatabase
import io.ktor.server.websocket.*

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
            cookie.path = "/"
            cookie.httpOnly = true

            transform(
                SessionTransportTransformerEncrypt(
                    hex("00112233445566778899aabbccddeeff"),
                    hex("8899aabbccddeeff0011223344556677")
                )
            )
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
}
