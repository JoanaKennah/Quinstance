package com.example

import com.example.database.objects.Question
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
data class SentQuestionData(
    val questionNumber: Int, // could remove?
    val questionText: String,
    val answerOne: String,
    val answerTwo: String,
    val answerThree: String?,
    val answerFour: String?,
)

@Serializable
data class SentAnswerData(
    val userID: Int,
    val chosenAnswer: Int, // answer index (min: 0, max: 3)
    val answerSpeed: Float // could be a time object, unsure yet
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
            sendSerialized(SentQuestionData(1, "what is my name?", "Ethan", "Emily", "Minghao", "Joana"))
            delay(1000)

            val userAnswer = receiveDeserialized<SentAnswerData>()
            println("Received userAnswer: $userAnswer")
            close(CloseReason(CloseReason.Codes.NORMAL, "All ok!"))
        }
    }
}