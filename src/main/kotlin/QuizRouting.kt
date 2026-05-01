package com.example

import com.example.database.objects.Question
import com.example.database.objects.Quinstance
import com.example.database.objects.Quiz
import com.example.database.tables.QuestionTable
import com.example.database.tables.QuinstanceTable
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.pebble.PebbleContent
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.utils.io.jvm.javaio.toInputStream
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File

fun Application.configureQuizRouting() {
    routing {
        // gets quiz creation page
        get("/create-quiz") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")

            val returnURL =
                when (session.role) {
                    "teacher" -> "/teacher-dashboard"
                    "student" -> "/student-dashboard"
                    else -> return@get call.respondRedirect("/login")
                }

            call.respond(PebbleContent("create_quiz.peb", mapOf("returnURL" to returnURL)))
        }

        // saves created quiz to db, redirects to add Qs page
        post("/create-quiz") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")

            val parameters = call.receiveParameters()
            val quizName =
                parameters["quizName"] ?: return@post call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Missing input(s)"),
                    ),
                )
            val description =
                parameters["description"]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val keystage =
                parameters["keystage"] ?: return@post call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Missing input(s)"),
                    ),
                )
            val category =
                parameters["category"] ?: return@post call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Missing input(s)"),
                    ),
                )
            val minigamesEnabled = parameters["miniGames"] == "true"

            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            val quiz = addQuiz(user, quizName, description, category, keystage)

            call.respondRedirect("/view-quiz-questions/${quiz.id.value}")
        }

        // serves list of Qs in quiz
        get("/view-quiz-questions/{quizId}") {
            val quizId =
                call.parameters["quizId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))

            val quiz =
                retrieveQuiz(quizId) ?: return@get call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Quiz not found"),
                    ),
                )
            val questions = retrieveQuizQuestions(quiz)

            call.respond(
                PebbleContent(
                    "view_quiz_questions.peb",
                    mapOf(
                        "quizName" to quiz.quizName,
                        "questions" to questions,
                        "quizId" to quizId,
                        "returnURL" to "/view-quizzes",
                        "error" to "none",
                    ),
                ),
            )
        }

        post("/view-quiz-questions/{quidId}") {
            // References: https://tahaben.com.ly/2023/04/ktor-server-for-beginners-uploading-files/
            // https://ryanharrison.co.uk/2018/09/20/ktor-file-upload-download.html

            val multipart = call.receiveMultipart()
            var quizId: Int? = null
            var quiz: Quiz? = null
            var deleteQuestions: Boolean? = null
            var data: List<CsvQuestionData>? = null
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
                if (quizId == null) {
                    return@post call.respond(
                        PebbleContent(
                            "error.peb",
                            mapOf("errorMessage" to "Invalid quizID"),
                        ),
                    )
                }
                quiz =
                    retrieveQuiz(quizId) ?: return@post call.respond(
                        PebbleContent(
                            "error.peb",
                            mapOf("errorMessage" to "Quiz not found"),
                        ),
                    )

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
                            .toList()
                            .filterNot { it == "" }
                    val listOfAns =
                        mutableListOf(question.option1, question.option2, question.option3, question.option4)
                            .filterNot { it == "" }

                    if (checkAnswerDupes == listOfAns) {
                        val questionId = addQuestion(quiz, question.question, question.keyStage)

                        // Format option3 and option4 so they are accepted by the database.
                        val option3 =
                            when (question.option3) {
                                "" -> null
                                else -> question.option3
                            }
                        val option4 =
                            when (question.option4) {
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
                            question.correctOption.toInt(),
                        )
                    } else {
                        csvReadError = "repeatAnswers"
                    }
                }
            } else {
                csvReadError = "invalid"
            }

            if (quiz != null) {
                // Retrieve quiz questions and check to see if they're still empty, results in empty error if true.
                val questions = retrieveQuizQuestions(quiz)
                if (questions.isEmpty()) csvReadError = "empty"

                call.respond(
                    PebbleContent(
                        "view_quiz_questions.peb",
                        mapOf(
                            "quizName" to quiz.quizName,
                            "questions" to questions,
                            "quizId" to quiz.id,
                            "error" to csvReadError,
                            "returnURL" to "/view-quizzes",
                        ),
                    ),
                )
            } else {
                return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))
            }
        }

        post("/delete-question/{questionId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")

            val questionId =
                call.parameters["questionId"]?.toIntOrNull()
                    ?: return@post call.respondText("Invalid question ID")

            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            val quizId =
                transaction(TransactionManager.defaultDatabase) {
                    val question = Question.findById(questionId) ?: return@transaction null
                    val quinstance =
                        Quinstance.find { QuinstanceTable.question eq question.id }.firstOrNull()
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
            } else {
                call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "questionTemplate.csv does not exist")))
            }
        }

        // serves add question to quiz form
        get("/add-question/{quizId}") {
            val quizId =
                call.parameters["quizId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))

            val quiz =
                retrieveQuiz(quizId) ?: return@get call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Quiz not found"),
                    ),
                )

            call.respond(
                PebbleContent(
                    "add_question.peb",
                    mapOf<String, Any>(
                        "quizName" to quiz.quizName,
                        "quizId" to quizId,
                        "questionId" to "",
                        "isEdit" to false,
                        "previousQuestionText" to "",
                        "previousCorrectAnswer" to "",
                        "previousOption2" to "",
                        "previousAnswer3" to "",
                        "previousAnswer4" to "",
                    ),
                ),
            )
        }

        get("/edit-question/{questionId}") {
            val questionId =
                call.parameters["questionId"]?.toIntOrNull()
                    ?: return@get call.respond(
                        PebbleContent(
                            "error.peb",
                            mapOf("errorMessage" to "Invalid questionID"),
                        ),
                    )

            val quiz =
                transaction {
                    val quinstance = Quinstance.find { QuinstanceTable.question eq questionId }.firstOrNull()
                    if (quinstance != null) {
                        return@transaction quinstance.quiz
                    } else {
                        return@transaction null
                    }
                } ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz not found")))

            val question =
                transaction(TransactionManager.defaultDatabase) {
                    Question.find { QuestionTable.id eq questionId }.firstOrNull()
                } ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Question not found")))

            val answer =
                retrieveQuestionAnswer(question)
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Answer not found")))
            val option3 =
                when (answer.option3) {
                    null -> ""
                    else -> answer.option3.toString()
                }

            val option4 =
                when (answer.option4) {
                    null -> ""
                    else -> answer.option4.toString()
                }
            call.respond(
                PebbleContent(
                    "add_question.peb",
                    mapOf(
                        "quizName" to quiz.quizName,
                        "quizId" to quiz.id,
                        "questionId" to question.id,
                        "isEdit" to true,
                        "previousQuestionText" to question.question,
                        "previousCorrectAnswer" to answer.correctAnswer,
                        "previousOption2" to answer.option2,
                        "previousOption3" to option3,
                        "previousOption4" to option4,
                    ),
                ),
            )
        }

        // Reading question, all the answers, checking correct answer, saving to DB, redirects back to Q list
        // If isEditQuestion is true, instead of saving everything to the DB as new records, it edits previous records.
        post("/add-question/{quizId}") {
            val quizId =
                call.parameters["quizId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))

            val quiz =
                retrieveQuiz(quizId) ?: return@post call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Quiz not found"),
                    ),
                )

            val parameters = call.receiveParameters()
            val questionText =
                parameters["question"]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))

            val answers =
                parameters.getAll("answers[]")
                    ?: return@post call.respond(
                        PebbleContent(
                            "error.peb",
                            mapOf("errorMessage" to "Missing answer input(s)"),
                        ),
                    )

            // Stores index of correct answer (checkbox)
            val correctIndex =
                parameters["correct"]?.toIntOrNull()
                    ?: return@post call.respond(
                        PebbleContent(
                            "error.peb",
                            mapOf("errorMessage" to "Select a correct answer"),
                        ),
                    )

            val option1 =
                answers.getOrNull(0)
                    ?: return@post call.respond(
                        PebbleContent(
                            "error.peb",
                            mapOf("errorMessage" to "Missing answer input(s)"),
                        ),
                    )
            val option2 =
                answers.getOrNull(1)
                    ?: return@post call.respond(
                        PebbleContent(
                            "error.peb",
                            mapOf("errorMessage" to "Missing answer input(s)"),
                        ),
                    )
            val option3 = answers.getOrNull(2)
            val option4 = answers.getOrNull(3)

            val isEditQuestion = parameters["isEdit"].toBoolean()

            if (!isEditQuestion) {
                // Correct index here indicates correct answer for Database
                val question = addQuestion(quiz, questionText, quiz.keyStage)
                addAnswer(question, option1, option2, option3, option4, correctIndex)
            } else {
                // Different to addAnswer so a new record isn't made.
                val questionId =
                    parameters["questionId"]?.toIntOrNull()
                        ?: return@post call.respond(
                            PebbleContent(
                                "error.peb",
                                mapOf("errorMessage" to "Invalid question ID"),
                            ),
                        )

                val questionData =
                    mapOf(
                        "question" to questionText,
                        "keyStage" to quiz.keyStage,
                    )

                val mappedOption2 =
                    when (option3) {
                        null -> option2
                        else -> option3
                    }

                val mappedOption3 =
                    when (option3) {
                        null -> null
                        else -> option2
                    }

                val answerData =
                    mapOf(
                        "option1" to option1,
                        "option2" to mappedOption2,
                        "option3" to mappedOption3,
                        "option4" to option4,
                        "correctIndex" to correctIndex.toString(),
                    )

                val question =
                    transaction(TransactionManager.defaultDatabase) { Question.findById(questionId) }
                        ?: return@post call.respond(
                            PebbleContent(
                                "error.peb",
                                mapOf("errorMessage" to "Question doesn't exist"),
                            ),
                        )

                val answer =
                    retrieveQuestionAnswer(question)
                        ?: return@post call.respond(
                            PebbleContent(
                                "error.peb",
                                mapOf("errorMessage" to "Answer doesn't exist"),
                            ),
                        )

                editQuestion(question, questionData, answer, answerData)
            }
            call.respondRedirect("/view-quiz-questions/$quizId")
        }

        // serves list of quizzes created by user
        get("/view-quizzes") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")

            val returnURL =
                if (session.role == "teacher" || session.role == "admin") {
                    "/teacher-dashboard"
                } else {
                    "/student-dashboard"
                }

            val quizzes = retrieveUserQuizzes(session.username) ?: emptyList()

            call.respond(
                PebbleContent(
                    "view_quizzes.peb",
                    mapOf(
                        "username" to session.username,
                        "returnURL" to returnURL,
                        "quizzes" to quizzes,
                    ),
                ),
            )
        }

        post("/delete-quiz/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")

            val quizId =
                call.parameters["quizId"]?.toIntOrNull()
                    ?: return@post call.respondText("Invalid quiz ID")

            val user = retrieveUser(session.username) ?: return@post call.respondRedirect("/login")

            val quiz =
                retrieveQuiz(quizId) ?: return@post call
                    .respond(PebbleContent("error.peb", mapOf("errorMessage" to "Quiz doesn't exist")))

            val deleted =
                transaction(TransactionManager.defaultDatabase) {
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

        // serves conformation page for created quiz
        post("/quiz-created/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")

            val quizId =
                call.parameters["quizId"]?.toIntOrNull()
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))

            val returnURL =
                if (session.role == "teacher" || session.role == "admin") {
                    "/teacher-dashboard"
                } else {
                    "/student-dashboard"
                }

            call.respond(PebbleContent("quiz_created.peb", mapOf("quizId" to quizId, "returnURL" to returnURL)))
        }

        // serves specific quiz view, includes formatting for catagory and KStage
        get("/view-quiz/{quizId}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val quizId =
                call.parameters["quizId"]?.toIntOrNull()
                    ?: return@get call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Invalid quizID")))
            val quiz =
                retrieveQuiz(quizId) ?: return@get call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Quiz not found"),
                    ),
                )

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
                when (quiz.keyStage) {
                    "KS1" -> "Key Stage 1"
                    "KS2" -> "Key Stage 2"
                    "KS3" -> "Key Stage 3"
                    "KS4" -> "Key Stage 4"
                    "KS5" -> "Key Stage 5"
                    "furthereducation" -> "Further Education"
                    "nonapplicable" -> "N/A"
                    else -> quiz.keyStage
                }

            call.respond(
                PebbleContent(
                    "view_quizzes_selectd_quiz.peb",
                    mapOf(
                        "username" to session.username,
                        "quiz" to quiz,
                        "quizId" to quizId,
                        "categoryDisplay" to categoryDisplay,
                        "keyStageDisplay" to keyStageDisplay,
                    ),
                ),
            )
        }
    }
}
