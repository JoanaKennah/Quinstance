package com.example

import io.ktor.server.application.Application
import io.ktor.server.pebble.PebbleContent
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions

fun Application.configureDashboardRouting() {
    routing {
        // Serves teacher dashboard if client has the role 'Teacher'
        get("/teacher-dashboard") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")

            if (session.role != "teacher") {
                return@get call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf(
                            "errorMessage" to "You do not have permission to access the teacher dashboard",
                        ),
                    ),
                )
            }

            call.respond(
                PebbleContent(
                    "teacher_dashboard.peb",
                    mapOf("username" to session.username),
                ),
            )
        }

        // Serves student dashboard if client has the role 'Student'
        get("/student-dashboard") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")

            if (session.role != "student") {
                return@get call.respond(
                    PebbleContent(
                        "error.peb",
                        mapOf(
                            "errorMessage" to "You do not have permission to access the student dashboard",
                        ),
                    ),
                )
            }

            call.respond(
                PebbleContent(
                    "student_dashboard.peb",
                    mapOf("username" to session.username),
                ),
            )
        }

        // Serves user statistic page showing all previous games the client has played
        get("/user-stats") {
            val session =
                call.sessions.get<UserSession>()
                    ?: return@get call.respondRedirect("/login")

            val user =
                retrieveUser(session.username)
                    ?: return@get call.respondRedirect("/login")

            val attempts = retrieveUserQuizAttempts(user)
            val returnURL =
                when (session.role) {
                    "teacher" -> "/teacher-dashboard"
                    "student" -> "/student-dashboard"
                    else -> return@get call.respondRedirect("/login")
                }

            call.respond(
                PebbleContent("user_view_stats.peb", mapOf("attempts" to attempts, "returnURL" to returnURL)),
            )
        }
    }
}
