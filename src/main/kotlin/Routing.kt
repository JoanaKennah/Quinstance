package com.example

import com.example.database.tables.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import kotlinx.serialization.json.Json
import io.ktor.websocket.Frame
import kotlinx.coroutines.delay

import com.password4j.Password
import com.example.database.objects.*


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(PebbleContent("homepage.peb", mapOf<String, Any>()))
        }

        get("/home") {
            call.respond(PebbleContent("homepage.peb", mapOf<String, Any>()))
        }

        get("/login") {
            call.respond(PebbleContent("login.peb", mapOf<String, Any>()))
        }

        post("/login") { // I think this will only work for the real database, so we can't test it just yet
            val parameters = call.receiveParameters()
            val username = parameters["username"]
            val password = parameters["password"]

            if (username == null || password == null) {
                call.respondText("Missing input(s)")
                return@post
            }

            val user = retrieveUser(username)

            if (user == null) {
                call.respondText("Unexisting user")
                return@post
            }

            val checkPassword = Password.check(password, user.password).withScrypt()

            if (checkPassword) {
                call.sessions.set(UserSession(username = user.username, role = user.role.lowercase()))
                when (user.role.lowercase()) {
                    "student" -> {
                        call.respondRedirect("/student-dashboard")
                    }
                    "teacher", "admin" -> {
                        call.respondRedirect("/teacher-dashboard")
                    }
                }
            } else {
                call.respondText("Incorrect password")
            }
        }

        get("/signup") {
            call.respond(PebbleContent("signup.peb", mapOf<String, Any>()))
        }

        post("/signup") {
            val parameters = call.receiveParameters()
            val firstname = parameters["firstname"]?: return@post call.respondText("Missing input")
            val lastname = parameters["lastname"]?: return@post call.respondText("Missing input")
            val email = parameters["email"]?: return@post call.respondText("Missing input")
            val username = parameters["username"]?: return@post call.respondText("Missing input")
            val password = parameters["password"]?: return@post call.respondText("Missing input")
            val confirmpassword = parameters["confirmpassword"]?: return@post call.respondText("Missing input")
            val role = parameters["role"]?: return@post call.respondText("Missing input")
            val keystage = parameters["keystage"]?: return@post call.respondText("Missing input")

            if (password != confirmpassword) {
                call.respondText("Passwords don't match")
                return@post
            }

            val name = "$firstname $lastname"
            val newUserRole = when (val newUser = addUser(username, password, name, email, keystage, role)) {
                null -> "USERNAME_EXISTS"
                else ->  newUser.role.lowercase()
            }

            when (newUserRole) {
                "USERNAME_EXISTS" -> {
                    call.respondText("Username already exists")
                }
                "student" -> {
                    call.sessions.set(UserSession(username = username, role = role))
                    call.respondRedirect("/student-dashboard")
                }
                "teacher", "admin" -> {
                    call.sessions.set(UserSession(username = username, role = role))
                    call.respondRedirect("/teacher-dashboard")
                }
                else -> {
                    call.respondText("Failed to create new user")
                }
            }
        }

        get("/teacher-dashboard") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")

            call.respond(PebbleContent("teacher_dashboard.peb", mapOf("username" to session.username)))
        }

        get("/student-dashboard") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            
            call.respond(PebbleContent("student_dashboard.peb", mapOf("username" to session.username)))
        }


        //gets gameId and other details to pass to template
        get("/game/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@get call.respondText("Game Not Found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respondText("Quiz Not Found")

            call.respond(PebbleContent("playing_question_client.peb", mapOf(
                "username" to session.username,
                "quizName" to quiz.quiz_name,
                "gameId" to gameId
            )))
        }

        get("/create-quiz") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")

            call.respond(PebbleContent("create_quiz.peb", mapOf<String, Any>()))
        }

        post("/create-quiz") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")

            val parameters = call.receiveParameters()
            val quizName = parameters["quizName"] ?: return@post call.respondText("Please enter a quiz name")
            val description = parameters["description"] ?: return@post call.respondText("Please enter a description")
            val keystage = parameters["keystage"] ?: return@post call.respondText("Please enter a key stage")
            val category = parameters["category"] ?: return@post call.respondText("Please enter a category")
            
            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            val quiz = addQuiz(user, quizName, description, category, keystage)

            call.respondRedirect("/view-quiz-questions/${quiz.id.value}")
        }
        get("/view-quiz-questions/{quizId}") {
            val quizId = call.parameters["quizId"]?.toIntOrNull()
                ?: return@get call.respondText("Quiz ID Invalid")

            val quiz = retrieveQuiz(quizId) ?: return@get call.respondText("Quiz not found")
            val questions = retrieveQuizQuestions(quiz)

            call.respond(PebbleContent("view_quiz_questions.peb", mapOf("quizName" to quiz.quiz_name, "questions" to questions, "quizId" to quizId)))
        }

        get("/add-question/{quizId}") {
            val quizId = call.parameters["quizId"]?.toIntOrNull()
                ?: return@get call.respondText("Invalid Quiz ID")
            
            val quiz = retrieveQuiz(quizId) ?: return@get call.respondText("No Quiz Found")

            call.respond(PebbleContent("add_question.peb", mapOf("quizName" to quiz.quiz_name, "quizId" to quizId)))
        }

        // Reading question, all the answers, checking correct answer, saving to DB
        post("/add-question/{quizId}") {
            val quizId = call.parameters["quizId"]?.toIntOrNull()
                ?: return@post call.respondText("Invalid quiz ID")

            val quiz = retrieveQuiz(quizId) ?: return@post call.respondText("No Quiz Found")

            val parameters = call.receiveParameters()
            val questionText = parameters["question"] 
                ?: return@post call.respondText("Please enter a question")

            val answers = parameters.getAll("answers[]") 
                ?: return@post call.respondText("Please enter at least 2 answers")

            // Stores index of correct answer (checkbox)
            val correctIndex = parameters["correct"]?.toIntOrNull()
                ?: return@post call.respondText("Please select a correct answer")

            val option1 = answers.getOrNull(0) 
                ?: return@post call.respondText("Please enter at least 2 answers")
            val option2 = answers.getOrNull(1) 
                ?: return@post call.respondText("Please enter at least 2 answers")
            val option3 = answers.getOrNull(2) 
            val option4 = answers.getOrNull(3)

            // Correct index here indicates correct answer for DB
            val question = addQuestion(quiz, questionText, quiz.category)
            addAnswer(question, option1, option2, option3, option4, correctIndex)

            call.respondRedirect("/view-quiz-questions/$quizId")
        }

        get ("/view-quizzes") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")

            val returnURL = if (session.role == "teacher" || session.role == "admin") {
                "/teacher-dashboard" 
            } else {
                "/student-dashboard"
            } 

            val quizzes = retrieveUserQuizzes(session.username) ?: emptyList<Quiz>()

            call.respond(PebbleContent("view_quizzes.peb", mapOf("username" to session.username, "returnURL" to returnURL, "quizzes" to quizzes)))
        }

        post ("/quiz-created/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")

            val quizId = call.parameters["quizId"]?.toIntOrNull()
                ?: return@post call.respondText("Quiz ID Invalid")

            val returnURL = if (session.role == "teacher" || session.role == "admin") {
                "/teacher-dashboard" 
            } else {
                "/student-dashboard"
            } 

            call.respond(PebbleContent("quiz_created.peb", mapOf("quizId" to quizId, "returnURL" to returnURL)))
        }

        get("/view-quiz/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val quizId = call.parameters["quizId"]?.toIntOrNull() ?: return@get call.respondText("Invalid quiz ID")
            val quiz = retrieveQuiz(quizId) ?: return@get call.respondText("Quiz not found")

            val categoryDisplay = when (quiz.category) {
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

            val keyStageDisplay = when (quiz.key_stage) {
                "KS1" -> "Key Stage 1"
                "KS2" -> "Key Stage 2"
                "KS3" -> "Key Stage 3"
                "KS4" -> "Key Stage 4"
                "KS5" -> "Key Stage 5"
                "furthereducation" -> "Further Education"
                "nonapplicable" -> "N/A"
                else -> quiz.key_stage
            }

            call.respond(PebbleContent("view_quizzes_selectd_quiz.peb", mapOf(
                "username" to session.username,
                "quiz" to quiz,
                "quizId" to quizId,
                "categoryDisplay" to categoryDisplay,
                "keyStageDisplay" to keyStageDisplay
            )))

        }

        get("/create-lobby/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val quizId = call.parameters["quizId"]?.toIntOrNull() ?: return@get call.respondText("Invalid quiz ID")
            val quiz = retrieveQuiz(quizId) ?: return@get call.respondText("Quiz not found")

            val categoryDisplay = when (quiz.category) {
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

            val keyStageDisplay = when (quiz.key_stage) {
                "KS1" -> "Key Stage 1"
                "KS2" -> "Key Stage 2"
                "KS3" -> "Key Stage 3"
                "KS4" -> "Key Stage 4"
                "KS5" -> "Key Stage 5"
                "furthereducation" -> "Further Education"
                "nonapplicable" -> "N/A"
                else -> quiz.key_stage
            }

            call.respond(PebbleContent("create_lobby.peb", mapOf(
                "quizId" to quizId, 
                "quizName" to quiz.quiz_name, 
                "numQuestions" to quiz.num_of_questions, 
                "category" to categoryDisplay, 
                "keyStage" to keyStageDisplay
                )))

        }
        
        post("/create-lobby/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val quizId = call.parameters["quizId"]?.toIntOrNull() ?: return@post call.respondText("Invalid quiz ID")
            val quiz = retrieveQuiz(quizId) ?: return@post call.respondText("Quiz not found")

            val parameters = call.receiveParameters()
            val timeLimit = parameters["timeLimit"]?.toIntOrNull() ?: 30

            val game = addGame(quiz)
            val questions = retrieveQuizQuestions(quiz)
            val gameSession = GameSession(gameId = game.id.value, questions = questions)
            activeSessions[game.id.value] = gameSession

            val roomCode = generateRoomCode()
            addInUseCode(roomCode, game)

            call.respondRedirect("/lobby-server/${game.id.value}")
        }

        get("/lobby-server/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")

            val game = retrieveGame(gameId) ?: return@get call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respondText("Quiz not found")

            val roomCode = retrieveRoomCode(game) ?: return@get call.respondText("Room code not found")

            val players = retrieveGamePlayers(game)

            call.respond(PebbleContent("lobby_server_view.peb", mapOf("quizName" to quiz.quiz_name, "roomCode" to roomCode, "players" to players, "gameId" to gameId)))
        }

        get("/join-lobby") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val returnURL = if (session.role == "teacher" || session.role == "admin") {
                "/teacher-dashboard"
            } else {
                "/student-dashboard"
            }

            call.respond(PebbleContent("join_lobby.peb", mapOf("returnURL" to returnURL, "roomCode" to "", "errorMessage" to "")))

        }

        post("/join-lobby") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val parameters = call.receiveParameters()
            val roomCode = parameters["roomCode"]?.uppercase() ?: return@post call.respondText("Please enter a room code")

            val returnURL = if (session.role == "teacher" || session.role == "admin") {
                "/teacher-dashboard"
            } else {
                "/student-dashboard"
            }

            val inUseCode = retrieveInUseCode(roomCode)

            if (inUseCode == null) {
                call.respond(PebbleContent("join_lobby.peb", mapOf("returnURL" to returnURL, "roomCode" to roomCode, "errorMessage" to "Invalid room code, please try again")))
                return@post
            }
            val game = retrieveInUseCodeGame(inUseCode) ?: return@post call.respondText("Game not found")
            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            addUserScore(game, user)
            call.respondRedirect("/lobby-client/${game.id.value}")
        }
        
        get("/lobby-client/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")

            val game = retrieveGame(gameId) ?: return@get call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respondText("Quiz not found")

            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")                          //verify user is actually in game

            val userScore = retrieveUserScore(game, user) ?: return@get call.respondText("You are not a player in this game")

            val roomCode = retrieveRoomCode(game) ?: return@get call.respondText("Room code not found")

            call.respond(PebbleContent("lobby_client_view.peb", mapOf("quizName" to quiz.quiz_name, "roomCode" to roomCode, "gameId" to gameId)))
        }

        post("/leave-lobby/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login") 
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@post call.respondText("Game not found")
            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            // This will change in the future so have left this for now :3
            transaction(TransactionManager.defaultDatabase) {
                UserScore.find {
                    UserScoreTable.game eq game.id and (UserScoreTable.user eq user.id) 
                }.firstOrNull()?.delete()
            }

            val returnURL = if (session.role == "teacher" || session.role == "admin") {                                         //delete UserScore to remove player from game
                "/teacher-dashboard"
            } else {
                "/student-dashboard"
            }
            call.respondRedirect(returnURL)
        }
        
        post("/close-lobby/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respondText("Invalid game ID") 
            val game = retrieveGame(gameId) ?: return@post call.respondText("Game not found")

            deleteInUseCode(game)

            // This will change in the future so have left this for now :3
            transaction(TransactionManager.defaultDatabase) {                                                                   //delete all user scores from this game
                UserScore.find { UserScoreTable.game eq game.id }.forEach { it.delete() }
            }

            transaction(TransactionManager.defaultDatabase) {                                                                   //delete game last
                game.delete()
            }

            call.respondRedirect("/teacher-dashboard")
        }

        post("/start-game/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login") 
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respondText("Invalid game ID") 
            val game = retrieveGame(gameId) ?: return@post call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@post call.respondText("Quiz not found")

            val questions = retrieveQuizQuestions(quiz)                                        //load questions into quiz
            if (questions.isEmpty()) {
                return@post call.respondText("No Questions Found in Quiz")
            } 

            val gameSession = activeSessions[gameId]
                ?: return@post call.respondText("No active session found")

            gameSession.isStarted = true
            
            //redirect all connected players to game page
            gameSession.connections.forEach { connection -> connection.send(Frame.Text(Json.encodeToString(mapOf("redirect" to "/game/$gameId")))) }

            // Small delay allows clients to connect to new WebSocket
            kotlinx.coroutines.delay(1000)

            //Broadcast first question to connected players
            broadcastQuestion(gameSession)

            call.respond(PebbleContent("playing_question_server.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "questionNumber" to 1,
                "questionText" to questions[0].question,
                "gameId" to gameId
            )))
        }

        post("/next-question/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")

            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respondText("Invalid game ID") 
            val game = retrieveGame(gameId) ?: return@post call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@post call.respondText("Quiz not found")

            val gameSession = activeSessions[gameId] ?: return@post call.respondText("No active session found for this game")

            if (gameSession.isAdvanced) {
                return@post call.respondText("Already advancing")
            }
            gameSession.isAdvanced = true

            gameSession.currentQuestionIndex++          //advance quiz to next Q

            if (gameSession.currentQuestionIndex >= gameSession.questions.size) {
                gameSession.isFinished = true
                activeSessions.remove(gameId)           //cut game and go to leaderboard
                call.respondRedirect("/leaderboard/$gameId") 

            } else {
                gameSession.isAdvanced = false          //reset flag for next Q
                delay(1000)
                broadcastQuestion(gameSession)

                val nextQuestion = gameSession.questions[gameSession.currentQuestionIndex] 
                call.respond(PebbleContent("playing_question_server.peb", mapOf(
                    "quizName" to quiz.quiz_name,
                    "questionNumber" to gameSession.currentQuestionIndex + 1,
                    "questionText" to nextQuestion.question,
                    "gameId" to gameId
                )))
            }

        }

        // Serves final leaderboard at end of game
        get("/leaderboard/{gameId}") {

            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@get call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respondText("Quiz not found")

            // Retrieve player scores, sort by highest
            val players = retrieveDescendingPlayerScores(game)

            call.respond(PebbleContent("end_leaderboard.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "players" to players,
                "gameId" to gameId,
                "quizId" to quiz.id
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

        //TESTED UP TO HERE

        // Serves selected answer template and params
        get("/selected-answer/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login") 
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@get call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respondText("Quiz not found")

            val selectedAnswer = call.request.queryParameters["answer"] ?: ""

            call.respond(PebbleContent("client_selected_ans.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "gameId" to gameId, 
                "selectedAnswer" to selectedAnswer
            )))
        }

        post("/show-correct-answer/{gameId}") {
            val session = call.sessions.get<UserSession>()?: return@post call.respondRedirect("/login") 
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@post call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@post call.respondText("Quiz not found")

            val gameSession = activeSessions[gameId] ?: return@post call.respondText("No active session found")

            // Get current Q and correct ans
            val currentQuestion = gameSession.questions[gameSession.currentQuestionIndex]
            val correctAnswer = retrieveQuestionAnswer(currentQuestion)?.correct_answer ?: return@post call.respondText("No answer found for question")

            val answers = UserAnswers.allUserAnswers()
            for (answerData in answers) {
                if (answerData.chosenAnswer == correctAnswer) {
                    val user = retrieveUser(answerData.username)
                    if (user != null) {
                        val userScore = retrieveUserScore(game, user)
                        if (userScore != null) userScore.score += 1
                    }
                }
            }

            // Clear answers ready for next question
            UserAnswers.clearAnswers()

            // Send clients to correct ans page
            gameSession.connections.forEach { connection -> connection.send(Frame.Text(Json.encodeToString(mapOf("redirect" to "/show-correct-answer-client/$gameId"))))}

            //send host to correct ans view w/ next button
            call.respond(PebbleContent("question_with_correct_ans.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "questionNumber" to gameSession.currentQuestionIndex + 1,
                "questionText" to currentQuestion.question,
                "correctAnswer" to correctAnswer,
                "gameId" to gameId,
                "isHost" to true
            )))
        }

        get("/show-correct-answer-client/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@get call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respondText("Quiz not found")

            val gameSession = activeSessions[gameId] ?: return@get call.respondText("No active session found")

            val currentQuestion = gameSession.questions[gameSession.currentQuestionIndex]
            val correctAnswer = retrieveQuestionAnswer(currentQuestion)?.correct_answer ?: return@get call.respondText("No answer found for question")

            call.respond(PebbleContent("question_with_correct_ans.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "questionNumber" to gameSession.currentQuestionIndex + 1,
                "questionText" to currentQuestion.question,
                "correctAnswer" to correctAnswer,
                "gameId" to gameId,
                "isHost" to false
            )))
        }

        get("/show-correct-answer/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@get call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respondText("Quiz not found")

            val gameSession = activeSessions[gameId] ?: return@get call.respondText("No active session found")

            val currentQuestion = gameSession.questions[gameSession.currentQuestionIndex]
            val correctAnswer = retrieveQuestionAnswer(currentQuestion)?.correct_answer ?: return@get call.respondText("No answer found for question")

            call.respond(PebbleContent("question_with_correct_ans.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "questionNumber" to gameSession.currentQuestionIndex + 1,
                "questionText" to currentQuestion.question,
                "correctAnswer" to correctAnswer,
                "gameId" to gameId,
                "isHost" to true
            )))
        }

        // Serves mid-game leaderboard between questions
        post("/show-positions/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@post call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@post call.respondText("Quiz not found")

            val gameSession = activeSessions[gameId] ?: return@post call.respondText("No active session found")

            val scores = transaction(TransactionManager.defaultDatabase) {
                UserScore.find { UserScoreTable.game eq game.id }
                    .sortedByDescending { it.score }
                    .map { userScore -> 
                        mapOf<String, Any>("username" to userScore.user.username, "score" to userScore.score) 
                    }
            }

            //send each client to position page
            gameSession.connections.forEach { connection -> 
                connection.send(Frame.Text(Json.encodeToString(
                    mapOf("redirect" to "/client-position/$gameId")
                )))
            }

            call.respond(PebbleContent("leaderboard.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "players" to scores,
                "gameId" to gameId,
                "quizId" to quiz.id
            )))
        }

        // Serves personal position and score to each player, personalised
        get("/client-position/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@get call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respondText("Quiz not found")

            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            val allScores = retrieveAllUserScores(game)

            // Find score and position per player
            val playerScore = allScores.indexOfFirst { it.first == user.id.value }
            val position = playerScore + 1 
            val score = allScores.getOrNull(playerScore)?.third ?: 0
            val totalPlayers = allScores.size
            val questionIndex: Int = activeSessions[gameId]?.currentQuestionIndex ?: 0

            call.respond(PebbleContent("client_current_pos.peb", mapOf<String, Any>(
                "quizName" to quiz.quiz_name,
                "gameId" to gameId,
                "position" to position,
                "score" to score,
                "totalPlayers" to totalPlayers,
                "questionIndex" to questionIndex
            )))
        }

        get("/game-started/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull()
                ?: return@get call.respondText("{\"started\":false}", ContentType.Application.Json)
            val started = activeSessions[gameId]?.isStarted ?: false
            call.respondText("{\"started\":$started}", ContentType.Application.Json)
        }

        get("/lobby-players/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("[]", ContentType.Application.Json) 
            val game = retrieveGame(gameId) ?: return@get call.respondText("[]", ContentType.Application.Json)

            val players = retrieveGamePlayers(game)

            val json = "[" + players.joinToString(",") { "\"$it\"" } + "]" 
            call.respondText(json, ContentType.Application.Json)
        }

        get("/question-index/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("{\"index\":0}", ContentType.Application.Json)
            val session = activeSessions[gameId]
            val index = activeSessions[gameId]?.currentQuestionIndex ?: 0
            val finished = session?.isFinished ?: true
            println("Question index polled: gameId=$gameId index=$index finished=$finished")
            call.respondText("{\"index\":$index,\"finished\":$finished}", ContentType.Application.Json)
        }


        // Serves client view at end of game
        get("/end-game/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")

            val game = retrieveGame(gameId) ?: return@get call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respondText("Quiz not found")

            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login") 

            val allScores = retrieveAllUserScores(game)

            val playerScore = allScores.indexOfFirst { it.first == user.id.value }
            val position = playerScore + 1
            val score = allScores.getOrNull(playerScore)?.third ?: 0
            val totalPlayers = allScores.size

            call.respond(PebbleContent("end_game_client.peb", mapOf<String, Any>(
                "quizName" to quiz.quiz_name,
                "gameId" to gameId,
                "position" to position,
                "score" to score,
                "totalPlayers" to totalPlayers
            )))
        }
    }
}