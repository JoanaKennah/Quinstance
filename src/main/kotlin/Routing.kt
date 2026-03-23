package com.example

import com.example.database.ProjectDatabase
import com.example.database.tables.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import io.pebbletemplates.pebble.loader.ClasspathLoader
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

import com.password4j.Password
import com.example.retrieveUser
import com.example.addUser
import com.example.database.objects.*
import com.example.addQuiz
import com.example.retrieveQuizQuestions
import com.example.addQuestion
import com.example.addAnswer



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
                    call.respondRedirect("/student-dashboard")
                }
                "teacher", "admin" -> {
                    call.respondRedirect("/teacher-dashboard")
                }
                else -> {
                    call.respondText("Failed to create new user")
                }
            }

        }

        get("/teacher-dashboard") {
            val username = "Teacher" // TODO: get from session
            call.respond(PebbleContent("teacher_dashboard.peb", mapOf("username" to username)))
        }

        get("/student-dashboard") {
            val username = "Student" // TODO: get from session
            call.respond(PebbleContent("student_dashboard.peb", mapOf("username" to username)))
        }
        get("/create-quiz") {
            call.respond(PebbleContent("create_quiz.peb", mapOf<String, Any>()))
        }

        post("/create-quiz") {
            val parameters = call.receiveParameters()
            val quizName = parameters["quizName"] ?: return@post call.respondText("Please enter a quiz name")
            val description = parameters["description"] ?: return@post call.respondText("Please enter a description")
            val keystage = parameters["keystage"] ?: return@post call.respondText("Please enter a key stage")
            val category = parameters["category"] ?: return@post call.respondText("Please enter a category")
            
            //NEEDED: Get actual user from session - This just grabs first available
            val user = transaction(ProjectDatabase.database) {
                User.all().firstOrNull()
            } ?: return@post call.respondText("No such user found")

            val quiz = transaction(ProjectDatabase.database) {
                addQuiz(user, quizName, description, category)
            }

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

    }
}