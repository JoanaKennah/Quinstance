package com.example

import com.example.database.objects.Question
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Collections
import kotlin.time.Duration.Companion.seconds

/**
 * Stores data about a Question sent across sockets.
 * @property questionNumber Non-negative question number
 * @property questionText Question text
 * @property answerOne First possible correct answer, cannot be empty
 * @property answerTwo Second possible correct answer, cannot be empty
 * @property answerThree Third possible correct answer, can be null
 * @property answerFour Fourth possible correct answer, can be null
 */
@Serializable
data class QuestionData(
    val questionNumber: Int,
    val questionText: String,
    val answerOne: String,
    val answerTwo: String,
    val answerThree: String?,
    val answerFour: String?,
)

/**
 * Stores data about a User's answer sent across sockets.
 * @property username Unique identifier for a User
 * @property chosenAnswer Text of the answer the User picked
 * @property timeRemaining Time left on the host timer
 */
@Serializable
data class SentAnswerData(
    val username: String,
    val chosenAnswer: String,
    val timeRemaining: Int,
)

/**
 * Stores data associated with a Game session. Emily's baby.
 * @property gameId Unique identifier on Database for the associated Game
 * @property hostUsername Username for the Game host
 * @property questions List of Questions from the Quiz associated with the current Game
 * @property currentQuestionIndex Index of the current Question the Game Session is on
 * @property isAdvanced State that is True if the Game is advancing to the next Question, False otherwise
 * @property isStarted State that is True if the Game has started (and timer is going down), False otherwise
 * @property isFinished State that is True if the Game has finished, False otherwise
 * @property timeLimit Time limit set for Users to answer a Question
 * @property maxPlayers Number of maximum players allowed in a Game
 * @property miniGamesEnabled State that is True if the host enabled minigames in Lobby, False otherwise
 * @property activeMinigames List of minigames that are active, meaning they could appear if User's get a question wrong
 * @property theme Theme for a Game set in the Lobby
 * @property lastCorrectPlayers List of players that recently got a Question correct
 * @property lastIncorrectPlayers List of players that recently got a Question wrong
 * @property minigameJustPlayed State that is True if the last Question had a minigame after, False otherwise
 * @property mathsGameState Stores a MathsGameState object if one is initialised, is null otherwise
 * @property potionsGameState Stores a PotionsGameState object if one is initialised, is null otherwise
 * @property roomsGameState Stores a RoomsGameState object if one is initialised, is null otherwise
 * @property wordsGameState Stores a WordsGameState object if one is initialised, is null otherwise
 * @property ghostPlayers List of players that are currently ghosts
 * @property revivedPlayers List of players that were revived after a minigame
 * @property autoGhostedPlayer Username of the player that became auto ghosted, null if no one became a ghost automatically
 * @property kickedUsernames List of usernames of Users who were previously kicked
 * @property barredUsernames List of usernames barred from a Game
 * @property answeredPlayers List of players who have answered the current Question
 * @property connections Set of WebSocketServerSession's
 */
data class GameSession(
    val gameId: Int,
    val hostUsername: String,
    val questions: List<Question>,
    var currentQuestionIndex: Int = 0,

    // flags to assist with quiz progression
    var isAdvanced: Boolean = false,
    var isStarted: Boolean = false,
    var isFinished: Boolean = false,

    // lobby defaults
    val timeLimit: Int = 30,
    val maxPlayers: Int = 40,

    // required for minigame triggers
    val miniGamesEnabled: Boolean = false,
    val activeMinigames: List<String> = listOf("maths", "potions", "rooms", "words"),

    val theme: String = "default",

    // lists to split players for minigames
    var lastCorrectPlayers: List<String> = emptyList(),
    var lastIncorrectPlayers: List<String> = emptyList(),

    // stops duplicate minigames triggering and 2 in a row
    var minigameJustPlayed: Boolean = false,

    // minigame states for progression
    var mathsGameState: MathsGameState? = null,
    var potionsGameState: PotionsGameState? = null,
    var roomsGameState: RoomsGameState? = null,
    var wordsGameState: WordsGameState? = null,

    // sort players by status for redirects
    var ghostPlayers: MutableList<String> = mutableListOf(),
    var revivedPlayers: MutableList<String> = mutableListOf(),
    var autoGhostedPlayer: String? = null,

    // used in managing participants
    var kickedUsernames: MutableList<String> = mutableListOf(),
    var barredUsernames: MutableList<String> = mutableListOf(),

    // assists with websocket loop issues and timing bugs
    var answeredPlayers: MutableSet<String> = mutableSetOf(),
    var playerAnswers: MutableMap<String, String> = mutableMapOf(),
    var isShowingCorrectAnswer: Boolean = false,

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

    // additional routing where game flow requiring large amounts of websocket integration
    routing {
        webSocket("/game/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull()

            if (gameId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid Game Id"))
                return@webSocket
            }

            val session = activeSessions[gameId]
            if (session == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Game Not Found"))
                return@webSocket
            }

            // register client connection for host to broadcast to
            session.connections.add(this)

            // Immediately send current question or redirect if already answered
            // Prevents sync issue where client connects after broadcastQuestion fires
            if (session.isStarted) {
                val username = call.sessions.get<UserSession>()?.username
                if (username != null && username in session.answeredPlayers && !session.isShowingCorrectAnswer) {
                    // Player already answered this question - send to waiting screen
                    val previousAnswer = session.playerAnswers[username] ?: ""
                    send(Frame.Text("{\"redirect\":\"/selected-answer/$gameId?answer=$previousAnswer\"}"))
                } else {
                    // player yet to answer so send current question
                    val question = session.questions[session.currentQuestionIndex]
                    val answers = retrieveQuestionAnswer(question)
                    if (answers != null) {
                        val answerList =
                            listOfNotNull(answers.correctAnswer, answers.option2, answers.option3, answers.option4)
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
                // listen for incoming answers from client
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val rawText = frame.readText()
                        try {
                            val answerData = Json.decodeFromString<SentAnswerData>(frame.readText())
                            // Only process if player hasn't already answered current question
                            if (answerData.username !in session.answeredPlayers) {
                                session.answeredPlayers.add(answerData.username)
                                session.playerAnswers[answerData.username] = answerData.chosenAnswer
                                UserAnswers.addAnswer(answerData)
                            } else {
                                // dupe answer recieved so ignore - fixes a double scoring issue
                            }
                        } catch (e: Exception) {
                            // ignore a frame with invalid answer data and print error
                            println("Could not deserialise answer: ${e.message}")
                        }
                    }
                }
            } finally {
                // deregister connection when client disconnects
                session.connections.remove(this)
            }
        }
    }
}

/**
 * Broadcast the current Question to every client associated with the given session.
 * @param session Current GameSession in use
 */
suspend fun broadcastQuestion(session: GameSession) {
    val question = session.questions[session.currentQuestionIndex]
    val answers = retrieveQuestionAnswer(question) ?: return

    val answerList =
        listOfNotNull(answers.correctAnswer, answers.option2, answers.option3, answers.option4)
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
