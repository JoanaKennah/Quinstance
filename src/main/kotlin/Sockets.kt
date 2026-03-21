package com.example

import io.ktor.http.*
import io.ktor.server.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

@Serializable
data class Message(
    val message: String,
)

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/") {
            sendSerialized(Message("hello world"))
            delay(1000)
        }
    }
}