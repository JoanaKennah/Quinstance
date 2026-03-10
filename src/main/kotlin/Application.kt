package com.example

import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import com.example.database.TestDatabase

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDatabase()
    configureTemplating()
    configureRouting()
}

// Used for testing :)

fun Application.testModule() {
    TestDatabase.create()
    TransactionManager.defaultDatabase = TestDatabase.db
    configureTemplating()
    configureRouting()
}
