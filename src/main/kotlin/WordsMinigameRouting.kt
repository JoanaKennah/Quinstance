package com.example

import com.example.database.objects.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.websocket.Frame
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

// create an HTTP client to reuse every request
// used to make outgoing request to API using coroutines
private val httpClient = HttpClient(CIO)

fun Application.configureWordsMinigameRouting() {
    routing {
        // Words minigame -
        // serves start screens to clients and host
        post("/words-begin/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val wordsState =
                gameSession.wordsGameState
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            gameSession.connections.forEach { connection ->
                connection.send(Frame.Text("{\"redirect\":\"/words-start/$gameId\"}"))
            }

            gameSession.wordsGameState?.isStarted = true

            call.respond(
                PebbleContent(
                    "words_minigame_server_playing.peb",
                    mapOf(
                        "quizName" to (retrieveQuizFromGameID(gameId)?.quiz_name ?: ""),
                        "gameId" to gameId,
                        "theme" to session.theme,
                        "grid" to wordsState.grid,
                    ),
                ),
            )
        }

        // serves uh-oh or safe screen while host is on intro
        get("/words-wait/{gameId}") {
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

        // serves all clients pick screen - safe players play along
        get("/words-start/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val wordsState =
                gameSession.wordsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            call.respond(
                PebbleContent(
                    "words_minigame_client_pick.peb",
                    mapOf(
                        "gameId" to gameId,
                        "theme" to session.theme,
                        "grid" to wordsState.grid,
                        "errorMessage" to "",
                    ),
                ),
            )
        }

        // get word, validate against grid and dict, redirect if pass
        post("/words-submit/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val wordsState =
                gameSession.wordsGameState
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            val parameters = call.receiveParameters()
            val word =
                parameters["word"]?.uppercase()?.trim() ?: return@post call.respond(
                    PebbleContent(
                        "words_minigame_client_pick.peb",
                        mapOf(
                            "gameId" to gameId,
                            "theme" to session.theme,
                            "grid" to wordsState.grid,
                            "errorMessage" to "Please enter a valid word",
                        ),
                    ),
                )

            // check letters in word against letters in grid
            val gridLetterCounts = wordsState.grid.groupingBy { it }.eachCount()
            val wordLetterCounts = word.map { it.toString() }.groupingBy { it }.eachCount()
            val lettersValid =
                wordLetterCounts.all { (letter, count) ->
                    (gridLetterCounts[letter] ?: 0) >= count
                }

            if (!lettersValid) {
                call.respond(
                    PebbleContent(
                        "words_minigame_client_pick.peb",
                        mapOf(
                            "gameId" to gameId,
                            "theme" to session.theme,
                            "grid" to wordsState.grid,
                            "errorMessage" to "Please enter a valid word",
                        ),
                    ),
                )
                return@post
            }

            // check word against dict API
            val response = httpClient.get("https://api.dictionaryapi.dev/api/v2/entries/en/${word.lowercase()}")
            if (response.status != HttpStatusCode.OK) {
                call.respond(
                    PebbleContent(
                        "words_minigame_client_pick.peb",
                        mapOf(
                            "gameId" to gameId,
                            "theme" to session.theme,
                            "grid" to wordsState.grid,
                            "errorMessage" to "Please enter a valid word",
                        ),
                    ),
                )
                return@post
            }

            // if both checks pass store word and redirect
            wordsState.playerWords[session.username] = word

            call.respondRedirect("/words-selected/$gameId")
        }

        // serves client waiting screen after word submitted
        get("/words-selected/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val wordsState =
                gameSession.wordsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            val word =
                wordsState.playerWords[session.username]
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No word submitted")))

            call.respond(
                PebbleContent(
                    "words_minigame_client_selected.peb",
                    mapOf(
                        "gameId" to gameId,
                        "theme" to session.theme,
                        "word" to word,
                    ),
                ),
            )
        }

        // process and calculate results for players
        post("/words-end/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val wordsState =
                gameSession.wordsGameState
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            // players who submit late given empty string
            gameSession.lastIncorrectPlayers.forEach { username ->
                if (!wordsState.playerWords.containsKey(username)) {
                    wordsState.playerWords[username] = ""
                }
            }

            // get shortest word length from dangerPlayers
            val dangerPlayerWords =
                gameSession.lastIncorrectPlayers.associateWith { username ->
                    wordsState.playerWords.getOrDefault(username, "")
                }

            val minLength = dangerPlayerWords.values.minOfOrNull { it.length } ?: 0

            // add players with shortest word to ghosts
            wordsState.ghostPlayers =
                dangerPlayerWords
                    .filter { it.value.length == minLength }
                    .map { it.key }

            gameSession.ghostPlayers.addAll(wordsState.ghostPlayers)

            // award bonus points for longest words if >3 incorrect players
            if (gameSession.lastIncorrectPlayers.size >= 3) {
                val topScorers =
                    dangerPlayerWords
                        .filter { it.value.isNotEmpty() }
                        .entries
                        .sortedByDescending { it.value.length }

                transaction(TransactionManager.defaultDatabase) {
                    topScorers.getOrNull(0)?.let { (username, _) ->
                        val bonusUser = retrieveUser(username)
                        val bonusGame = retrieveGame(gameId)
                        if (bonusUser != null && bonusGame != null) {
                            val userScore = retrieveUserScore(bonusGame, bonusUser)
                            if (userScore != null) userScore.score += 200
                        }
                    }

                    topScorers.getOrNull(1)?.let { (username, _) ->
                        val bonusUser = retrieveUser(username)
                        val bonusGame = retrieveGame(gameId)
                        if (bonusUser != null && bonusGame != null) {
                            val userScore = retrieveUserScore(bonusGame, bonusUser)
                            if (userScore != null) userScore.score += 100
                        }
                    }
                }
            }

            // mark minigame finished and redirect clients
            wordsState.isFinished = true

            gameSession.connections.forEach { connection ->
                connection.send(Frame.Text("{\"redirect\":\"/words-result/$gameId\"}"))
            }

            call.respond(HttpStatusCode.OK)
        }

        // serves list of created words to host screen
        get("/words-end-host/{gameId}") {
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

            val wordsState =
                gameSession.wordsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            // build list of username-word pairs for template
            val playerWords =
                wordsState.playerWords.map { (username, word) ->
                    mapOf("username" to username, "word" to word.ifEmpty { "(no submission)" })
                }

            call.respond(
                PebbleContent(
                    "words_minigame_server_words.peb",
                    mapOf(
                        "quizName" to quiz.quiz_name,
                        "gameId" to gameId,
                        "theme" to session.theme,
                        "playerWords" to playerWords,
                    ),
                ),
            )
        }

        // serves shortest words and new ghosts for host screen
        post("/words-reveal/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId =
                call.parameters["gameId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession =
                activeSessions[gameId]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val quiz =
                retrieveQuizFromGameID(gameId)
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val wordsState =
                gameSession.wordsGameState
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            // get shortest words to display
            val shortestWords =
                wordsState.ghostPlayers.map { username ->
                    wordsState.playerWords.getOrDefault(username, "(no submission)")
                }

            call.respond(
                PebbleContent(
                    "words_minigame_server_outcome_ghost.peb",
                    mapOf(
                        "quizName" to quiz.quiz_name,
                        "gameId" to gameId,
                        "theme" to session.theme,
                        "shortestWords" to shortestWords,
                        "ghostPlayers" to wordsState.ghostPlayers,
                    ),
                ),
            )
        }

        // sends clients to respective results screens
        get("/words-result/{gameId}") {
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

            val wordsState =
                gameSession.wordsGameState
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            val userScore = retrieveUserScore(game, user)?.score ?: 0
            val isExistingGhost =
                session.username in gameSession.ghostPlayers &&
                    session.username !in wordsState.ghostPlayers

            if (session.username in wordsState.ghostPlayers &&
                session.username in gameSession.lastIncorrectPlayers
            ) {
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
