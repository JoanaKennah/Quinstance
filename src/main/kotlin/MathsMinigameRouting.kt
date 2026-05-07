package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.pebble.PebbleContent
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.websocket.Frame
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureMathsMinigameRouting() {
    routing {
        // Serves start screens to clients and host when host presses start
        post("/maths-begin/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            gameSession.connections.forEach { connection ->
                connection.send(Frame.Text("{\"redirect\":\"/minigame-question/$gameId\"}"))
            }

            gameSession.mathsGameState?.isStarted = true

            call.respond(
                PebbleContent(
                    "mini_game_maths_server.peb",
                    mapOf(
                        "quizName" to (retrieveQuizFromGameID(gameId)?.quizName ?: ""),
                        "gameId" to gameId,
                        "theme" to session.theme,
                        "left_users" to gameSession.lastIncorrectPlayers.take(3),
                        "right_users" to gameSession.lastIncorrectPlayers.drop(3),
                    ),
                ),
            )
        }

        // Sends clients to uh-oh or safe screen, split by dangerPlayers
        get("/minigame-start/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val game =
                retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            val userScore = retrieveUserScore(game, user)?.score ?: 0

            // splits players
            if (session.username in gameSession.lastIncorrectPlayers) {
                call.respond(
                    PebbleContent(
                        "mini_game_uhoh.peb",
                        mapOf(
                            "gameId" to gameId,
                            "score" to userScore,
                            "theme" to session.theme,
                        ),
                    ),
                )
            } else {
                call.respond(
                    PebbleContent(
                        "minigame_client_safe_wait.peb",
                        mapOf(
                            "gameId" to gameId,
                            "score" to userScore,
                            "theme" to session.theme,
                        ),
                    ),
                )
            }
        }

        // Serves client with a given maths question and four options, one correct answer
        get("/minigame-question/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val mathsState =
                gameSession.mathsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val questionIndex = mathsState.questionIndex.getOrDefault(session.username, 0)
            val mathsQuestion =
                mathsState.questions.getOrNull(questionIndex)
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No question available")))

            // get answer options and shuffle
            val options: List<String> =
                listOf(
                    mathsQuestion.correctAnswer,
                    mathsQuestion.option2,
                    mathsQuestion.option3,
                    mathsQuestion.option4,
                ).shuffled()

            call.respond(
                PebbleContent(
                    "mini_game_maths_question.peb",
                    mapOf(
                        "gameId" to gameId,
                        "question" to mathsQuestion.question,
                        "correct_answer" to mathsQuestion.correctAnswer,
                        "option1" to options[0],
                        "option2" to options[1],
                        "option3" to options[2],
                        "option4" to options[3],
                        "theme" to session.theme,
                    ),
                ),
            )
        }

        // If client gets maths question right, update maths state and send client a new maths question
        get("/minigame-correct/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val mathsState =
                gameSession.mathsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            // Increments client score by one.
            mathsState.mathsScores[session.username] =
                (mathsState.mathsScores.getOrDefault(session.username, 0)) + 1

            // Increments which question the user is on by 1 and redirects them to a new question. This repeats until the host's timer ends.
            val currentIndex = mathsState.questionIndex.getOrDefault(session.username, 0)
            mathsState.questionIndex[session.username] = currentIndex + 1

            call.respondRedirect("/minigame-question/$gameId")
        }

        // If the client gets the maths question wrong, send them to the incorrect/stunned page for 3 seconds
        get("/minigame-incorrect/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            activeSessions[gameId]
                ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            call.respond(
                PebbleContent(
                    "mini_game_maths_incorrect_ans.peb",
                    mapOf(
                        "gameId" to gameId,
                        "theme" to session.theme,
                    ),
                ),
            )
        }

        // Calculate which clients got the lowest score (can be a draw) and add them to Maths state ghost player list.
        post("/minigame-end/{gameId}") {
            call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val mathsState =
                gameSession.mathsGameState
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val dangerScores =
                gameSession.lastIncorrectPlayers.associateWith { username ->
                    mathsState.mathsScores.getOrDefault(username, 0)
                }

            val minScore = dangerScores.values.minOrNull() ?: 0

            mathsState.ghostPlayers =
                dangerScores
                    .filter { it.value == minScore }
                    .map { it.key }

            // Incentive points for winning minigame, if players > 3.
            if (gameSession.lastIncorrectPlayers.size >= 3) {
                // Sort scores descending, exclude 0s for top scorers check
                val topScorers =
                    mathsState.mathsScores
                        .filter { it.value > 0 }
                        .entries
                        .sortedByDescending { it.value }

                transaction(TransactionManager.defaultDatabase) {
                    // Winner - 200 points
                    topScorers.getOrNull(0)?.let { (username, _) ->
                        val bonusUser = retrieveUser(username)
                        val bonusGame = retrieveGame(gameId)
                        if (bonusUser != null && bonusGame != null) {
                            val userScore = retrieveUserScore(bonusGame, bonusUser)
                            if (userScore != null) {
                                userScore.score += 200
                            }
                        }
                    }

                    // Second - 100 points
                    topScorers.getOrNull(1)?.let { (username, _) ->
                        val bonusUser = retrieveUser(username)
                        val bonusGame = retrieveGame(gameId)
                        if (bonusUser != null && bonusGame != null) {
                            val userScore = retrieveUserScore(bonusGame, bonusUser)
                            if (userScore != null) {
                                userScore.score += 100
                            }
                        }
                    }
                }
            }

            // Add new ghosts to master list of ghosts in GameSession
            gameSession.ghostPlayers.addAll(mathsState.ghostPlayers)

            // Mark minigame finished
            mathsState.isFinished = true

            gameSession.connections.forEach { connection -> connection.send(Frame.Text("{\"redirect\":\"/minigame-result/$gameId\"}")) }

            call.respond(HttpStatusCode.OK)
        }

        // Serves end screen to host showing which client/s became ghosts.
        get("/minigame-end-host/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val mathsState =
                gameSession.mathsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            call.respond(
                PebbleContent(
                    "mini_game_maths_server_ghost.peb",
                    mapOf(
                        "gameId" to gameId,
                        "theme" to session.theme,
                        "ghostPlayers" to mathsState.ghostPlayers,
                    ),
                ),
            )
        }

        // Serves client screen with their result, showing if they are safe or became a ghost.
        get("/minigame-result/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val game =
                retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            val mathsState =
                gameSession.mathsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val userScore = retrieveUserScore(game, user)?.score ?: 0

            val isExistingGhost =
                session.username in gameSession.ghostPlayers &&
                    session.username !in mathsState.ghostPlayers

            if (session.username in mathsState.ghostPlayers &&
                session.username in gameSession.lastIncorrectPlayers
            ) {
                // Newly ghosted this minigame
                call.respond(
                    PebbleContent(
                        "mini_game_maths_ur_ghost.peb",
                        mapOf(
                            "gameId" to gameId,
                            "score" to userScore,
                            "theme" to session.theme,
                        ),
                    ),
                )
            } else {
                // Safe or existing ghost
                call.respond(
                    PebbleContent(
                        "mini_games_maths_ur_safe.peb",
                        mapOf(
                            "gameId" to gameId,
                            "score" to userScore,
                            "theme" to session.theme,
                            "isExistingGhost" to isExistingGhost,
                        ),
                    ),
                )
            }
        }
    }
}
