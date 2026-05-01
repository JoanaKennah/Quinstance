package com.example

import com.example.database.objects.UserScore
import com.example.database.tables.UserScoreTable
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.pebble.PebbleContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureLobbyRouting() {
    routing {
        // Sserves create lobby page, same formatting for category & Kstage
        get("/create-lobby/{quizId}") {
            call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val quizId =
                call.parameters["quizId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))
            val quiz =
                retrieveQuiz(quizId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val categoryDisplay =
                when (quiz.category) {
                    "art" -> "Art and Design"
                    "biology" -> "Biology"
                    "business" -> "Business Studies"
                    "chemistry" -> "Chemistry"
                    "cs" -> "Computer Science"
                    "dt" -> "Design and Technology"
                    "drama" -> "Drama"
                    "eng_lang" -> "English Language"
                    "eng_lit" -> "English Literature"
                    "french" -> "French"
                    "geography" -> "Geography"
                    "german" -> "German"
                    "history" -> "History"
                    "latin" -> "Latin"
                    "maths" -> "Mathematics"
                    "media" -> "Media Studies"
                    "music" -> "Music"
                    "pe" -> "Physical Education"
                    "physics" -> "Physics"
                    "rs" -> "Religious Studies"
                    "spanish" -> "Spanish"
                    "other" -> "Other"
                    else -> quiz.category
                }

            val keyStageDisplay =
                when (quiz.key_stage) {
                    "KS1" -> "Key Stage 1"
                    "KS2" -> "Key Stage 2"
                    "KS3" -> "Key Stage 3"
                    "KS4" -> "Key Stage 4"
                    "KS5" -> "Key Stage 5"
                    "furthereducation" -> "Further Education"
                    "nonapplicable" -> "N/A"
                    else -> quiz.key_stage
                }

            call.respond(
                PebbleContent(
                    "create_lobby.peb",
                    mapOf(
                        "quizId" to quizId,
                        "quizName" to quiz.quiz_name,
                        "numQuestions" to quiz.num_of_questions,
                        "category" to categoryDisplay,
                        "keyStage" to keyStageDisplay,
                    ),
                ),
            )
        }

        // Create game in DB, gen room code, create GameSession and redirect to host lobby view
        post("/create-lobby/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val quizId =
                call.parameters["quizId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))
            val quiz =
                retrieveQuiz(quizId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val parameters = call.receiveParameters()
            val timeLimit = parameters["timeLimit"]?.toIntOrNull() ?: 30

            // Read and update theme from checkbox
            val theme = if (parameters["theme"] == "spooky") "spooky" else "default"
            val miniGamesEnabled = parameters["minigames"] == "true"
            val activeMinigames = parameters.getAll("activeMinigames") ?: emptyList()

            call.sessions.set(session.copy(theme = theme))

            val game = addGame(quiz)
            val questions = retrieveQuizQuestions(quiz)
            val gameSession =
                GameSession(
                    gameId = game.id.value,
                    hostUsername = session.username,
                    questions = questions,
                    timeLimit = timeLimit,
                    miniGamesEnabled = miniGamesEnabled,
                    activeMinigames = activeMinigames,
                    theme = theme,
                )
            activeSessions[game.id.value] = gameSession

            // Generates a unique room code
            var roomCode: String?
            do {
                roomCode = generateRoomCode()
            } while (roomCodeAlreadyExists(roomCode))
            addInUseCode(roomCode, game)

            call.respondRedirect("/lobby-server/${game.id.value}")
        }

        // Serves host lobby view with joined players
        get("/lobby-server/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))

            val game =
                retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz =
                retrieveQuizFromGameID(gameId)
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val roomCode =
                retrieveRoomCode(game)
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Room code not found")))

            val players = retrieveGamePlayers(game)

            call.respond(
                PebbleContent(
                    "lobby_server_view.peb",
                    mapOf(
                        "quizName" to quiz.quiz_name,
                        "roomCode" to roomCode,
                        "players" to players,
                        "gameId" to gameId,
                        "theme" to session.theme,
                    ),
                ),
            )
        }

        // Serves join lobby page for client
        get("/join-lobby") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val returnURL =
                if (session.role == "teacher" || session.role == "admin") {
                    "/teacher-dashboard"
                } else {
                    "/student-dashboard"
                }

            call.respond(PebbleContent("join_lobby.peb", mapOf("returnURL" to returnURL, "roomCode" to "", "errorMessage" to "")))
        }

        // Validates room code, creates UserScore for client, redirects to client lobby view
        post("/join-lobby") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val parameters = call.receiveParameters()
            val roomCode =
                parameters["roomCode"]?.uppercase()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))

            val returnURL =
                if (session.role == "teacher" || session.role == "admin") {
                    "/teacher-dashboard"
                } else {
                    "/student-dashboard"
                }

            val inUseCode = retrieveInUseCode(roomCode)

            if (inUseCode == null) {
                call.respond(
                    PebbleContent(
                        "join_lobby.peb",
                        mapOf(
                            "returnURL" to returnURL,
                            "roomCode" to roomCode,
                            "errorMessage" to "Invalid room code, please try again",
                        ),
                    ),
                )
                return@post
            }
            val game =
                retrieveInUseCodeGame(inUseCode)
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))

            // Stop players joining after game started
            if (activeSessions[game.id.value]?.isStarted == true) {
                call.respond(
                    PebbleContent(
                        "join_lobby.peb",
                        mapOf(
                            "returnURL" to returnURL,
                            "roomCode" to roomCode,
                            "errorMessage" to "This Game has Already Started!",
                        ),
                    ),
                )
                return@post
            }

            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            val gameSession = activeSessions[game.id.value]

            // Block host from joining their own game
            if (gameSession != null && session.username == gameSession.hostUsername) {
                call.respond(
                    PebbleContent(
                        "join_lobby.peb",
                        mapOf(
                            "returnURL" to returnURL,
                            "roomCode" to roomCode,
                            "errorMessage" to "You cannot join your own game!",
                        ),
                    ),
                )
                return@post
            }

            val theme = gameSession?.theme ?: "default"
            call.sessions.set(session.copy(theme = theme))

            // Block previously removed players from joining back
            if (gameSession != null && session.username in gameSession.barredUsernames) {
                call.respond(
                    PebbleContent(
                        "join_lobby.peb",
                        mapOf(
                            "returnURL" to returnURL,
                            "roomCode" to roomCode,
                            "errorMessage" to "You have already been removed from this game!",
                        ),
                    ),
                )
                return@post
            }

            // Block players after the lobby cap is reached
            if (gameSession != null && retrieveGamePlayers(game).size >= gameSession.maxPlayers) {
                call.respond(
                    PebbleContent(
                        "join_lobby.peb",
                        mapOf(
                            "returnURL" to returnURL,
                            "roomCode" to roomCode,
                            "errorMessage" to "This lobby is full!",
                        ),
                    ),
                )
                return@post
            }

            addUserScore(game, user)
            call.respondRedirect("/lobby-client/${game.id.value}")
        }

        // Serves client lobby view and checks verified player is in the game
        get("/lobby-client/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))

            val game =
                retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz =
                retrieveQuizFromGameID(gameId)
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")
            retrieveUserScore(game, user)
                ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "You are not a player in this quiz")))

            val roomCode =
                retrieveRoomCode(game)
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Room code not found")))

            call.respond(
                PebbleContent(
                    "lobby_client_view.peb",
                    mapOf(
                        "quizName" to quiz.quiz_name,
                        "roomCode" to roomCode,
                        "gameId" to gameId,
                        "theme" to session.theme,
                    ),
                ),
            )
        }

        // Removes UserScore entry for player and redirects to their dashboard
        post("/leave-lobby/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val game =
                retrieveGame(gameId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            val test = deleteUserScoreForGivenUser(game, user)
            if (!test) {
                return@post call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Score for User not found"),
                    ),
                )
            }

            val returnURL =
                if (session.role == "teacher" || session.role == "admin") { // delete UserScore to remove player from game
                    "/teacher-dashboard"
                } else {
                    "/student-dashboard"
                }
            call.respondRedirect(returnURL)
        }

        // Deletes room code, player scores and game, then redirects host to their dashboard
        post("/close-lobby/{gameId}") {
            call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val game =
                retrieveGame(gameId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))

            // Always delete room code when lobby is closed
            deleteInUseCode(game)

            // If game did not start, delete game and scores
            if (activeSessions[gameId]?.isStarted != true) {
                deleteGameUserScores(game)
                transaction(TransactionManager.defaultDatabase) { game.delete() }
            }
            call.respondRedirect("/teacher-dashboard")
        }

        // Gets game session details, populated and serves settings page
        get("/settings/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Inavlid gameID")))
            val returnUrl = call.request.queryParameters["return"] ?: "/lobby-server/$gameId"
            val gameSession =
                activeSessions[gameId]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            call.respond(
                PebbleContent(
                    "settings.peb",
                    mapOf(
                        "gameId" to gameId,
                        "returnUrl" to returnUrl,
                        "theme" to session.theme,
                        "timeLimit" to gameSession.timeLimit,
                        "minigamesEnabled" to gameSession.miniGamesEnabled,
                        "mathsEnabled" to gameSession.activeMinigames.contains("maths"),
                        "potionsEnabled" to gameSession.activeMinigames.contains("potions"),
                        "roomsEnabled" to gameSession.activeMinigames.contains("rooms"),
                        "wordsEnabled" to gameSession.activeMinigames.contains("words"),
                    ),
                ),
            )
        }

        // Updates game session w/ new settings and redirects back to previous page
        post("/settings/{gameId}") {
            call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val returnUrl = call.request.queryParameters["return"] ?: "/lobby-server/$gameId"
            val gameSession =
                activeSessions[gameId]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val parameters = call.receiveParameters()
            val timeLimit = parameters["timeLimit"]?.toIntOrNull() ?: gameSession.timeLimit
            val minigamesEnabled = parameters["minigames"] == "true"
            val activeMinigames = parameters.getAll("activeMinigames") ?: emptyList()

            activeSessions[gameId] =
                gameSession.copy(
                    timeLimit = timeLimit,
                    miniGamesEnabled = minigamesEnabled,
                    activeMinigames = activeMinigames,
                )

            call.respondRedirect(returnUrl)
        }

        // Serves manage participant page with current player list
        get("/manage-participants/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@get call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respondText("Quiz not found")

            val players = retrieveGamePlayers(game)

            call.respond(
                PebbleContent(
                    "manage_participants.peb",
                    mapOf(
                        "gameId" to gameId,
                        "quizName" to quiz.quiz_name,
                        "players" to players,
                        "theme" to session.theme,
                    ),
                ),
            )
        }

        // Processes kick list, removes scores, redirects clients to kicked route and redirects host back to settings
        post("/manage-participants/{gameId}") {
            call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@post call.respondText("Game not found")
            val gameSession = activeSessions[gameId] ?: return@post call.respondText("No active session found")

            val parameters = call.receiveParameters()
            val kickedPlayers = parameters.getAll("kickedPlayers") ?: emptyList()

            transaction(TransactionManager.defaultDatabase) {
                kickedPlayers.forEach { username ->
                    val user = retrieveUser(username)
                    if (user != null) {
                        // Remove UserScore entry
                        UserScore
                            .find {
                                UserScoreTable.game eq game.id and (UserScoreTable.user eq user.id)
                            }.firstOrNull()
                            ?.delete()

                        // Remove ghost entry if ghost
                        gameSession.ghostPlayers.remove(username)
                    }
                }
            }

            gameSession.lastIncorrectPlayers = gameSession.lastIncorrectPlayers.filter { it !in kickedPlayers }
            gameSession.lastCorrectPlayers = gameSession.lastCorrectPlayers.filter { it !in kickedPlayers }

            gameSession.kickedUsernames.addAll(kickedPlayers)
            gameSession.barredUsernames.addAll(kickedPlayers)

            call.respondRedirect("/settings/$gameId")
        }

        // Serves client page depending on if they were kicked
        get("/kicked/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@get call.respondText("Game not found")
            val gameSession = activeSessions[gameId] ?: return@get call.respondText("No active session found")
            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            // Send host back to settings
            if (session.username == gameSession.hostUsername) {
                call.respondRedirect("/settings/$gameId")
                return@get
            }

            val userScore = retrieveUserScore(game, user)

            if (userScore == null) {
                // If their userScore doesn't exist, client was kicked
                call.respond(
                    PebbleContent(
                        "kicked.peb",
                        mapOf(
                            "gameId" to gameId,
                            "theme" to session.theme,
                        ),
                    ),
                )
            } else {
                // If userScore exists, they were not kicked
                call.respondRedirect("/client-position/$gameId")
            }
        }

        // Polling endpoint - returns if current user kicked
        get("/is-kicked/{gameId}") {
            val session =
                call.sessions.get<UserSession>() ?: return@get call.respondText("{\"kicked\":false}", ContentType.Application.Json)
            val gameId =
                call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("{\"kicked\":false}", ContentType.Application.Json)
            val gameSession = activeSessions[gameId]
            val kicked = gameSession?.kickedUsernames?.contains(session.username) ?: false
            call.respondText("{\"kicked\":$kicked}", ContentType.Application.Json)
        }

        // Polling endpoint - returns player list in lobby, for host lobby view
        get("/lobby-players/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("[]", ContentType.Application.Json)
            val game = retrieveGame(gameId) ?: return@get call.respondText("[]", ContentType.Application.Json)

            val players = retrieveGamePlayers(game)

            val json = "[" + players.joinToString(",") { "\"$it\"" } + "]"
            call.respondText(json, ContentType.Application.Json)
        }
    }
}
