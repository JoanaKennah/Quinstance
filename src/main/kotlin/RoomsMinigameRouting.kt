package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.pebble.PebbleContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.websocket.Frame

fun Application.configureRoomsMinigameRouting() {
    // Routing for Rooms minigame
    routing {
        // Serves start screens to clients and host after host presses start
        post("/rooms-begin/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            gameSession.connections.forEach { connection ->
                connection.send(Frame.Text("{\"redirect\":\"/rooms-start/$gameId\"}"))
            }

            gameSession.roomsGameState?.isStarted = true

            call.respond(
                PebbleContent(
                    "rooms_minigame_server_playing.peb",
                    mapOf(
                        "quizName" to (retrieveQuizFromGameID(gameId)?.quizName ?: ""),
                        "gameId" to gameId,
                        "theme" to session.theme,
                    ),
                ),
            )
        }

        // Serves uh-oh or safe screen while host is on intro
        get("/rooms-wait/{gameId}") {
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

        // Serves room client pick screen to client
        get("/rooms-start/{gameId}") {
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
                        "rooms_minigame_client_pick.peb",
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

        // Records room pick and sends client to waiting screen
        post("/rooms-pick/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val roomsState =
                gameSession.roomsGameState
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val parameters = call.receiveParameters()
            val room =
                parameters["room"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid selection")))

            // Validation just in case
            if (room !in listOf("A", "B", "C")) {
                return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid selection")))
            }

            // Client room pick
            roomsState.playerPicks[session.username] = room

            call.respondRedirect("/rooms-selected/$gameId")
        }

        // Serves room selection screen to client
        get("/rooms-selected/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val roomsState =
                gameSession.roomsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val room =
                roomsState.playerPicks[session.username]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid selection")))

            call.respond(
                PebbleContent(
                    "rooms_minigame_client_selectd.peb",
                    mapOf(
                        "gameId" to gameId,
                        "room" to room,
                        "theme" to session.theme,
                    ),
                ),
            )
        }

        // Counts all client room picks and determines which room is the ghost room
        post("/rooms-end/{gameId}") {
            call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val roomsState =
                gameSession.roomsGameState
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            // Assign random room to late picks
            gameSession.lastIncorrectPlayers.forEach { username ->
                if (!roomsState.playerPicks.containsKey(username)) {
                    roomsState.playerPicks[username] = listOf("A", "B", "C").random()
                }
            }

            // Count picks per room
            val roomCounts =
                mapOf(
                    "A" to roomsState.playerPicks.count { it.value == "A" },
                    "B" to roomsState.playerPicks.count { it.value == "B" },
                    "C" to roomsState.playerPicks.count { it.value == "C" },
                )

            // Check if all players in same room
            val uniqueRooms = roomsState.playerPicks.values.toSet()
            if (uniqueRooms.size == 1) {
                roomsState.allSafe = true
                roomsState.isFinished = true

                gameSession.connections.forEach { connection -> connection.send(Frame.Text("{\"redirect\":\"/rooms-result/$gameId\"}")) }

                call.respond(HttpStatusCode.OK)
                return@post
            }

            // Find room with most players - makes a random pick if its a tie
            val maxCount = roomCounts.values.maxOrNull() ?: 0
            val busyRooms = roomCounts.filter { it.value == maxCount }.keys.toList()
            roomsState.ghostRoom = busyRooms.random()

            // Determine players in the picked room (ghost room)
            roomsState.newGhosts =
                roomsState.playerPicks
                    .filter { it.value == roomsState.ghostRoom }
                    .map { it.key }

            // Add new ghosts to list
            gameSession.ghostPlayers.addAll(roomsState.newGhosts)

            roomsState.isFinished = true

            // Send all clients to results page
            gameSession.connections.forEach { connection -> connection.send(Frame.Text("{\"redirect\":\"/rooms-result/$gameId\"}")) }

            call.respond(HttpStatusCode.OK)
        }

        // Serves rooms minigame outcome screen to client. If they are all safe, serve all safe screen, else serve ghost outcome screen
        get("/rooms-end-host/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val roomsState =
                gameSession.roomsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            if (roomsState.allSafe) {
                call.respond(
                    PebbleContent(
                        "rooms_minigame_server_outcome_allsafe.peb",
                        mapOf(
                            "gameId" to gameId,
                            "theme" to session.theme,
                        ),
                    ),
                )
            } else {
                val ghosts = roomsState.newGhosts
                val leftCol1 = ghosts.take(4)
                val rightCol1 = ghosts.drop(4).take(4)
                val leftCol2 = ghosts.drop(8).take(4)
                val rightCol2 = ghosts.drop(12).take(4)

                call.respond(
                    PebbleContent(
                        "rooms_minigame_server_outcome_ghost.peb",
                        mapOf(
                            "gameId" to gameId,
                            "theme" to session.theme,
                            "ghostRoom" to roomsState.ghostRoom,
                            "left_col1" to leftCol1,
                            "right_col1" to rightCol1,
                            "left_col2" to leftCol2,
                            "right_col2" to rightCol2,
                        ),
                    ),
                )
            }
        }

        // Serve room minigame result to client, telling them if they have become a ghost or not
        get("/rooms-result/{gameId}") {
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

            val roomsState =
                gameSession.roomsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val userScore = retrieveUserScore(game, user)?.score ?: 0
            val isExistingGhost =
                session.username in gameSession.ghostPlayers &&
                    session.username !in roomsState.newGhosts

            if (session.username in roomsState.newGhosts &&
                session.username in gameSession.lastIncorrectPlayers
            ) {
                // Newly ghosted
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
                // Safe or already existing ghost
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
