package com.example

import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList
import kotlin.time.Duration.Companion.seconds

@Serializable
data class Message(
    val text: String
)

fun Application.configureSockets() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    println("Server socket configured")

    routing {
        webSocket("/") {
            sendSerialized(Message("Hello World!"))
            delay(1000)

            close(CloseReason(CloseReason.Codes.NORMAL, "All ok!"))
        }
    }
}