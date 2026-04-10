package com.example

import com.example.database.objects.Question
import com.example.database.objects.Answer
import com.example.UserAnswers
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.internal.decodeByReader
import java.util.*
import kotlin.collections.ArrayList
import kotlin.time.Duration.Companion.seconds

@Serializable
data class QuestionData(
    val questionNumber: Int,
    val questionText: String,
    val answerOne: String,
    val answerTwo: String,
    val answerThree: String?,
    val answerFour: String?,
)

@Serializable
data class SentAnswerData(
    val username: String,
    val chosenAnswer: String, // actual text as the questions are jumbled up
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
}
fun Application.sendQuestionToUsers(questionNumber: Int, question: Question, questionAnswers: Answer){
    routing {
        webSocket("/gameServer") {
            println("game server web socket exists :3")
            val answers = listOf(questionAnswers.correct_answer, questionAnswers.option2, questionAnswers.option3, questionAnswers.option4)
            val randomisedAnswers = answers.filterNotNull().shuffled()
            val builtQuestion = QuestionData(questionNumber, question.question, randomisedAnswers[0], randomisedAnswers[1], randomisedAnswers[2], randomisedAnswers[3])
            sendSerialized(builtQuestion)

            // when data is sent back
            val answerData= receiveDeserialized<SentAnswerData>()
            UserAnswers.addAnswer(answerData)
            println("Answer $answerData")
            println(UserAnswers.allUserAnswers())
        }
    }
}