package com.example

import com.example.database.ProjectDatabase
import com.example.database.TestDatabase
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionTransportTransformerEncrypt
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.util.hex
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

/**
 * Configures the project Database (if it doesn't already exist), sets the default database to this for Database.kt and
 * configures all routing files and session file.
 */
fun Application.module() {
    TransactionManager.defaultDatabase = ProjectDatabase.database
    configureSessions()
    configureDatabase()
    configureTemplating()
    configureSockets()
    configureAuthRouting()
    configureDashboardRouting()
    configureQuizRouting()
    configureLobbyRouting()
    configureGameRouting()
    configureMathsMinigameRouting()
    configurePotionsMinigameRouting()
    configureRoomsMinigameRouting()
    configureWordsMinigameRouting()
}

/**
 * Configures sessions used throughout routing to store information about the given client and configures cookies for
 * this client.
 */
fun Application.configureSessions() {
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.path = "/"
            cookie.httpOnly = true

            transform(
                SessionTransportTransformerEncrypt(
                    hex("00112233445566778899aabbccddeeff"),
                    hex("8899aabbccddeeff0011223344556677"),
                ),
            )
        }
    }
}

/**
 * Creates Test Database, populates it with test data, sets the default database to this for Database.kt and
 * configures all routing files and session file.
 */
fun Application.testModule() {
    TestDatabase.create()
    TransactionManager.defaultDatabase = TestDatabase.db
    configureSessions()
    configureTemplating()
    configureAuthRouting()
    configureDashboardRouting()
    configureQuizRouting()
    configureLobbyRouting()
    configureGameRouting()
    configureMathsMinigameRouting()
    configurePotionsMinigameRouting()
    configureRoomsMinigameRouting()
    configureWordsMinigameRouting()
}
