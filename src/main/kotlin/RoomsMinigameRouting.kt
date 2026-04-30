package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import com.example.database.objects.*
import io.ktor.websocket.Frame

fun Application.configureRoomsMinigameRouting() {
    routing {

        //Rooms Minigame -
        //serves start screens to clients and host after host presses start
        post("/rooms-begin/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            gameSession.connections.forEach { connection ->
                connection.send(Frame.Text("{\"redirect\":\"/rooms-start/$gameId\"}"))
            }

            gameSession.roomsGameState?.isStarted = true

            call.respond(PebbleContent("rooms_minigame_server_playing.peb", mapOf(
                "quizName" to (retrieveQuizFromGameID(gameId)?.quiz_name ?: ""),
                "gameId" to gameId,
                "theme" to session.theme
            )))
        }

        //serves uh-oh or safe screen while host is on intro
        get("/rooms-wait/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            val userScore = retrieveUserScore(game, user)?.score ?: 0
            val isExistingGhost = session.username in gameSession.ghostPlayers

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
                    "theme" to session.theme,
                    "isExistingGhost" to isExistingGhost
                )))
            }
        }

        get("/rooms-start/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            val userScore = retrieveUserScore(game, user)?.score ?: 0
            val isExistingGhost = session.username in gameSession.ghostPlayers

            if (session.username in gameSession.lastIncorrectPlayers) {
                call.respond(PebbleContent("rooms_minigame_client_pick.peb", mapOf(
                    "gameId" to gameId,
                    "theme" to session.theme
                )))
            } else {
                call.respond(PebbleContent("mini_games_maths_ur_safe.peb", mapOf(
                    "gameId" to gameId,
                    "score" to userScore,
                    "theme" to session.theme,
                    "isExistingGhost" to isExistingGhost
                )))
            }
        }

        //records room pick and sends client to waiting screen
        post("/rooms-pick/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val roomsState = gameSession.roomsGameState ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val parameters = call.receiveParameters()
            val room = parameters["room"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid selection")))

            // validation just in case
            if (room !in listOf("A", "B", "C")) {
                return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid selection")))
            }

            //record pick
            roomsState.playerPicks[session.username] = room

            call.respondRedirect("/rooms-selected/$gameId")
        }

        get("/rooms-selected/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val roomsState = gameSession.roomsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val room = roomsState.playerPicks[session.username] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid selection")))

            call.respond(PebbleContent("rooms_minigame_client_selectd.peb", mapOf(
                "gameId" to gameId,
                "room" to room,
                "theme" to session.theme,
            )))
        }

        post("/rooms-end/{gameId}") {
            call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val roomsState = gameSession.roomsGameState ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            //assign random room to late picks
            gameSession.lastIncorrectPlayers.forEach { username ->
                if (!roomsState.playerPicks.containsKey(username)) {
                    roomsState.playerPicks[username] = listOf("A", "B", "C").random()
                }
            }

            //count picks per room
            val roomCounts = mapOf(
                "A" to roomsState.playerPicks.count { it.value == "A" },
                "B" to roomsState.playerPicks.count { it.value == "B" },
                "C" to roomsState.playerPicks.count { it.value == "C" }
            )

            //check if all players in same room
            val uniqueRooms = roomsState.playerPicks.values.toSet()
            if (uniqueRooms.size == 1) {
                roomsState.allSafe = true
                roomsState.isFinished = true

                gameSession.connections.forEach { connection -> connection.send(Frame.Text("{\"redirect\":\"/rooms-result/$gameId\"}")) }

                call.respond(HttpStatusCode.OK)
                return@post
            }

            //find room w/ most players - ramdom pick if tie
            val maxCount = roomCounts.values.maxOrNull() ?: 0
            val busyRooms = roomCounts.filter { it.value == maxCount }.keys.toList()
            roomsState.ghostRoom = busyRooms.random()

            //determine players in ghost roomm
            roomsState.newGhosts = roomsState.playerPicks
                .filter { it.value == roomsState.ghostRoom }
                .map { it.key }

            //add new ghosts to list
            gameSession.ghostPlayers.addAll(roomsState.newGhosts)

            roomsState.isFinished = true

            //send all clients to results page
            gameSession.connections.forEach { connection -> connection.send(Frame.Text("{\"redirect\":\"/rooms-result/$gameId\"}")) }

            call.respond(HttpStatusCode.OK)
        }

        get("/rooms-end-host/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val roomsState = gameSession.roomsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            if (roomsState.allSafe) {
                call.respond(PebbleContent("rooms_minigame_server_outcome_allsafe.peb", mapOf(
                    "gameId" to gameId,
                    "theme" to session.theme
                )))
            } else {
                val ghosts = roomsState.newGhosts
                val leftCol1 = ghosts.take(4)
                val rightCol1 = ghosts.drop(4).take(4)
                val leftCol2 = ghosts.drop(8).take(4)
                val rightCol2 = ghosts.drop(12).take(4)

                call.respond(PebbleContent("rooms_minigame_server_outcome_ghost.peb", mapOf(
                    "gameId" to gameId,
                    "theme" to session.theme,
                    "ghostRoom" to roomsState.ghostRoom,
                    "left_col1" to leftCol1,
                    "right_col1" to rightCol1,
                    "left_col2" to leftCol2,
                    "right_col2" to rightCol2
                )))
            }
        }

        get("/rooms-result/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            val roomsState = gameSession.roomsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val userScore = retrieveUserScore(game, user)?.score ?: 0
            val isExistingGhost = session.username in gameSession.ghostPlayers
                && session.username !in roomsState.newGhosts

            if (session.username in roomsState.newGhosts
                && session.username in gameSession.lastIncorrectPlayers) {
                // newly ghosted
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