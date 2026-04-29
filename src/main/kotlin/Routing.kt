package com.example

import com.example.database.tables.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import kotlinx.serialization.json.Json
import io.ktor.websocket.Frame
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import com.password4j.Password
import com.example.database.objects.*
import com.example.sendConfirmationEmail
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File

//create an HTTP client to reuse every request
//used to make outgoing request to API using coroutines
private val httpClient = HttpClient(CIO)

fun Application.configureRouting() {
    routing {

        //serves homepage - default
        get("/") {
            call.respond(PebbleContent("homepage.peb", mapOf()))
        }

        //serves homepage - dedicated
        get("/home") {
            call.respond(PebbleContent("homepage.peb", mapOf()))
        }

        //serves login page
        get("/login") {
            call.respond(PebbleContent("login.peb", mapOf()))
        }


        //takes credentials & validates, sets session, redirects to dashboards
        post("/login") { // I think this will only work for the real database, so we can't test it just yet
            val parameters = call.receiveParameters()
            val username = parameters["username"]
            val password = parameters["password"]

            if (username == null || password == null) {
                call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
                return@post
            }

            val user = retrieveUser(username)

            if (user == null) {
                call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Unexisiting user")))
                return@post
            }

            val checkPassword = Password.check(password, user.password).withScrypt()

            if (checkPassword) {
                call.sessions.set(
                    UserSession(
                        username = user.username,
                        role = user.role.lowercase()
                    )  
                )      
                when (user.role.lowercase()) {
                    "student" -> {
                        call.respondRedirect("/student-dashboard")
                    }
                    "teacher", "admin" -> {
                        call.respondRedirect("/teacher-dashboard")
                    }
                    else -> {
                        call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Unknown role")))
                    }  
                }   
            }  else {
                call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Incorrect password")))
            }
        }

        //serves signup page 
        get("/signup") {
            call.respond(PebbleContent("signup.peb", mapOf()))
        }

        //validates signup form, creates user in database, redirects to dash
        post("/signup") {
            val parameters = call.receiveParameters()
            val firstname = parameters["firstname"]?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val lastname = parameters["lastname"]?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val email = parameters["email"]?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val username = parameters["username"]?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val password = parameters["password"]?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val confirmpassword = parameters["confirmpassword"]?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val role = parameters["role"]?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val keystage = parameters["keystage"]?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))

            if (password != confirmpassword) {
                call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Passwords do not match")))
                return@post
            }

            val name = "$firstname $lastname"
            val createdUser = addUser(username, password, name, email, keystage, role)
            val newUserRole = createdUser?.role?.lowercase() ?: "USERNAME_EXISTS"
            
            if (createdUser != null && newUserRole != "USERNAME_EXISTS") {
                sendConfirmationEmail(
                    toEmail = email,
                    firstName = firstname,
                    lastName = lastname,
                    username = username,
                    password = password
                )
            }

            when (newUserRole) {
                "USERNAME_EXISTS" -> {
                    call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Username already exists")))
                }
                "student" -> {
                    call.sessions.set(
                        UserSession(
                            username = createdUser!!.username,
                            role = createdUser.role.lowercase()
                        )
                    )
                    call.respondRedirect("/student-dashboard")    
                }
                "teacher", "admin" -> {
                    call.sessions.set(
                        UserSession(
                            username = createdUser!!.username,
                            role = createdUser.role.lowercase()
                        )
                    )
                    call.respondRedirect("/teacher-dashboard")    
                }
                else -> {
                    call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Failed to create new user")))
                }
            }
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/login")
        }

        //serves teacher dash
        get("/teacher-dashboard") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")

            if (session.role != "teacher" && session.role != "admin") {
                return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "You do not have permission to access the teacher dashboard")))
            }

            call.respond(
                PebbleContent(
                    "teacher_dashboard.peb",
                    mapOf("username" to session.username)
                )
            )    
        }

        //serves student dash
        get("/student-dashboard") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            
            if (session.role != "student") {
                return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "You do not have permission to access the student dashboard")))
            }

            call.respond(
                PebbleContent(
                    "student_dashboard.peb",
                    mapOf("username" to session.username)
                )    
            )
        }    

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

        //gets quiz creation page
        get("/create-quiz") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")

            val returnURL = when (session.role) {
                "teacher" -> "/teacher-dashboard"
                "student" -> "/student-dashboard"
                else -> return@get call.respondRedirect("/login")
            }

            call.respond(PebbleContent("create_quiz.peb", mapOf("returnURL" to returnURL)))
        }

        //saves created quiz to db, redirects to add Qs page
        post("/create-quiz") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")

            val parameters = call.receiveParameters()
            val quizName = parameters["quizName"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val description = parameters["description"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val keystage = parameters["keystage"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val category = parameters["category"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val minigamesEnabled = parameters["miniGames"] == "true"
            
            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            val quiz = addQuiz(user, quizName, description, category, keystage, minigamesEnabled = minigamesEnabled)

            call.respondRedirect("/view-quiz-questions/${quiz.id.value}")
        }

        //serves list of Qs in quiz
        get("/view-quiz-questions/{quizId}") {
            val quizId = call.parameters["quizId"]?.toIntOrNull()
                ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))

            val quiz = retrieveQuiz(quizId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))
            val questions = retrieveQuizQuestions(quiz)

            call.respond(PebbleContent("view_quiz_questions.peb",
                mapOf("quizName" to quiz.quiz_name,
                    "questions" to questions,
                    "quizId" to quizId,
                    "returnURL" to "/view-quizzes",
                    "error" to "none")))
        }

        post("/view-quiz-questions/{quidId}") {
            // References: https://tahaben.com.ly/2023/04/ktor-server-for-beginners-uploading-files/
            // https://ryanharrison.co.uk/2018/09/20/ktor-file-upload-download.html

            val multipart = call.receiveMultipart()
            var quizId: Int? = null
            var quiz: Quiz? = null
            var deleteQuestions: Boolean? = null
            var data: List<ReadQuestionData>? = null
            var csvReadError = "none"

            multipart.forEachPart { part ->
                when (part) {
                    // If the data retrieved is a file item, separate the data into a file name and the actual data.
                    is PartData.FileItem -> {
                        val fileData = part.provider().toInputStream()

                        // Returns a list of question data to be converted into a Question and Answer object.
                        data = readCsv(fileData)
                    }
                    // If the data is an item sent with the form (i.e. quizId and resetQuestions).
                    is PartData.FormItem -> {
                        when (part.name) {
                            "quizId" -> quizId = part.value.toInt()
                            "resetQuestions" -> deleteQuestions = part.value.toBoolean()
                        }
                    }
                    // Return an empty unit if nothing is sent.
                    else -> Unit
                }
                // Dispose of part so it can't be accessed by accident.
                part.dispose()
            }
            if (data != null) {
                // If readCsv returns an empty list of data.
                if (data!!.isEmpty()) csvReadError = "empty"
                if (quizId == null) return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))
                quiz = retrieveQuiz(quizId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

                // Delete all questions from the given quiz if the user imports a .csv file over it.
                if (deleteQuestions != null && deleteQuestions) {
                    val questions = retrieveQuizQuestions(quiz)
                    for (q in questions) {
                        deleteQuestion(q)
                    }
                }

                // If readCsv returns valid data (a list of ReadQuestionData) add Question & Answer to database
                for (question in data) {
                    // If option1 or option2 are empty, continue to next question.
                    if (question.option1 == "" || question.option2 == "") continue

                    // Check to see if there are any repeated answers (usually completed in add_question.peb file).
                    val checkAnswerDupes =
                        mutableSetOf(question.option1, question.option2, question.option3, question.option4)
                        .toList().filterNot { it == ""}
                    val listOfAns = mutableListOf(question.option1, question.option2, question.option3, question.option4)
                            .filterNot { it == ""}

                    if (checkAnswerDupes == listOfAns) {
                        val questionId = addQuestion(quiz, question.question, question.keyStage)

                        // Format option3 and option4 so they are accepted by the database.
                        val option3 = when (question.option3) {
                            "" -> null
                            else -> question.option3
                        }
                        val option4 = when (question.option4) {
                            "" -> null
                            else -> question.option4
                        }
                        // Add Answer to database.
                        addAnswer(
                            questionId,
                            question.option1,
                            question.option2,
                            option3,
                            option4,
                            question.correctOption.toInt()
                        )
                    } else csvReadError = "repeatAnswers"
                }
            } else csvReadError = "invalid"

            if (quiz != null) {
                // Retrieve quiz questions and check to see if they're still empty, results in empty error if true.
                val questions = retrieveQuizQuestions(quiz)
                if (questions.isEmpty()) csvReadError = "empty"

                call.respond(PebbleContent("view_quiz_questions.peb",
                    mapOf("quizName" to quiz.quiz_name,
                        "questions" to questions,
                        "quizId" to quiz.id,
                        "error" to csvReadError,
                        "returnURL" to "/view-quizzes")))
            } else return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))
        }
        post("/delete-question/{questionId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")

            val questionId = call.parameters["questionId"]?.toIntOrNull()
                ?: return@post call.respondText("Invalid question ID")

            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            val quizId = transaction(TransactionManager.defaultDatabase) {
                val question = Question.findById(questionId) ?: return@transaction null
                val quinstance = Quinstance.find { QuinstanceTable.question eq question.id }.firstOrNull()
                    ?: return@transaction null
                val quiz = quinstance.quiz

                if (quiz.user.id != user.id) {
                    return@transaction null
            }

            val deleted = deleteQuestion(question)
            if (deleted) quiz.id.value else null
        }

        if (quizId != null) {
            call.respondRedirect("/view-quiz-questions/$quizId")
        } else {
            call.respondText("Failed to delete question or permission denied")
        }
    }

        get("/questionTemplate") {
            // Reference: https://ryanharrison.co.uk/2018/09/20/ktor-file-upload-download.html
            val file = File("src/main/resources/questionTemplate.csv")
            if (file.exists()) {
                call.respondFile(file)
            } else call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "questionTemplate.csv does not exist")))
        }

        //serves add question to quiz form
        get("/add-question/{quizId}") {
            val quizId = call.parameters["quizId"]?.toIntOrNull()
                ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))
            
            val quiz = retrieveQuiz(quizId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            call.respond(PebbleContent("add_question.peb",
                mapOf<String, Any>(
                    "quizName" to quiz.quiz_name,
                    "quizId" to quizId,
                    "questionId" to "",
                    "isEdit" to false,
                    "previousQuestionText" to "",
                    "previousCorrectAnswer" to "",
                    "previousOption2" to "",
                    "previousAnswer3" to "",
                    "previousAnswer4" to "",
                )
            ))
        }

        get("/edit-question/{questionId}") {
            val questionId = call.parameters["questionId"]?.toIntOrNull()
                ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid questionID")))

            val quiz =
                transaction {
                    val quinstance = Quinstance.find { QuinstanceTable.question eq questionId }.firstOrNull()
                    if (quinstance != null) return@transaction quinstance.quiz
                    else return@transaction null
                } ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val question =
                transaction(TransactionManager.defaultDatabase) {
                    Question.find { QuestionTable.id eq questionId }.firstOrNull()
                } ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Question not found")))

            val answer = retrieveQuestionAnswer(question) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Answer not found")))
            val option3 = when(answer.option3) {
                null -> ""
                else -> answer.option3.toString()
            }

            val option4 = when(answer.option4) {
                null -> ""
                else -> answer.option4.toString()
            }
            call.respond(PebbleContent("add_question.peb",
                mapOf(
                    "quizName" to quiz.quiz_name,
                    "quizId" to quiz.id,
                    "questionId" to question.id,
                    "isEdit" to true,
                    "previousQuestionText" to question.question,
                    "previousCorrectAnswer" to answer.correct_answer,
                    "previousOption2" to answer.option2,
                    "previousOption3" to option3,
                    "previousOption4" to option4,
                ))
            )
        }

        // Reading question, all the answers, checking correct answer, saving to DB, redirects back to Q list
        // If isEditQuestion is true, instead of saving everything to the DB as new records, it edits previous records.
        post("/add-question/{quizId}") {
            val quizId = call.parameters["quizId"]?.toIntOrNull()
                ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))

            val quiz = retrieveQuiz(quizId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val parameters = call.receiveParameters()
            val questionText = parameters["question"] 
                ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))

            val answers = parameters.getAll("answers[]") 
                ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing answer input(s)")))

            // Stores index of correct answer (checkbox)
            val correctIndex = parameters["correct"]?.toIntOrNull()
                ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Select a correct answer")))

            val option1 = answers.getOrNull(0) 
                ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing answer input(s)")))
            val option2 = answers.getOrNull(1) 
                ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing answer input(s)")))
            val option3 = answers.getOrNull(2) 
            val option4 = answers.getOrNull(3)

            val isEditQuestion = parameters["isEdit"].toBoolean()

            if (!isEditQuestion) {
                // Correct index here indicates correct answer for Database
                val question = addQuestion(quiz, questionText, quiz.key_stage)
                addAnswer(question, option1, option2, option3, option4, correctIndex)
            } else {
                // Different to addAnswer so a new record isn't made.
                val questionId = parameters["questionId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid question ID")))

                val questionData = mapOf(
                    "question" to questionText,
                    "keyStage" to quiz.key_stage,
                )

                val mappedOption2 =
                    when(option3) {
                        null -> option2
                        else -> option3
                    }

                val mappedOption3 =
                    when(option3) {
                        null -> null
                        else -> option2
                    }

                val answerData = mapOf(
                    "option1" to option1,
                    "option2" to mappedOption2,
                    "option3" to mappedOption3,
                    "option4" to option4,
                    "correctIndex" to correctIndex.toString(),
                )

                val question = transaction(TransactionManager.defaultDatabase) { Question.findById(questionId) }
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Question doesn't exist")))

                val answer = retrieveQuestionAnswer(question)
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Answer doesn't exist")))

                editQuestion(question, questionData, answer, answerData)
            }
            call.respondRedirect("/view-quiz-questions/$quizId")
        }

        //serves list of quizzes created by user
        get ("/view-quizzes") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")

            val returnURL = if (session.role == "teacher" || session.role == "admin") {
                "/teacher-dashboard" 
            } else {
                "/student-dashboard"
            } 

            val quizzes = retrieveUserQuizzes(session.username) ?: emptyList()

            call.respond(PebbleContent("view_quizzes.peb", mapOf("username" to session.username, "returnURL" to returnURL, "quizzes" to quizzes)))
        }


        post("/delete-quiz/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")

            val quizId = call.parameters["quizId"]?.toIntOrNull()
                ?: return@post call.respondText("Invalid quiz ID")

            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            val quiz = retrieveQuiz(quizId) ?: return@post call
                .respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz doesn't exist")))

            val deleted = transaction(TransactionManager.defaultDatabase) {
                if (quiz.user.id != user.id) {
                    return@transaction false
                }

                return@transaction deleteQuiz(quiz)
            }

            if (deleted) {
                call.respondRedirect("/view-quizzes")
            } else {
                call.respondText("Failed to delete quiz or permission denied")
            }
        }


        //serves conformation page for created quiz
        post ("/quiz-created/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")

            val quizId = call.parameters["quizId"]?.toIntOrNull()
                ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))

            val returnURL = if (session.role == "teacher" || session.role == "admin") {
                "/teacher-dashboard" 
            } else {
                "/student-dashboard"
            } 

            call.respond(PebbleContent("quiz_created.peb", mapOf("quizId" to quizId, "returnURL" to returnURL)))
        }

        // serves specific quiz view, includes formatting for catagory and KStage
        get("/view-quiz/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val quizId = call.parameters["quizId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))
            val quiz = retrieveQuiz(quizId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

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

        //serves create lobby page, same formatting for category & Kstage
        get("/create-lobby/{quizId}") {
            call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val quizId = call.parameters["quizId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))
            val quiz = retrieveQuiz(quizId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

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
                "keyStage" to keyStageDisplay,
                "minigamesEnabled" to quiz.minigames_enabled
                )))

        }
        
        //create game in DB, gen room code, create GameSession, redirect to host lobby view
        post("/create-lobby/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val quizId = call.parameters["quizId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))
            val quiz = retrieveQuiz(quizId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val parameters = call.receiveParameters()
            val timeLimit = parameters["timeLimit"]?.toIntOrNull() ?: 30

            //read and update theme from checkbox
            val theme = if (parameters["theme"] == "spooky") "spooky" else "default"
            val miniGamesEnabled = parameters["minigames"] == "true"
            val activeMinigames = parameters.getAll("activeMinigames") ?: emptyList()

            call.sessions.set(session.copy(theme = theme))

            val game = addGame(quiz)
            val questions = retrieveQuizQuestions(quiz)
            val gameSession = GameSession(
                gameId = game.id.value, 
                hostUsername = session.username,
                questions = questions,
                timeLimit = timeLimit,
                miniGamesEnabled = miniGamesEnabled,
                activeMinigames = activeMinigames,
                theme = theme
            )
            activeSessions[game.id.value] = gameSession

            var roomCode: String?
            do {
                roomCode = generateRoomCode()
            } while (roomCodeAlreadyExists(roomCode))
            addInUseCode(roomCode, game)

            call.respondRedirect("/lobby-server/${game.id.value}")
        }

        //serves host lobby view w/ joined players
        get("/lobby-server/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))

            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val roomCode = retrieveRoomCode(game) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Room code not found")))

            val players = retrieveGamePlayers(game)

            call.respond(PebbleContent("lobby_server_view.peb", mapOf("quizName" to quiz.quiz_name, 
                "roomCode" to roomCode, 
                "players" to players, 
                "gameId" to gameId,
                "theme" to session.theme
            )))
        }

        //serves join lobby page for client
        get("/join-lobby") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val returnURL = if (session.role == "teacher" || session.role == "admin") {
                "/teacher-dashboard"
            } else {
                "/student-dashboard"
            }

            call.respond(PebbleContent("join_lobby.peb", mapOf("returnURL" to returnURL, "roomCode" to "", "errorMessage" to "")))

        }

        //validates room code, creates UserScore for client, redirects to client lobby view
        post("/join-lobby") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val parameters = call.receiveParameters()
            val roomCode = parameters["roomCode"]?.uppercase() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))

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
            val game = retrieveInUseCodeGame(inUseCode) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))

            //stop players joining after game started
            if (activeSessions[game.id.value]?.isStarted == true) {
                call.respond(PebbleContent("join_lobby.peb", mapOf(
                    "returnURL" to returnURL,
                    "roomCode" to roomCode,
                    "errorMessage" to "This Game has Already Started!"
                )))
                return@post
            }

            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            val gameSession = activeSessions[game.id.value]

            //block host from joining own game
            if (gameSession != null && session.username == gameSession.hostUsername) {
                call.respond(PebbleContent("join_lobby.peb", mapOf(
                    "returnURL" to returnURL,
                    "roomCode" to roomCode,
                    "errorMessage" to "You cannot join your own game!"
                )))
                return@post
            }

            val theme = gameSession?.theme ?: "default"
            call.sessions.set(session.copy(theme = theme))

            //block prev removed players from joining
            if (gameSession != null && session.username in gameSession.barredUsernames) {
                call.respond(PebbleContent("join_lobby.peb", mapOf(
                    "returnURL" to returnURL,
                    "roomCode" to roomCode,
                    "errorMessage" to "You have already been removed from this game!"
                )))
                return@post
            }

            //block players after lobby cap reached
            if (gameSession != null && retrieveGamePlayers(game).size >= gameSession.maxPlayers) {
                call.respond(PebbleContent("join_lobby.peb", mapOf(
                    "returnURL" to returnURL,
                    "roomCode" to roomCode,
                    "errorMessage" to "This lobby is full!"
                )))
                return@post
            }

            addUserScore(game, user)
            call.respondRedirect("/lobby-client/${game.id.value}")
        }
        
        //serves client lobby view, verified player is in game
        get("/lobby-client/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))

            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")                          //verify user is actually in game
            retrieveUserScore(game, user) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "You are not a player in this quiz")))

            val roomCode = retrieveRoomCode(game) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Room code not found")))

            call.respond(PebbleContent("lobby_client_view.peb", mapOf("quizName" to quiz.quiz_name, "roomCode" to roomCode, "gameId" to gameId, "theme" to session.theme)))
        }

        //removes UserScore entry for player, redirects to dashboard
        post("/leave-lobby/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login") 
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val game = retrieveGame(gameId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            val test = deleteUserScoreForGivenUser(game, user)
            if (!test) return@post call.respond(PebbleContent("error.peb",
                mapOf("errorMessage" to "Score for User not found")))

            val returnURL = if (session.role == "teacher" || session.role == "admin") {                                         //delete UserScore to remove player from game
                "/teacher-dashboard"
            } else {
                "/student-dashboard"
            }
            call.respondRedirect(returnURL)
        }
        
        //deletes room code, player scores and game, redirects host to dash
        post("/close-lobby/{gameId}") {
            call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val game = retrieveGame(gameId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))

            //always delete room code when lobby is closed
            deleteInUseCode(game)

            // IF game did not start, delete game and scores
            if (activeSessions[gameId]?.isStarted != true) {
                deleteGameUserScores(game)
                transaction(TransactionManager.defaultDatabase) { game.delete() }
            }
            call.respondRedirect("/teacher-dashboard")
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

        //TESTED UP TO HERE

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
            val session = call.sessions.get<UserSession>()?: return@post call.respondRedirect("/login") 
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
            //randomly select 6 players who answered wrong
            val dangerPlayers = answers
                .filter { it.chosenAnswer != correctAnswer }
                .filter { it.username !in gameSession.ghostPlayers }
                .map { it.username }
                .shuffled()
            
            gameSession.lastIncorrectPlayers = dangerPlayers
            gameSession.lastCorrectPlayers = answers
                .map { it.username }
                .filter {it !in dangerPlayers }

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

                //Need to remember to change this value back to .5
                gameSession.miniGamesEnabled
                    && gameSession.lastIncorrectPlayers.size >= 2
                    && gameSession.activeMinigames.isNotEmpty()
                    && !gameSession.minigameJustPlayed
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

        //polling endpoint, returns if game has started, used by lobby client view
        get("/game-started/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull()
                ?: return@get call.respondText("{\"started\":false}", ContentType.Application.Json)
            val started = activeSessions[gameId]?.isStarted ?: false
            call.respondText("{\"started\":$started}", ContentType.Application.Json)
        }

        //polling endpoint, returns player list in lobby, for host lobby view
        get("/lobby-players/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("[]", ContentType.Application.Json) 
            val game = retrieveGame(gameId) ?: return@get call.respondText("[]", ContentType.Application.Json)

            val players = retrieveGamePlayers(game)

            val json = "[" + players.joinToString(",") { "\"$it\"" } + "]" 
            call.respondText(json, ContentType.Application.Json)
        }

        //polling endpoint, returns current Q index and finished flag, used by client pos page
        get("/question-index/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("{\"index\":0}", ContentType.Application.Json)
            val session = activeSessions[gameId]
            val index = activeSessions[gameId]?.currentQuestionIndex ?: 0
            val finished = session?.isFinished ?: true

            call.respondText("{\"index\":$index,\"finished\":$finished}", ContentType.Application.Json)
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

        //clear session and redirect to home
        post("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }

        get("/user-stats") {
            val session = call.sessions.get<UserSession>()
                ?: return@get call.respondRedirect("/login")

            val user = retrieveUser(session.username)
                ?: return@get call.respondRedirect("/login")

            val attempts = retrieveUserAttempts(user)
            val returnURL = when(session.role) {
                "teacher" -> "/teacher-dashboard"
                "student" -> "/student-dashboard"
                else -> return@get call.respondRedirect("/login")
            }

            call.respond(
                PebbleContent("user_view_stats.peb", mapOf("attempts" to attempts, "returnURL" to returnURL))
            )
        }

        //gets game session details, populated and serves settings page
        get("/settings/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Inavlid gameID")))
            val returnUrl = call.request.queryParameters["return"] ?: "/lobby-server/$gameId"
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            call.respond(PebbleContent("settings.peb", mapOf(
                "gameId" to gameId,
                "returnUrl" to returnUrl,
                "theme" to session.theme,
                "timeLimit" to gameSession.timeLimit,
                "minigamesEnabled" to gameSession.miniGamesEnabled,
                "mathsEnabled" to gameSession.activeMinigames.contains("maths"),
                "potionsEnabled" to gameSession.activeMinigames.contains("potions"),
                "roomsEnabled" to gameSession.activeMinigames.contains("rooms"),
                "wordsEnabled" to gameSession.activeMinigames.contains("words")
            )))
        }

        //updates game session w/ new settings & redirects
        post("/settings/{gameId}") {
            call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val returnUrl = call.request.queryParameters["return"] ?: "/lobby-server/$gameId"
            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val parameters = call.receiveParameters()
            val timeLimit = parameters["timeLimit"]?.toIntOrNull() ?: gameSession.timeLimit
            val minigamesEnabled = parameters["minigames"] == "true"
            val activeMinigames = parameters.getAll("activeMinigames") ?: emptyList()

            activeSessions[gameId] = gameSession.copy(
                timeLimit = timeLimit,
                miniGamesEnabled = minigamesEnabled,
                activeMinigames = activeMinigames
            )

            call.respondRedirect(returnUrl)
        }

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
                call.respond(PebbleContent("mini_game_uhoh.peb",mapOf(
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

        //Potions Minigame - 
        //serves start screens to clients and host when host presses start
        post("/potions-begin/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            gameSession.connections.forEach { connection ->
                connection.send(Frame.Text("{\"redirect\":\"/potions-start/$gameId\"}"))
            }

            gameSession.potionsGameState?.isStarted = true

            call.respond(PebbleContent("potions_minigame_server.peb", mapOf(
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
                "isGhostCol3" to false
            )))
        }

        //sends clients to potions pick or safe screen
        get("/potions-start/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            val userScore = retrieveUserScore(game, user)?.score ?: 0
            val isExistingGhost = session.username in gameSession.ghostPlayers

            if (session.username in gameSession.lastIncorrectPlayers) {
                call.respond(PebbleContent("potions_minigame_client_pick.peb", mapOf(
                    "gameId" to gameId,
                    "theme" to session.theme
                )))
            } else{
                call.respond(PebbleContent("mini_games_maths_ur_safe.peb", mapOf(
                    "gameId" to gameId,
                    "score" to userScore,
                    "theme" to session.theme,
                    "isExistingGhost" to isExistingGhost
                )))
            }
        }

        //records potion pick and sends client to waiting screen
        post("/potions-pick/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val potionsState = gameSession.potionsGameState ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val parameters = call.receiveParameters()
            val potion = parameters["potion"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid selection")))

            //record pick
            potionsState.playerPicks[session.username] = potion

            call.respondRedirect("/potions-selected/$gameId")
        }

        //serves waiting screen after potion pick
        get("/potions-selected/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val potionsState = gameSession.potionsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val potion = potionsState.playerPicks[session.username] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No potion selected")))

            call.respond(PebbleContent("potions_minigame_client_selectd_ans.peb", mapOf(
                "gameId" to gameId,
                "potion" to potion,
                "theme" to session.theme
            )))
        }

        get("/potions-picks/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("{}", ContentType.Application.Json)
            val potionsState = activeSessions[gameId]?.potionsGameState ?: return@get call.respondText("{}", ContentType.Application.Json)

            //assign players to columns on server screen, arbitrary
            val players = potionsState.playerPicks.keys.toList()
            val column1 = players.take(2)
            val column2 = players.drop(2).take(2)
            val column3 = players.drop(4).take(2)

            val response = PotionsPicksResponse(
                column1 = column1,
                column2 = column2,
                column3 = column3,
                ghostUsers = potionsState.newGhosts,
                isFinished = potionsState.isFinished
            )
            call.respondText(Json.encodeToString(response), ContentType.Application.Json)
        }

        post("/potions-end/{gameId}") {
            call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val potionsState = gameSession.potionsGameState ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            // assign random potion to any danger player who didn't pick in time
            gameSession.lastIncorrectPlayers.forEach { username ->
                if (!potionsState.playerPicks.containsKey(username)) {
                    potionsState.playerPicks[username] = (1..4).random()
                }
            }

            // determine who picked the ghost potion
            potionsState.newGhosts = potionsState.playerPicks
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

        //serves potion reveal to host
        get("/potions-end-host/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val potionsState = gameSession.potionsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val players = potionsState.playerPicks.keys.toList()
            val col1 = players.take(2)
            val col2 = players.drop(2).take(2)
            val col3 = players.drop(4).take(2)

            call.respond(PebbleContent("potions_minigame_server.peb", mapOf(
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
                "isGhostCol3" to false
            )))
        }

        //serves personalised potions result to client
        get("/potions-result/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            val potionsState = gameSession.potionsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigames active")))

            val userScore = retrieveUserScore(game, user)?.score ?: 0
            val isExistingGhost = session.username in gameSession.ghostPlayers
                && session.username !in potionsState.newGhosts

            if (session.username in potionsState.newGhosts
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
            } else{
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
        //tested up to here

        //serves manage page with current player list
        get("/manage-participants/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@get call.respondText("Game not found")
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respondText("Quiz not found")

            val players = retrieveGamePlayers(game)

            call.respond(PebbleContent("manage_participants.peb", mapOf(
                "gameId" to gameId,
                "quizName" to quiz.quiz_name,
                "players" to players,
                "theme" to session.theme
            )))
        }

        //processes kicklist, removes scores, redirects clients to kicked route, redirects host back to settings
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
                        //remove UserScore entry
                        UserScore.find {
                            UserScoreTable.game eq game.id and (UserScoreTable.user eq user.id)
                        }.firstOrNull()?.delete()

                        //remove ghost entry if ghost
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

        //serves client page depending on if kicked
        get("/kicked/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("Invalid game ID")
            val game = retrieveGame(gameId) ?: return@get call.respondText("Game not found")
            val gameSession = activeSessions[gameId] ?: return@get call.respondText("No active session found")
            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            //send host back to settings
            if (session.username == gameSession.hostUsername) {
                call.respondRedirect("/settings/$gameId")
                return@get
            }

            val userScore = retrieveUserScore(game, user)

            if (userScore == null) {
                //score == null - kicked
                call.respond(PebbleContent("kicked.peb", mapOf(
                    "gameId" to gameId,
                    "theme" to session.theme
                )))
            } else {
                //score exists - not kicked
                call.respondRedirect("/client-position/$gameId")
            }
        }

        //polling endpoint - returns if current user kicked
        get("/is-kicked/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondText("{\"kicked\":false}", ContentType.Application.Json)
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respondText("{\"kicked\":false}", ContentType.Application.Json)
            val gameSession = activeSessions[gameId]
            val kicked = gameSession?.kickedUsernames?.contains(session.username) ?: false
            call.respondText("{\"kicked\":$kicked}", ContentType.Application.Json)
        }
        //Words minigame -
        //serves start screens to clients and host
        post("/words-begin/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val wordsState = gameSession.wordsGameState ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            gameSession.connections.forEach { connection ->
                connection.send(Frame.Text("{\"redirect\":\"/words-start/$gameId\"}"))
            }

            gameSession.wordsGameState?.isStarted = true

            call.respond(PebbleContent("words_minigame_server_playing.peb", mapOf(
                "quizName" to (retrieveQuizFromGameID(gameId)?.quiz_name ?: ""),
                "gameId" to gameId,
                "theme" to session.theme,
                "grid" to wordsState.grid
            )))
        }

        //serves all clients pick screen - safe players play along
        get("/words-start/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val wordsState = gameSession.wordsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            call.respond(PebbleContent("words_minigame_client_pick.peb", mapOf(
                "gameId" to gameId,
                "theme" to session.theme,
                "grid" to wordsState.grid,
                "errorMessage" to ""
            )))
        }

        //get word, validate against grid and dict, redirect if pass
        post("/words-submit/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val wordsState = gameSession.wordsGameState ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            val parameters = call.receiveParameters()
            val word = parameters["word"]?.uppercase()?.trim() ?: return@post call.respond(PebbleContent("words_minigame_client_pick.peb", mapOf(
                "gameId" to gameId,
                "theme" to session.theme,
                "grid" to wordsState.grid,
                "errorMessage" to "Please enter a valid word"
            )))

            //check letters in word against letters in grid
            val gridLetterCounts = wordsState.grid.groupingBy { it }.eachCount()
            val wordLetterCounts = word.map { it.toString() }.groupingBy { it }.eachCount()
            val lettersValid = wordLetterCounts.all { (letter, count) -> 
                (gridLetterCounts[letter] ?: 0) >= count
            }

            if (!lettersValid) {
                call.respond(PebbleContent("words_minigame_client_pick.peb", mapOf(
                    "gameId" to gameId,
                    "theme" to session.theme,
                    "grid" to wordsState.grid,
                    "errorMessage" to "Please enter a valid word"
                )))
                return@post
            }

            //check word against dict API
            val response = httpClient.get("https://api.dictionaryapi.dev/api/v2/entries/en/${word.lowercase()}")
            if (response.status != HttpStatusCode.OK) {
                call.respond(PebbleContent("words_minigame_client_pick.peb", mapOf(
                    "gameId" to gameId,
                    "theme" to session.theme,
                    "grid" to wordsState.grid,
                    "errorMessage" to "Please enter a valid word"
                )))
                return@post
            }

            //if both checks pass store word and redirect
            wordsState.playerWords[session.username] = word

            call.respondRedirect("/words-selected/$gameId")
        }

        //serves client waiting screen after word submitted
        get("/words-selected/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val wordsState = gameSession.wordsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            val word = wordsState.playerWords[session.username] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No word submitted")))

            call.respond(PebbleContent("words_minigame_client_selected.peb", mapOf(
                "gameId" to gameId,
                "theme" to session.theme,
                "word" to word
            )))
        }

        //process and calculate results for players
        post("/words-end/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))

            val wordsState = gameSession.wordsGameState ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            //players who submit late given empty string
            gameSession.lastIncorrectPlayers.forEach { username -> 
                if (!wordsState.playerWords.containsKey(username)) {
                    wordsState.playerWords[username] = ""
                }
            }

            //get shortest word length from dangerPlayers
            val dangerPlayerWords = gameSession.lastIncorrectPlayers.associateWith { username -> 
                wordsState.playerWords.getOrDefault(username, "")
            }

            val minLength = dangerPlayerWords.values.minOfOrNull { it.length } ?: 0

            //add players with shortest word to ghosts
            wordsState.ghostPlayers = dangerPlayerWords
                .filter { it.value.length == minLength }
                .map { it.key }

            gameSession.ghostPlayers.addAll(wordsState.ghostPlayers)

            //award bonus points for longest words if >3 incorrect players
            if (gameSession.lastIncorrectPlayers.size >= 3) {
                val topScorers = dangerPlayerWords
                    .filter { it.value.isNotEmpty() }
                    .entries
                    .sortedByDescending { it.value.length }

                transaction(TransactionManager.defaultDatabase) {
                    topScorers.getOrNull(0)?.let { (username, _) -> 
                        val bonusUser = retrieveUser(username)
                        val bonusGame = retrieveGame(gameId)
                        if (bonusUser != null && bonusGame != null) {
                            val userScore = retrieveUserScore(bonusGame, bonusUser)
                            if (userScore != null) userScore.score +=200 
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

            //mark minigame finished and redirect clients
            wordsState.isFinished = true

            gameSession.connections.forEach { connection ->
                connection.send(Frame.Text("{\"redirect\":\"/words-result/$gameId\"}"))
            }

            call.respond(HttpStatusCode.OK)
        }

        //serves list of created words to host screen
        get("/words-end-host/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val wordsState = gameSession.wordsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            //build list of username-word pairs for template
            val playerWords = wordsState.playerWords.map { (username, word) ->
                mapOf("username" to username, "word" to word.ifEmpty { "(no submission)" })
            }

            call.respond(PebbleContent("words_minigame_server_words.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "gameId" to gameId,
                "theme" to session.theme,
                "playerWords" to playerWords
            )))
        }

        //serves shortest words and new ghosts for host screen
        post("/words-reveal/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val quiz = retrieveQuizFromGameID(gameId) ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val wordsState = gameSession.wordsGameState ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            //get shortest words to display
            val shortestWords = wordsState.ghostPlayers.map { username ->
                wordsState.playerWords.getOrDefault(username, "(no submission)")
            }

            call.respond(PebbleContent("words_minigame_server_outcome_ghost.peb", mapOf(
                "quizName" to quiz.quiz_name,
                "gameId" to gameId,
                "theme" to session.theme,
                "shortestWords" to shortestWords,
                "ghostPlayers" to wordsState.ghostPlayers
            )))
        }

        //sends clients to respective results screens
        get("/words-result/{gameId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid gameID")))
            val gameSession = activeSessions[gameId] ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No active session found")))
            val game = retrieveGame(gameId) ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Game not found")))
            val user = retrieveUser(session.username) ?: return@get call.respondRedirect("/login")

            val wordsState = gameSession.wordsGameState ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "No minigame active")))

            val userScore = retrieveUserScore(game, user)?.score ?: 0
            val isExistingGhost = session.username in gameSession.ghostPlayers
                && session.username !in wordsState.ghostPlayers

            if (session.username in wordsState.ghostPlayers
                && session.username in gameSession.lastIncorrectPlayers) {
                call.respond(PebbleContent("mini_game_maths_ur_ghost.peb", mapOf(
                    "gameId" to gameId,
                    "score" to userScore,
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

        //serves uh-oh or safe screen while host is on intro
        get("/words-wait/{gameId}") {
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
        //tested up to here

        //serves uh-oh or safe screen while host is on intro
        get("/potions-wait/{gameId}") {
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


    }
}