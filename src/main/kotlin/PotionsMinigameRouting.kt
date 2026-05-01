package com.example

import com.example.database.objects.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.websocket.Frame
import kotlinx.serialization.json.Json

fun Application.configurePotionsMinigameRouting() {
    routing {
        // Potions Minigame -
        // serves start screens to clients and host when host presses start
        post("/potions-begin/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            gameSession.connections.forEach { connection ->
                connection.send(Frame.Text("{\"redirect\":\"/potions-start/$gameId\"}"))
            }

            gameSession.potionsGameState?.isStarted = true

            call.respond(
                PebbleContent(
                    "potions_minigame_server.peb",
                    mapOf(
                        "quizName" to (retrieveQuizFromGameID(gameId)?.quiz_name ?: ""),
                        "gameId" to gameId,
                        "theme" to session.theme,
                        "isReveal" to false,
                        "ghostTrophy" to 0,
                        "col1_users" to emptyList<String>(),
                        "col2_users" to emptyList<String>(),
                        "col3_users" to emptyList<String>(),
                        "isGhostCol1" to false,
                        "isGhostCol2" to false,
                        "isGhostCol3" to false,
                    ),
                ),
            )
        }

        // serves uh-oh or safe screen while host is on intro
        get("/potions-wait/{gameId}") {
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
            val isExistingGhost = session.username in gameSession.ghostPlayers

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
                            "isExistingGhost" to isExistingGhost,
                        ),
                    ),
                )
            }
        }

        // sends clients to potions pick or safe screen
        get("/potions-start/{gameId}") {
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
            val isExistingGhost = session.username in gameSession.ghostPlayers

            if (session.username in gameSession.lastIncorrectPlayers) {
                call.respond(
                    PebbleContent(
                        "potions_minigame_client_pick.peb",
                        mapOf(
                            "gameId" to gameId,
                            "theme" to session.theme,
                        ),
                    ),
                )
            } else {
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

        // records potion pick and sends client to waiting screen
        post("/potions-pick/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val potionsState =
                gameSession.potionsGameState
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val parameters = call.receiveParameters()
            val potion =
                parameters["potion"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid selection")))

            // record pick
            potionsState.playerPicks[session.username] = potion

            call.respondRedirect("/potions-selected/$gameId")
        }

        // serves waiting screen after potion pick
        get("/potions-selected/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val potionsState =
                gameSession.potionsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val potion =
                potionsState.playerPicks[session.username]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No potion selected")))

            call.respond(
                PebbleContent(
                    "potions_minigame_client_selectd_ans.peb",
                    mapOf(
                        "gameId" to gameId,
                        "potion" to potion,
                        "theme" to session.theme,
                    ),
                ),
            )
        }

        get("/potions-picks/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("{}", ContentType.Application.Json)
            val potionsState = activeSessions[gameId]?.potionsGameState ?: return@get call.respondText("{}", ContentType.Application.Json)

            // assign players to columns on server screen, arbitrary
            val players = potionsState.playerPicks.keys.toList()
            val column1 = players.take(2)
            val column2 = players.drop(2).take(2)
            val column3 = players.drop(4).take(2)

            val response =
                PotionsPicksResponse(
                    column1 = column1,
                    column2 = column2,
                    column3 = column3,
                    ghostUsers = potionsState.newGhosts,
                    isFinished = potionsState.isFinished,
                )
            call.respondText(Json.encodeToString(response), ContentType.Application.Json)
        }

        post("/potions-end/{gameId}") {
            call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val potionsState =
                gameSession.potionsGameState
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            // assign random potion to any danger player who didn't pick in time
            gameSession.lastIncorrectPlayers.forEach { username ->
                if (!potionsState.playerPicks.containsKey(username)) {
                    potionsState.playerPicks[username] = (1..4).random()
                }
            }

            // determine who picked the ghost potion
            potionsState.newGhosts =
                potionsState.playerPicks
                    .filter { it.value == potionsState.ghostPotion }
                    .map { it.key }

            // add new ghosts to persistent game session ghost list
            gameSession.ghostPlayers.addAll(potionsState.newGhosts)

            // mark minigame finished
            potionsState.isFinished = true

            // notify all clients to go to result page
            gameSession.connections.forEach { connection -> connection.send(Frame.Text("{\"redirect\":\"/potions-result/$gameId\"}")) }

            call.respond(HttpStatusCode.OK)
        }

        // serves potion reveal to host
        get("/potions-end-host/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val quiz =
                retrieveQuizFromGameID(gameId)
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val potionsState =
                gameSession.potionsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val players = potionsState.playerPicks.keys.toList()
            val col1 = players.take(2)
            val col2 = players.drop(2).take(2)
            val col3 = players.drop(4).take(2)

            call.respond(
                PebbleContent(
                    "potions_minigame_server.peb",
                    mapOf(
                        "quizName" to quiz.quiz_name,
                        "gameId" to gameId,
                        "theme" to session.theme,
                        "isReveal" to true,
                        "ghostTrophy" to potionsState.ghostPotion,
                        "col1_users" to col1,
                        "col2_users" to col2,
                        "col3_users" to col3,
                        "isGhostCol1" to false,
                        "isGhostCol2" to false,
                        "isGhostCol3" to false,
                    ),
                ),
            )
        }

        // serves personalised potions result to client
        get("/potions-result/{gameId}") {
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

            val potionsState =
                gameSession.potionsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val userScore = retrieveUserScore(game, user)?.score ?: 0
            val isExistingGhost =
                session.username in gameSession.ghostPlayers &&
                    session.username !in potionsState.newGhosts

            if (session.username in potionsState.newGhosts &&
                session.username in gameSession.lastIncorrectPlayers
            ) {
                // newly ghosted
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
                // safe or existing ghost
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
