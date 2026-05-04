package com.example

import com.password4j.Password
import io.ktor.server.application.Application
import io.ktor.server.pebble.PebbleContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

fun Application.configureAuthRouting() {
    routing {
        // Serves the funny logo on browser tabs
        get("/favicon.ico") {
            call.respond("/resources/favicon.ico")
        }

        // Serves homepage - default
        get("/") {
            call.respond(PebbleContent("homepage.peb", mapOf()))
        }

        // Serves homepage - dedicated
        get("/home") {
            call.respond(PebbleContent("homepage.peb", mapOf()))
        }

        // serves how to play page with info
        get("/how-to-play") {
            call.respond(PebbleContent("how_to_play.peb", mapOf()))
        }
        
        // Serves login page
        get("/login") {
            call.respond(PebbleContent("login.peb", mapOf()))
        }

        // Takes credentials & validates, sets session and redirects to dashboards
        post("/login") {
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

            // Set user session and redirect users to their dedicated dashboard if the password given is correct with the database.
            if (checkPassword) {
                call.sessions.set(
                    UserSession(
                        username = user.username,
                        role = user.role.lowercase(),
                    ),
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
            } else {
                call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Incorrect password")))
            }
        }

        // Serves reset password page
        get("/forgot-password") {
            call.respond(PebbleContent("forgot_password.peb", mapOf()))
        }

        // Takes email address and sends it the reset password email
        post("/forgot-password") {
            val parameters = call.receiveParameters()
            val email =
                parameters["email"] ?: return@post call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Missing email"),
                    ),
                )
            sendResetEmail(email)
            call.respondText("OK")
        }

        // Serves signup page
        get("/signup") {
            call.respond(PebbleContent("signup.peb", mapOf()))
        }

        // Validates signup form, creates user in database and redirects to dash
        post("/signup") {
            val parameters = call.receiveParameters()
            val firstname =
                parameters["firstname"] ?: return@post call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Missing input(s)"),
                    ),
                )
            val lastname =
                parameters["lastname"] ?: return@post call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Missing input(s)"),
                    ),
                )
            val email =
                parameters["email"] ?: return@post call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Missing input(s)"),
                    ),
                )
            val username =
                parameters["username"] ?: return@post call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Missing input(s)"),
                    ),
                )
            val password =
                parameters["password"] ?: return@post call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Missing input(s)"),
                    ),
                )
            val confirmPassword =
                parameters["confirmpassword"]
                    ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val role =
                parameters["role"] ?: return@post call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Missing input(s)"),
                    ),
                )
            val keystage =
                parameters["keystage"] ?: return@post call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf("errorMessage" to "Missing input(s)"),
                    ),
                )

            if (password != confirmPassword) {
                call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Passwords do not match")))
                return@post
            }

            val name = "$firstname $lastname"
            val createdUser = addUser(username, password, name, email, keystage, role)
            val newUserRole = createdUser?.role?.lowercase() ?: "USERNAME_EXISTS"

            // Sends new user an email confirming that they created an account
            if (createdUser != null && newUserRole != "USERNAME_EXISTS") {
                sendConfirmationEmail(
                    toEmail = email,
                    firstName = firstname,
                    lastName = lastname,
                    username = username,
                    password = password,
                )
            }

            // Set user session and redirect users to their dedicated dashboard after account creation
            when (newUserRole) {
                "USERNAME_EXISTS" -> {
                    call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Username already exists")))
                }
                "student" -> {
                    call.sessions.set(
                        UserSession(
                            username = createdUser!!.username,
                            role = createdUser.role.lowercase(),
                        ),
                    )
                    call.respondRedirect("/student-dashboard")
                }
                "teacher" -> {
                    call.sessions.set(
                        UserSession(
                            username = createdUser!!.username,
                            role = createdUser.role.lowercase(),
                        ),
                    )
                    call.respondRedirect("/teacher-dashboard")
                }
                else -> {
                    call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Failed to create new user")))
                }
            }
        }

        // Clear session and redirect to log in
        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/login")
        }

        // Clear session and redirect to home
        post("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }
    }
}
