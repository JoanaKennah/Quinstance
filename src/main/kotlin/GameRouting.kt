package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.core.eq
import com.example.database.objects.*
import com.example.database.tables.*
import io.ktor.websocket.Frame
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

fun Application.configureGameRouting() {
    routing {

        //serves question page client-side, websocket connects here during game
        get("/game/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            call.respond(PebbleContent("playing_question_client.peb", mapOf(
                "username" to session.username,
                "quizName" to quiz.quiz_name,
                "gameId" to gameId,
                "theme" to session.theme,
                "timeLimit" to (activeSessions[gameId]?.timeLimit ?: 30)
            )))
        }

        //set isStarted flag, sends redirect to clients, broadcasts Question 1 to host
        post("/start-game/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            retrieveGame(gameId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val questions = retrieveQuizQuestions(quiz)                                        //load questions into quiz
            if (questions.isEmpty()) {
                return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No questions found in this quiz")))
            }

            val gameSession = activeSessions[gameId]
                ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            gameSession.isStarted = true

            //redirect all connected players to game page
            gameSession.connections.forEach { connection -> connection.send(Frame.Text("{\"redirect\":\"/game/$gameId\"}"))}

            // Small delay allows clients to connect to new WebSocket
            delay(1000)

            //Broadcast first question to connected players
            broadcastQuestion(gameSession)

            call.respond(PebbleContent("playing_question_server.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "questionNumber" to 1,
                "questionText" to questions[0].question,
                "gameId" to gameId,
                "theme" to session.theme,
                "timeLimit" to gameSession.timeLimit
            )))
        }

        //increments question index, broadcast next Q or ends game after last Q
        post("/next-question/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")

            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val game = retrieveGame(gameId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            // clear maths game &autoghost state ready for next question
            gameSession.mathsGameState = null
            gameSession.potionsGameState = null
            gameSession.roomsGameState = null
            gameSession.wordsGameState = null
            gameSession.autoGhostedPlayer = null
            gameSession.minigameJustPlayed = false
            gameSession.answeredPlayers.clear()

            if (gameSession.isAdvanced) {
                return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Already advancing")))
            }
            gameSession.isAdvanced = true

            gameSession.currentQuestionIndex++          //advance quiz to next Q

            if (gameSession.currentQuestionIndex >= gameSession.questions.size) {
                gameSession.isFinished = true
                transaction(TransactionManager.defaultDatabase) {
                    gameSession.ghostPlayers.forEach { username ->
                        val ghostUser = retrieveUser(username)
                        if (ghostUser != null) {
                            val userScore = retrieveUserScore(game, ghostUser)
                            userScore?.isGhost = true
                        }
                    }
                }

                // send clients to end game screen before session ends
                gameSession.connections.forEach { connection ->
                    connection.send(Frame.Text("{\"redirect\":\"/end-game/$gameId\"}"))
                }

                activeSessions.remove(gameId)           //cut game and go to leaderboard
                deleteInUseCode(game)
                call.respondRedirect("/leaderboard/$gameId")

            } else {
                gameSession.isAdvanced = false          //reset flag for next Q
                //added missing websocket redirect since broadcastQuestion doesn't redirect, only sends Q data
                gameSession.connections.forEach { connection ->
                    connection.send(Frame.Text("{\"redirect\":\"/game/$gameId\"}"))
                }
                delay(1000)
                broadcastQuestion(gameSession)

                val nextQuestion = gameSession.questions[gameSession.currentQuestionIndex]
                call.respond(PebbleContent("playing_question_server.peb", mapOf(
                    "quizName" to quiz.quiz_name,
                    "questionNumber" to gameSession.currentQuestionIndex + 1,
                    "questionText" to nextQuestion.question,
                    "gameId" to gameId,
                    "timeLimit" to gameSession.timeLimit,
                    "theme" to session.theme
                )))
            }
        }

        // Serves final leaderboard at end of game
        get("/leaderboard/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            // Retrieve player scores, sort by highest
            val allUserScores = retrieveAllUserScoresDescending(game)
            val players = allUserScores.map {
                mapOf<String, Any>("username" to it.username, "score" to it.score, "isGhost" to it.isGhost)
            }

            call.respond(PebbleContent("end_leaderboard.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "players" to players,
                "gameId" to gameId,
                "quizId" to quiz.id,
                "theme" to session.theme
            )))
        }

        // Quit button back to dashboard from leaderboards
        post("/leaderboard-quit/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val returnURL = if (session.role == "teacher" || session.role == "admin") {
                "/teacher-dashboard"
            } else {
                "/student-dashboard"
            }

            call.respondRedirect(returnURL)
        }

        //serves selected answer template
        get("/selected-answer/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val selectedAnswer = call.request.queryParameters["answer"] ?: ""

            call.respond(PebbleContent("client_selected_ans.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "gameId" to gameId,
                "selectedAnswer" to selectedAnswer,
                "theme" to session.theme
            )))
        }

        //scores answers, clears UserAnswers, serves correct ans pages for clients and host
        post("/show-correct-answer/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invaid gameID")))
            val game = retrieveGame(gameId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            // Get current Q and correct ans
            val currentQuestion = gameSession.questions[gameSession.currentQuestionIndex]
            val correctAnswer = retrieveQuestionAnswer(currentQuestion)?.correct_answer ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No correct answer found for this question")))

            val answers = UserAnswers.allUserAnswers()

            gameSession.revivedPlayers.clear()

            transaction(TransactionManager.defaultDatabase) {
                for (answerData in answers) {
                    if (answerData.chosenAnswer == correctAnswer) {
                        val user = retrieveUser(answerData.username)
                        if (user != null) {
                            //check if player isghost
                            if (answerData.username in gameSession.ghostPlayers) {
                                gameSession.ghostPlayers.remove(answerData.username)
                                gameSession.revivedPlayers.add(answerData.username)
                            } else {
                                val userScore = retrieveUserScore(game, user)
                                if (userScore != null) {
                                    val pointsEarned = if (gameSession.timeLimit == 0) {
                                        1000
                                    } else {
                                        (100 + ((answerData.timeRemaining.toDouble() / gameSession.timeLimit.toDouble()) * 900)).roundToInt()
                                    }
                                    userScore.score += pointsEarned
                                }
                            }
                        }
                    }
                }
            }

            //store (in)correct answering players before clearing answers
            val dangerPlayers = answers
                .filter { it.chosenAnswer != correctAnswer }
                .filter { it.username !in gameSession.ghostPlayers }
                .map { it.username }
                .shuffled()

            gameSession.lastIncorrectPlayers = dangerPlayers
            gameSession.lastCorrectPlayers = answers
                .map { it.username }
                .filter { it !in dangerPlayers }

            // Clear answers ready for next question
            UserAnswers.clearAnswers()

            // Send clients to correct ans page
            gameSession.connections.forEach { connection -> connection.send(Frame.Text("{\"redirect\":\"/show-correct-answer-client/$gameId\"}"))}

            //send host to correct ans view w/ next button
            call.respond(PebbleContent("question_with_correct_ans.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "questionNumber" to gameSession.currentQuestionIndex + 1,
                "questionText" to currentQuestion.question,
                "correctAnswer" to correctAnswer,
                "gameId" to gameId,
                "isHost" to true,
                "theme" to session.theme
            )))
        }

        //serves correct ans for clients, Websocket waits for host to advance
        get("/show-correct-answer-client/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val currentQuestion = gameSession.questions[gameSession.currentQuestionIndex]
            val correctAnswer = retrieveQuestionAnswer(currentQuestion)?.correct_answer ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No correct answer found for this question")))

            //check if player was revived
            if (session.username in gameSession.revivedPlayers) {
                val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")
                val userScore = retrieveUserScore(game, user)?.score ?: 0

                call.respond(PebbleContent("revival.peb", mapOf(
                    "gameId" to gameId,
                    "score" to userScore,
                    "theme" to session.theme
                )))
                return@get
            }

            call.respond(PebbleContent("question_with_correct_ans.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "questionNumber" to gameSession.currentQuestionIndex + 1,
                "questionText" to currentQuestion.question,
                "correctAnswer" to correctAnswer,
                "gameId" to gameId,
                "isHost" to false,
                "theme" to session.theme
            )))
        }

        //serves correct ans page to host after timer expires
        get("/show-correct-answer/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            //get current Q plus correct ans
            val currentQuestion = gameSession.questions[gameSession.currentQuestionIndex]
            val correctAnswer = retrieveQuestionAnswer(currentQuestion)?.correct_answer ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No correct answer found for this question")))

            call.respond(PebbleContent("question_with_correct_ans.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "questionNumber" to gameSession.currentQuestionIndex + 1,
                "questionText" to currentQuestion.question,
                "correctAnswer" to correctAnswer,
                "gameId" to gameId,
                "isHost" to true,
                "theme" to session.theme
            )))
        }

        // Serves mid-game leaderboard between questions
        post("/show-positions/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val game = retrieveGame(gameId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val scores = transaction(TransactionManager.defaultDatabase) {
                UserScore.find { UserScoreTable.game eq game.id }
                    .sortedByDescending { it.score }
                    .map { userScore ->
                        mapOf<String, Any>(
                            "username" to userScore.user.username,
                            "score" to userScore.score,
                            "isGhost" to (userScore.user.username in gameSession.ghostPlayers)
                        )
                    }
            }

            val testRandomValue = (0..1).random()
            when {
                gameSession.miniGamesEnabled && gameSession.lastIncorrectPlayers.size == 1 -> {
                    val autoGhostedPlayer = gameSession.lastIncorrectPlayers[0]
                    gameSession.ghostPlayers.add(autoGhostedPlayer)
                    gameSession.autoGhostedPlayer = autoGhostedPlayer

                    // recalculate scores after adding ghost so isGhost reflects correctly
                    val updatedScores = transaction(TransactionManager.defaultDatabase) {
                        UserScore.find { UserScoreTable.game eq game.id }
                            .sortedByDescending { it.score }
                            .map { userScore ->
                                mapOf<String, Any>(
                                    "username" to userScore.user.username,
                                    "score" to userScore.score,
                                    "isGhost" to (userScore.user.username in gameSession.ghostPlayers)
                                )
                            }
                    }

                    gameSession.connections.forEach { connection -> connection.send(Frame.Text("{\"redirect\":\"/auto-ghost-result/$gameId\"}")) }

                    call.respond(PebbleContent("leaderboard.peb", mapOf(
                        "quizName" to quiz.quiz_name,
                        "players" to updatedScores,
                        "gameId" to gameId,
                        "quizId" to quiz.id,
                        "theme" to session.theme
                    )))
                }

                //When finished testing, swap out random line
                gameSession.miniGamesEnabled
                    && gameSession.lastIncorrectPlayers.size >= 2
                    && gameSession.activeMinigames.isNotEmpty()
                    && !gameSession.minigameJustPlayed
                    //&& (0..1).random() == 0 -> {
                    && testRandomValue == 1 -> {

                    gameSession.minigameJustPlayed = true
                    val selectedMinigame = gameSession.activeMinigames.random()

                    println("miniGamesEnabled: ${gameSession.miniGamesEnabled}, incorrectPlayers: ${gameSession.lastIncorrectPlayers.size}, activeMinigames: ${gameSession.activeMinigames}, minigameJustPlayed: ${gameSession.minigameJustPlayed}, random: $testRandomValue")

                    when (selectedMinigame) {
                        "maths" -> {
                            gameSession.mathsGameState = MathsGameState()

                            gameSession.connections.forEach { connection -> connection.send(Frame.Text("{\"redirect\":\"/minigame-start/$gameId\"}")) }

                            call.respond(PebbleContent("minigame_server_intro.peb", mapOf(
                                "quizName" to quiz.quiz_name,
                                "gameId" to gameId,
                                "theme" to session.theme,
                                "minigameName" to "Maths",
                                "beginRoute" to "/maths-begin/$gameId"
                            )))
                        }

                        "potions" -> {
                            gameSession.lastIncorrectPlayers = gameSession.lastIncorrectPlayers.take(6)
                            gameSession.potionsGameState = PotionsGameState()

                            gameSession.connections.forEach { connection -> connection.send(Frame.Text("{\"redirect\":\"/potions-wait/$gameId\"}")) }

                            call.respond(PebbleContent("minigame_server_intro.peb", mapOf(
                                "quizName" to quiz.quiz_name,
                                "gameId" to gameId,
                                "theme" to session.theme,
                                "minigameName" to "Potions",
                                "beginRoute" to "/potions-begin/$gameId"
                            )))
                        }

                        "rooms" -> {
                            gameSession.lastIncorrectPlayers = gameSession.lastIncorrectPlayers.take(16)
                            gameSession.roomsGameState = RoomsGameState()

                            gameSession.connections.forEach { connection -> connection.send(Frame.Text("{\"redirect\":\"/rooms-wait/$gameId\"}")) }

                            call.respond(PebbleContent("minigame_server_intro.peb", mapOf(
                                "quizName" to quiz.quiz_name,
                                "gameId" to gameId,
                                "theme" to session.theme,
                                "minigameName" to "Rooms",
                                "beginRoute" to "/rooms-begin/$gameId"
                            )))
                        }

                        "words" -> {
                            gameSession.wordsGameState = WordsGameState()

                            gameSession.connections.forEach { connection ->
                                connection.send(Frame.Text("{\"redirect\":\"/words-wait/$gameId\"}"))
                            }

                            call.respond(PebbleContent("minigame_server_intro.peb", mapOf(
                                "quizName" to quiz.quiz_name,
                                "gameId" to gameId,
                                "theme" to session.theme,
                                "minigameName" to "Words",
                                "beginRoute" to "/words-begin/$gameId"
                            )))
                        }

                        else -> {
                            gameSession.connections.forEach { connection ->
                                connection.send(Frame.Text("{\"redirect\":\"/client-position/$gameId\"}"))
                            }

                            call.respond(PebbleContent("leaderboard.peb", mapOf(
                                "quizName" to quiz.quiz_name,
                                "players" to scores,
                                "gameId" to gameId,
                                "quizId" to quiz.id,
                                "theme" to session.theme
                            )))
                        }
                    }
                }

                else -> {
                    gameSession.connections.forEach { connection ->
                        connection.send(Frame.Text("{\"redirect\":\"/client-position/$gameId\"}"))
                    }

                    call.respond(PebbleContent("leaderboard.peb", mapOf(
                        "quizName" to quiz.quiz_name,
                        "players" to scores,
                        "gameId" to gameId,
                        "quizId" to quiz.id,
                        "theme" to session.theme
                    )))
                }
            }
        }

        // Serves personal position and score to each player, personalised
        get("/client-position/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            val allScores = retrieveAllUserScoresDescending(game)

            // Find score and position per player
            val playerScore = allScores.indexOfFirst { it.userID == user.id.value }
            val position = playerScore + 1
            val score = allScores.getOrNull(playerScore)?.score ?: 0
            val totalPlayers = allScores.size
            val questionIndex: Int = activeSessions[gameId]?.currentQuestionIndex ?: 0

            call.respond(PebbleContent("client_current_pos.peb", mapOf<String, Any>(
                "quizName" to quiz.quiz_name,
                "gameId" to gameId,
                "position" to position,
                "score" to score,
                "totalPlayers" to totalPlayers,
                "questionIndex" to questionIndex,
                "theme" to session.theme
            )))
        }

        // Serves client view at end of game
        get("/end-game/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))

            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            val allScores = retrieveAllUserScoresDescending(game)

            val playerScore = allScores.indexOfFirst { it.userID == user.id.value }
            val position = playerScore + 1
            val score = allScores.getOrNull(playerScore)?.score ?: 0
            val totalPlayers = allScores.size

            call.respond(PebbleContent("end_game_client.peb", mapOf<String, Any>(
                "quizName" to quiz.quiz_name,
                "gameId" to gameId,
                "position" to position,
                "score" to score,
                "totalPlayers" to totalPlayers,
                "theme" to session.theme
            )))
        }

        get("/auto-ghost-result/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            if (session.username == gameSession.autoGhostedPlayer) {
                val userScore = retrieveUserScore(game, user)?.score ?: 0
                call.respond(PebbleContent("mini_game_maths_ur_ghost.peb", mapOf(
                    "gameId" to gameId,
                    "score" to userScore,
                    "theme" to session.theme
                )))
            } else {
                val allScores = retrieveAllUserScoresDescending(game)
                val playerScore = allScores.indexOfFirst { it.userID == user.id.value }
                val position = playerScore + 1
                val score = allScores.getOrNull(playerScore)?.score ?: 0
                val totalPlayers = allScores.size
                val questionIndex: Int = gameSession.currentQuestionIndex

                call.respond(PebbleContent("client_current_pos.peb", mapOf<String, Any>(
                    "quizName" to quiz.quiz_name,
                    "gameId" to gameId,
                    "position" to position,
                    "score" to score,
                    "totalPlayers" to totalPlayers,
                    "questionIndex" to questionIndex,
                    "theme" to session.theme
                )))
            }
        }

        get("/leaderboard-return/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val scores = transaction(TransactionManager.defaultDatabase) {
                UserScore.find { UserScoreTable.game eq game.id }
                    .sortedByDescending { it.score }
                    .map { userScore ->
                        mapOf<String, Any>(
                            "username" to userScore.user.username,
                            "score" to userScore.score,
                            "isGhost" to (userScore.user.username in gameSession.ghostPlayers)
                        )
                    }
            }

            call.respond(PebbleContent("leaderboard.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "players" to scores,
                "gameId" to gameId,
                "quizId" to quiz.id,
                "theme" to session.theme
            )))
        }

        //polling endpoint, returns if game has started, used by lobby client view
        get("/game-started/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull()
                ?: return@get call.respondText("{\"started\":false}", ContentType.Application.Json)
            val started = activeSessions[gameId]?.isStarted ?: false
            call.respondText("{\"started\":$started}", ContentType.Application.Json)
        }

        //polling endpoint, returns current Q index and finished flag, used by client pos page
        get("/question-index/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("{\"index\":0}", ContentType.Application.Json)
            val session = activeSessions[gameId]
            val index = activeSessions[gameId]?.currentQuestionIndex ?: 0
            val finished = session?.isFinished ?: true

            call.respondText("{\"index\":$index,\"finished\":$finished}", ContentType.Application.Json)
        }

        //polling endpoint - returns minigame state
        get("/minigame-active/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("{\"active\":false}", ContentType.Application.Json)
            val session = activeSessions[gameId]
            val active = when {
                session?.mathsGameState != null -> session.mathsGameState!!.isFinished == false
                session?.potionsGameState != null -> session.potionsGameState!!.isFinished == false
                session?.roomsGameState != null -> session.roomsGameState!!.isFinished == false
                session?.wordsGameState != null -> session.wordsGameState!!.isFinished == false
                else -> false
            }
            call.respondText("{\"active\":$active}", ContentType.Application.Json)
        }

        //polling endpoint - redirects when a minigame is active
        get("/minigame-redirect/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("{\"redirect\":null}", ContentType.Application.Json)
            val session = activeSessions[gameId]

            val redirect = when {
                session?.mathsGameState?.isStarted == true -> "/minigame-question/$gameId"
                session?.potionsGameState?.isStarted == true -> "/potions-start/$gameId"
                session?.roomsGameState?.isStarted == true -> "/rooms-start/$gameId"
                session?.wordsGameState?.isStarted == true -> "/words-start/$gameId"
                else -> null
            }

            call.respondText("{\"redirect\":${if (redirect != null) "\"$redirect\"" else "null"}}", ContentType.Application.Json)
        }
    }
}