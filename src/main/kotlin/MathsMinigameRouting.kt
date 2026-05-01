package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import com.example.database.objects.*
import io.ktor.websocket.Frame

fun Application.configureMathsMinigameRouting() {
    routing {

        //Maths Minigame
        //serves start screens to clients and host when host presses start
        post("/maths-begin/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            gameSession.connections.forEach { connection ->
                connection.send(Frame.Text("{\"redirect\":\"/minigame-question/$gameId\"}"))
            }

            gameSession.mathsGameState?.isStarted = true

            call.respond(PebbleContent("mini_game_maths_server.peb", mapOf(
                "quizName" to (retrieveQuizFromGameID(gameId)?.quiz_name ?: ""),
                "gameId" to gameId,
                "theme" to session.theme,
                "left_users" to gameSession.lastIncorrectPlayers.take(3),
                "right_users" to gameSession.lastIncorrectPlayers.drop(3)
            )))
        }

        //sends clients to uh-oh or safe screen, split by dangerPlayers
        get("/minigame-start/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            val userScore = retrieveUserScore(game, user)?.score ?: 0

            if (session.username in gameSession.lastIncorrectPlayers) {
                call.respond(PebbleContent("mini_game_uhoh.peb", mapOf(
                    "gameId" to gameId,
                    "score" to userScore,
                    "theme" to session.theme
                )))
            } else {
                call.respond(PebbleContent("minigame_client_safe_wait.peb", mapOf(
                    "gameId" to gameId,
                    "score" to userScore,
                    "theme" to session.theme
                )))
            }
        }

        get("/minigame-question/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val mathsState = gameSession.mathsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val questionIndex = mathsState.questionIndex.getOrDefault(session.username, 0)
            val mathsQuestion = mathsState.questions.getOrNull(questionIndex)
                ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No question available")))

            val options: List<String> = listOf(
                mathsQuestion.correctAnswer,
                mathsQuestion.option2,
                mathsQuestion.option3,
                mathsQuestion.option4
            ).shuffled()

            call.respond(PebbleContent("mini_game_maths_question.peb", mapOf(
                "gameId" to gameId,
                "question" to mathsQuestion.question,
                "correct_answer" to mathsQuestion.correctAnswer,
                "option1" to options[0],
                "option2" to options[1],
                "option3" to options[2],
                "option4" to options[3],
                "theme" to session.theme
            )))
        }

        get("/minigame-correct/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val mathsState = gameSession.mathsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            mathsState.mathsScores[session.username] =
                (mathsState.mathsScores.getOrDefault(session.username, 0)) + 1

            val currentIndex = mathsState.questionIndex.getOrDefault(session.username, 0)
            mathsState.questionIndex[session.username] = currentIndex + 1

            call.respondRedirect("/minigame-question/$gameId")
        }

        get("/minigame-incorrect/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            call.respond(PebbleContent("mini_game_maths_incorrect_ans.peb", mapOf(
                "gameId" to gameId,
                "theme" to session.theme
            )))
        }

        post("/minigame-end/{gameId}") {
            call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val mathsState = gameSession.mathsGameState ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val dangerScores = gameSession.lastIncorrectPlayers.associateWith { username ->
                mathsState.mathsScores.getOrDefault(username, 0)
            }

            val minScore = dangerScores.values.minOrNull() ?: 0

            mathsState.ghostPlayers = dangerScores
                .filter { it.value == minScore }
                .map { it.key }

            //consolation points for winning minigame, if players > 3
            if (gameSession.lastIncorrectPlayers.size >= 3) {
                //sort scores descending, disclude 0s
                val topScorers = mathsState.mathsScores
                    .filter { it.value > 0}
                    .entries
                    .sortedByDescending { it.value }

                transaction(TransactionManager.defaultDatabase) {
                    //winner - 200 points
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

                    //second - 100 points
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

            //add new ghosts to master list of ghosts in GameSession
            gameSession.ghostPlayers.addAll(mathsState.ghostPlayers)

            //mark minigame finished
            mathsState.isFinished = true

            gameSession.connections.forEach { connection -> connection.send(Frame.Text("{\"redirect\":\"/minigame-result/$gameId\"}")) }

            call.respond(HttpStatusCode.OK)
        }

        get("/minigame-end-host/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val mathsState = gameSession.mathsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            call.respond(PebbleContent("mini_game_maths_server_ghost.peb", mapOf(
                "gameId" to gameId,
                "theme" to session.theme,
                "ghostPlayers" to mathsState.ghostPlayers
            )))
        }

        get("/minigame-result/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            val mathsState = gameSession.mathsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val userScore = retrieveUserScore(game, user)?.score ?: 0

            val isExistingGhost = session.username in gameSession.ghostPlayers
                && session.username !in mathsState.ghostPlayers

            if (session.username in mathsState.ghostPlayers
                && session.username in gameSession.lastIncorrectPlayers) {
                // newly ghosted this minigame
                call.respond(PebbleContent("mini_game_maths_ur_ghost.peb", mapOf(
                    "gameId" to gameId,
                    "score" to userScore,
                    "theme" to session.theme
                )))
            } else {
                // safe or existing ghost
                call.respond(PebbleContent("mini_games_maths_ur_safe.peb", mapOf(
                    "gameId" to gameId,
                    "score" to userScore,
                    "theme" to session.theme,
                    "isExistingGhost" to isExistingGhost
                )))
            }
        }
    }
}