package com.example

import com.example.database.ProjectDatabase
import com.example.UserAnswers
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
import io.pebbletemplates.pebble.loader.ClasspathLoader
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.core.eq

import com.password4j.Password
import com.example.database.objects.*
import com.example.activeSessions
import com.example.GameSession
import com.example.broadcastQuestion



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
            val newUserRole = transaction(ProjectDatabase.database) {
                val newUser = addUser(username, password, name, email, keystage, role)
                if (newUser == null) {
                    "USERNAME_EXISTS"
                } else {
                    newUser?.role?.lowercase()
                }
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


        get("/game") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val user = retrieveUser(session.username)
            val quizName = "test" // PLACEHOLDER FOR WHEN WE HAVE LOBBY :3
            if (user != null) call.respond(PebbleContent("playing_question_client.peb", mapOf("username" to session.username, "quizName" to quizName)))
            else call.respondText("User with username ${session.username} doesn't exist")
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

            val quiz = transaction(ProjectDatabase.database) {
                Quiz.findById(quizId)
            } ?: return@get call.respondText("Quiz not found") 

            val questions = retrieveQuizQuestions(quiz)

            call.respond(PebbleContent("view_quiz_questions.peb", mapOf("quizName" to quiz.quiz_name, "questions" to questions, "quizId" to quizId)))
        }

        get("/add-question/{quizId}") {
            val quizId = call.parameters["quizId"]?.toIntOrNull()
                ?: return@get call.respondText("Invalid Quiz ID")
            
            val quiz = transaction(ProjectDatabase.database) {
                Quiz.findById(quizId)
            } ?: return@get call.respondText("No Quiz Found") 

            call.respond(PebbleContent("add_question.peb", mapOf("quizName" to quiz.quiz_name, "quizId" to quizId)))
        }

        //Reading question, all the answers, checking correct answer, saving to DB
        post("/add-question/{quizId}") {
            val quizId = call.parameters["quizId"]?.toIntOrNull()
                ?: return@post call.respondText("Invalid quiz ID")

            val quiz = transaction(ProjectDatabase.database) {
                Quiz.findById(quizId)
            } ?: return@post call.respondText("No Quiz Found")

            val parameters = call.receiveParameters()
            val questionText = parameters["question"] 
                ?: return@post call.respondText("Please enter a question")

            val answers = parameters.getAll("answers[]") 
                ?: return@post call.respondText("Please enter at least 2 answers")

            //stores index of correct answer (checkbox)
            val correctIndex = parameters["correct"]?.toIntOrNull()
                ?: return@post call.respondText("Please select a correct answer")

            val option1 = answers.getOrNull(0) 
                ?: return@post call.respondText("Please enter at least 2 answers")
            val option2 = answers.getOrNull(1) 
                ?: return@post call.respondText("Please enter at least 2 answers")
            val option3 = answers.getOrNull(2) 
            val option4 = answers.getOrNull(3)

            transaction(ProjectDatabase.database) {
                val question = addQuestion(quiz, questionText, quiz.category)
                addAnswer(question, option1, option2, option3, option4, correctIndex)
                //correct index here indicates correct answer for DB
            } 

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
            val quiz = transaction(TransactionManager.defaultDatabase) {
                Quiz.findById(quizId)
            } ?: return@get call.respondText("Quiz not found") 

            call.respond(PebbleContent("view_quizzes_selectd_quiz.peb", mapOf( "username" to session.username, "quiz" to quiz, "quizId" to quizId)))

        }

        get("/create-lobby/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val quizId = call.parameters["quizId"]?.toIntOrNull() ?: return@get call.respondText("Invalid quiz ID")
            val quiz = transaction(TransactionManager.defaultDatabase) {
                Quiz.findById(quizId)
            } ?: return@get call.respondText("Quiz not found")

            call.respond(PebbleContent("create_lobby.peb", mapOf("quizId" to quizId, "quizName" to quiz.quiz_name, "numQuestions" to quiz.num_of_questions, "category" to quiz.category, "keyStage" to quiz.key_stage)))

        }
        
        post("/create-lobby/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val quizId = call.parameters["quizId"]?.toIntOrNull() ?: return@post call.respondText("Invalid quiz ID") 
            val quiz = transaction(TransactionManager.defaultDatabase) {
                Quiz.findById(quizId) 
            } ?: return@post call.respondText("Quiz not found") 

            val parameters = call.receiveParameters()
            val timeLimit = parameters["timeLimit"]?.toIntOrNull() ?: 30

            val game = transaction(TransactionManager.defaultDatabase) {
                Game.new {
                    this.quiz = quiz
                }
            }

            val roomCode = generateRoomCode()

            transaction(TransactionManager.defaultDatabase) {
                InUseCode.new {
                    this.room_code = roomCode
                    this.game = game
                }
            }

            call.respondRedirect("/lobby-server/${game.id.value}")
        }

        get("/lobby-server/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")
            val game = transaction(TransactionManager.defaultDatabase) {
                Game.findById(gameId)
            } ?: return@get call.respondText("Game not found") 

            val roomCode = transaction(TransactionManager.defaultDatabase) {
                InUseCode.find { InUseTable.game eq game.id }.firstOrNull()?.room_code
            } ?: return@get call.respondText("Room code not found") 

            val players = transaction(TransactionManager.defaultDatabase) {
                UserScore.find { UserScoreTable.game eq game.id }.map { it.user.username }
            }

            call.respond(PebbleContent("lobby_server_view.peb", mapOf("quizName" to game.quiz.quiz_name, "roomCode" to roomCode, "players" to players, "gameId" to gameId)))
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

            val inUseCode = transaction(TransactionManager.defaultDatabase) {
                InUseCode.find { InUseTable.room_code eq roomCode }.firstOrNull()
            }

            if (inUseCode == null) {
                call.respond(PebbleContent("join_lobby.peb", mapOf("returnURL" to returnURL, "roomCode" to roomCode, "errorMessage" to "Invalid room code, please try again")))
                return@post
            }
            val game = inUseCode.game
            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            transaction(TransactionManager.defaultDatabase) {
                UserScore.new {
                    this.game = game
                    this.user = user
                    this.score = 0
                }
            }
            call.respondRedirect("/lobby-client/${game.id.value}")
        }
        
        get("/lobby-client/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID") 
            val game = transaction(TransactionManager.defaultDatabase) {
                Game.findById(gameId)
            } ?: return@get call.respondText("Game not found") 

            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")                          //verify user is actually in game

            val userScore = transaction(TransactionManager.defaultDatabase) {
                UserScore.find {
                    UserScoreTable.game eq game.id and (UserScoreTable.user eq user.id)
                }.firstOrNull()
            } ?: return@get call.respondText("You are not a player in this game")

            val roomCode = transaction(TransactionManager.defaultDatabase) {                                                // display room code
                InUseCode.find { InUseTable.game eq game.id }.firstOrNull()?.room_code
            } ?: return@get call.respondText("Room code not found") 

            call.respond(PebbleContent("lobby_client_view.peb", mapOf("quizName" to game.quiz.quiz_name, "roomCode" to roomCode, "gameId" to gameId)))

        }

        post("/leave-lobby/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login") 
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respondText("Invalid game ID")
            val game = transaction(TransactionManager.defaultDatabase) {
                Game.findById(gameId)
            } ?: return@post call.respondText("Game not found") 
            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

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
            val game = transaction(TransactionManager.defaultDatabase) {
                Game.findById(gameId)
            } ?: return@post call.respondText("Game not found") 

            transaction(TransactionManager.defaultDatabase) {                                                                   //delete room code
                InUseCode.find { InUseTable.game eq game.id }.firstOrNull()?.delete() 
            }

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
            val game = transaction(TransactionManager.defaultDatabase) {
                Game.findById(gameId)
            } ?: return@post call.respondText("Game not found") 

            val questions = retrieveQuizQuestions(game.quiz)                                        //load questions into quiz
            if (questions.isEmpty()) {
                return@post call.respondText("No Questions Found in Quiz")
            } 

            //create GameSession, post to activeSessions
            val gameSession = GameSession(gameId = gameId, questions = questions)
            activeSessions[gameId] = gameSession

            //Broadcasr first question to connected players
            broadcastQuestion(gameSession)

            call.respond(PebbleContent("playing_question_server.peb", mapOf(
                "quizName" to game.quiz.quiz_name,
                "questionNumber" to 1,
                "questionText" to questions[0].question,
                "gameId" to gameId
            )))
        }

        post("/next-question/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respondText("Invalid game ID") 
            val game = transaction(TransactionManager.defaultDatabase) {
                Game.findById(gameId)
            } ?: return@post call.respondText("Game not found") 

            val gameSession = activeSessions[gameId] ?: return@post call.respondText("No active session found for this game")

            //retrieve just-answered question
            val currentQuestion = gameSession.questions[gameSession.currentQuestionIndex]
            val correctAnswer = retrieveQuestionAnswer(currentQuestion)?.correct_answer

            //update scores for correct answers
            if (correctAnswer != null) {
                val answers = UserAnswers.allUserAnswers() 
                transaction(TransactionManager.defaultDatabase) {
                    answers.forEach { answerData -> 
                        if (answerData.chosenAnswer == correctAnswer) {
                            val user = retrieveUser(answerData.username)
                            if (user != null) {
                                UserScore.find {
                                    UserScoreTable.game eq game.id and (UserScoreTable.user eq user.id) 
                                }.firstOrNull()?.let { userScore -> userScore.score += 1}
                            }
                        }
                    }
                }
            }

            UserAnswers.clearAnswers()                  //clear answers for next Q

            gameSession.currentQuestionIndex++          //advance quiz to next Q

            if (gameSession.currentQuestionIndex >= gameSession.questions.size) {
                activeSessions.remove(gameId)           //cut game and go to leaderboard
                call.respondRedirect("/leaderboard/$gameId") 

            } else {
                broadcastQuestion(gameSession)

                val nextQuestion = gameSession.questions[gameSession.currentQuestionIndex] 
                call.respond(PebbleContent("playing_question_server.peb", mapOf(
                    "quizName" to game.quiz.quiz_name,
                    "questionNumber" to gameSession.currentQuestionIndex + 1,
                    "questionText" to nextQuestion.question,
                    "gameId" to gameId
                )))
            }

        }

        get("/leaderboard/{gameId}") {

            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID") 
            val game = transaction(TransactionManager.defaultDatabase) {
                Game.findById(gameId)
            } ?: return@get call.respondText("Game not found") 

            //retrieve player scores, sort by highest 
            val players = transaction(TransactionManager.defaultDatabase) {
                UserScore.find { UserScoreTable.game eq game.id }.sortedByDescending { it.score }.map { userScore -> mapOf("username" to userScore.user.username, "score" to userScore.score)}

            } 

            call.respond(PebbleContent("leaderboard.peb", mapOf("quizName" to game.quiz.quiz_name, "players" to players)))

        }

        //quit button back to dashboard from leaderboards
        post("/leaderboard-quit/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val returnURL = if (session.role == "teacher" || session.role == "admin") {
                "/teacher-dashboard" 
            } else {
                "/student-dashboard"
            }

            call.respondRedirect(returnURL)
            
        }

    }
}