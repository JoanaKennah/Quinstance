package com.example

import com.example.database.ProjectDatabase
import com.password4j.Password
import io.ktor.http.Cookie
import io.ktor.server.application.*
import io.ktor.server.pebble.PebbleContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.GMTDate
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun ApplicationResponse.setLoginCookies(username: String, role: String) {
    cookies.append(
        Cookie(
            name = "username",
            value = username,
            path = "/",
            maxAge = 60 * 60,
            httpOnly = true
        )
    )

    cookies.append(
        Cookie(
            name = "role",
            value = role.lowercase(),
            path = "/",
            maxAge = 60 * 60,
            httpOnly = true
        )
    )
}

fun ApplicationResponse.clearLoginCookies() {
    cookies.append(
        Cookie(
            name = "username",
            value = "",
            path = "/",
            expires = GMTDate(0),
            httpOnly = true
        )
    )

    cookies.append(
        Cookie(
            name = "role",
            value = "",
            path = "/",
            expires = GMTDate(0),
            httpOnly = true
        )
    )
}

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
                val normalizedRole = user.role.lowercase()

                call.response.setLoginCookies(user.username, normalizedRole)

                when (normalizedRole) {
                    "student" -> {
                        call.respondRedirect("/student-dashboard")
                    }
                    "teacher", "admin" -> {
                        call.respondRedirect("/teacher-dashboard")
                    }
                    else -> {
                        call.respondText("Unknown role")
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

            val firstname = parameters["firstname"] ?: return@post call.respondText("Missing input")
            val lastname = parameters["lastname"] ?: return@post call.respondText("Missing input")
            val email = parameters["email"] ?: return@post call.respondText("Missing input")
            val username = parameters["username"] ?: return@post call.respondText("Missing input")
            val password = parameters["password"] ?: return@post call.respondText("Missing input")
            val confirmpassword = parameters["confirmpassword"] ?: return@post call.respondText("Missing input")
            val role = parameters["role"] ?: return@post call.respondText("Missing input")
            val keystage = parameters["keystage"] ?: return@post call.respondText("Missing input")

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
                    newUser.role.lowercase()
                }
            }

            when (newUserRole) {
                "USERNAME_EXISTS" -> {
                    call.respondText("Username already exists")
                }
                "student" -> {
                    call.response.setLoginCookies(username, "student")
                    call.respondRedirect("/student-dashboard")
                }
                "teacher", "admin" -> {
                    call.response.setLoginCookies(username, newUserRole)
                    call.respondRedirect("/teacher-dashboard")
                }
                else -> {
                    call.respondText("Failed to create new user")
                }
            }
        }

        get("/teacher-dashboard") {
            val username = call.request.cookies["username"]
            val role = call.request.cookies["role"]

            if (username == null || role == null) {
                call.respondRedirect("/login")
                return@get
            }

            if (role != "teacher" && role != "admin") {
                call.respondText("You do not have permission to access the teacher dashboard")
                return@get
            }

            call.respond(
                PebbleContent(
                    "teacher_dashboard.peb",
                    mapOf("username" to username)
                )
            )
        }

        get("/student-dashboard") {
            val username = call.request.cookies["username"]
            val role = call.request.cookies["role"]

            if (username == null || role == null) {
                call.respondRedirect("/login")
                return@get
            }

            if (role != "student") {
                call.respondText("You do not have permission to access the student dashboard")
                return@get
            }

            call.respond(
                PebbleContent(
                    "student_dashboard.peb",
                    mapOf("username" to username)
                )
            )
        }

        get("/logout") {
            call.response.clearLoginCookies()
            call.respondRedirect("/login")
        }
    }
}