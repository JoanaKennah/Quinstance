package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import com.password4j.Password
import com.example.database.objects.*
import com.example.sendConfirmationEmail
import com.example.sendResetEmail

fun Application.configureAuthRouting() {
    routing {

        get("/favicon.ico") {
            call.respond("/resources/favicon.ico")
        }

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
            } else {
                call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Incorrect password")))
            }
        }

        //enter email to reset password page
        get("/forgot-password") {
            call.respond(PebbleContent("forgot_password.peb", mapOf()))
        }

        post("/forgot-password") {
            val parameters = call.receiveParameters()
            val email = parameters["email"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing email")))
            sendResetEmail(email)
            call.respondText("OK")
        }

        //serves signup page
        get("/signup") {
            call.respond(PebbleContent("signup.peb", mapOf()))
        }

        //validates signup form, creates user in database, redirects to dash
        post("/signup") {
            val parameters = call.receiveParameters()
            val firstname = parameters["firstname"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val lastname = parameters["lastname"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val email = parameters["email"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val username = parameters["username"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val password = parameters["password"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val confirmpassword = parameters["confirmpassword"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val role = parameters["role"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))
            val keystage = parameters["keystage"] ?: return@post call.respond(PebbleContent("error.peb", mapOf("errorMessage" to "Missing input(s)")))

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

        //clear session and redirect to home
        post("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }
    }
}