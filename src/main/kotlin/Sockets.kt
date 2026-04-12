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
import com.example.retrieveQuestionAnswer
import io.ktor.server.websocket.WebSocketServerSession

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

data class GameSession(
    val gameId: Int,
    val questions: List<Question>,
    var currentQuestionIndex: Int = 0,
    var isAdvanced: Boolean = false,                //flag for advancing question
    val connections: MutableSet<WebSocketServerSession> = Collections.synchronizedSet(LinkedHashSet())
)

val activeSessions = Collections.synchronizedMap(mutableMapOf<Int, GameSession>())

fun Application.configureSockets() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    println("Server socket configured")

    //routing for sockets - added here to keep wesockets code in the same place
    routing {
    webSocket("/game/{gameId}") {
        val gameId = call.parameters["gameId"]?.toIntOrNull()              //read gameId from URL
        if (gameId == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid Game Id"))
            return@webSocket
        }

        //Fetch active session for game
        val session = activeSessions[gameId]
        if (session == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Game Not Found"))
            return@webSocket
        }

        //set connection per player
        session.connections.add(this)
        println("Player Connected To Game $gameId, ${session.connections.size} players connected")

        //monitor for incoming messages from player
        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    try {
                        val answerData = Json.decodeFromString<SentAnswerData>(frame.readText())
                        UserAnswers.addAnswer(answerData)
                        println("Answer recieved from ${answerData.username}: ${answerData.chosenAnswer}")
                    } catch (e: Exception) {
                        println("Could not deserialise answer: ${e.message}")
                    }
                }
            }
        } finally {
            //remove connection when player disconnects
            session.connections.remove(this)
            println("Player Disconnected From Game $gameId, ${session.connections.size} players remaining")
        }

    }
}
}



suspend fun broadcastQuestion(session: GameSession) {
    val question = session.questions[session.currentQuestionIndex]
    val answers = retrieveQuestionAnswer(question) ?: return 

    //builds list of answers & shuffles
    val answerList = listOf(answers.correct_answer, answers.option2, answers.option3, answers.option4).filterNotNull().shuffled()


    
    val questionData = QuestionData(
        questionNumber = session.currentQuestionIndex + 1,
        questionText = question.question,
        answerOne = answerList[0],
        answerTwo = answerList[1],
        answerThree = answerList.getOrNull(2),
        answerFour = answerList.getOrNull(3)
    )
    //sends to each player connected concurrrently
    session.connections.forEach { connection -> connection.sendSerialized(questionData) }

}

