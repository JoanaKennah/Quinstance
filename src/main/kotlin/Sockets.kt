package com.example

import com.example.UserAnswers
import com.example.database.objects.Question
import com.example.retrieveQuestionAnswer
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*
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
    val chosenAnswer: String,
    val timeRemaining: Int,
)

data class GameSession(
    val gameId: Int,
    val hostUsername: String,
    val questions: List<Question>,
    var currentQuestionIndex: Int = 0,
    var isAdvanced: Boolean = false,
    var isStarted: Boolean = false,
    var isFinished: Boolean = false,
    val timeLimit: Int = 30,
    val maxPlayers: Int = 40,
    val miniGamesEnabled: Boolean = false,
    val activeMinigames: List<String> = listOf("maths", "potions", "rooms", "words"),
    val theme: String = "default",
    var lastCorrectPlayers: List<String> = emptyList(),
    var lastIncorrectPlayers: List<String> = emptyList(),
    var minigameJustPlayed: Boolean = false,
    var mathsGameState: MathsGameState? = null,
    var potionsGameState: PotionsGameState? = null,
    var roomsGameState: RoomsGameState? = null,
    var wordsGameState: WordsGameState? = null,
    var ghostPlayers: MutableList<String> = mutableListOf(),
    var revivedPlayers: MutableList<String> = mutableListOf(),
    var autoGhostedPlayer: String? = null,
    var kickedUsernames: MutableList<String> = mutableListOf(),
    var barredUsernames: MutableList<String> = mutableListOf(),
    var answeredPlayers: MutableSet<String> = mutableSetOf(),
    val connections: MutableSet<WebSocketServerSession> = Collections.synchronizedSet(LinkedHashSet()),
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

    routing {
        webSocket("/game/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull()

            println("WebSocket connection attempt for gameId: $gameId")
            println("Active sessions: ${activeSessions.keys}")

            if (gameId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid Game Id"))
                return@webSocket
            }

            val session = activeSessions[gameId]
            if (session == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Game Not Found"))
                return@webSocket
            }

            session.connections.add(this)
            println("Player Connected To Game $gameId, ${session.connections.size} players connected")

            // immediately send current question or redirect if already answered
            // prevents sync issue where client connects after broadcastQuestion fires
            if (session.isStarted) {
                val username = call.sessions.get<UserSession>()?.username
                if (username != null && username in session.answeredPlayers) {
                    // player already answered this question - send to waiting screen
                    send(Frame.Text("{\"redirect\":\"/selected-answer/$gameId?answer=\"}"))
                } else {
                    val question = session.questions[session.currentQuestionIndex]
                    val answers = retrieveQuestionAnswer(question)
                    if (answers != null) {
                        val answerList =
                            listOf(answers.correct_answer, answers.option2, answers.option3, answers.option4)
                                .filterNotNull()
                                .shuffled()
                        val questionData =
                            QuestionData(
                                questionNumber = session.currentQuestionIndex + 1,
                                questionText = question.question,
                                answerOne = answerList[0],
                                answerTwo = answerList[1],
                                answerThree = answerList.getOrNull(2),
                                answerFour = answerList.getOrNull(3),
                            )
                        sendSerialized(questionData)
                    }
                }
            }

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val rawText = frame.readText()
                        println("Raw frame recieved: $rawText")
                        try {
                            val answerData = Json.decodeFromString<SentAnswerData>(frame.readText())
                            // only process if player hasn't already answered current question
                            if (answerData.username !in session.answeredPlayers) {
                                session.answeredPlayers.add(answerData.username)
                                UserAnswers.addAnswer(answerData)
                                println("Answer recieved from ${answerData.username}: ${answerData.chosenAnswer}")
                            } else {
                                println("Duplicate answer ignored from ${answerData.username}")
                            }
                        } catch (e: Exception) {
                            println("Could not deserialise answer: ${e.message}")
                        }
                    }
                }
            } finally {
                session.connections.remove(this)
                println("Player Disconnected From Game $gameId, ${session.connections.size} players remaining")
            }
        }
    }
}

suspend fun broadcastQuestion(session: GameSession) {
    val question = session.questions[session.currentQuestionIndex]
    val answers = retrieveQuestionAnswer(question) ?: return

    val answerList =
        listOf(answers.correct_answer, answers.option2, answers.option3, answers.option4)
            .filterNotNull()
            .shuffled()

    val questionData =
        QuestionData(
            questionNumber = session.currentQuestionIndex + 1,
            questionText = question.question,
            answerOne = answerList[0],
            answerTwo = answerList[1],
            answerThree = answerList.getOrNull(2),
            answerFour = answerList.getOrNull(3),
        )
    session.connections.forEach { connection -> connection.sendSerialized(questionData) }
}
