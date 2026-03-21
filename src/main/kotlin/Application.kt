package com.example

import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import com.example.database.TestDatabase
import io.ktor.server.websocket.*
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDatabase()
    configureTemplating()
    configureRouting()
    configureSockets()
}

// Used for testing :)

fun Application.testModule() {
    TestDatabase.create()
    TransactionManager.defaultDatabase = TestDatabase.db
    configureTemplating()
    configureRouting()
}
