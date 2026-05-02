package com.example

import io.kotest.core.spec.style.StringSpec
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.server.testing.testApplication

class DashboardRoutingTest :
    StringSpec({

        // protected routes, no session
        "GET /teacher_dashboard w/out session redirects to login - protected route guard redirects properly" {
            testApplication {
                application { testModule() }
                val client = createClient { followRedirects = false }
                val response = client.get("/teacher-dashboard")
                checkForRedirect(response, "/login")
            }
        }

        "GET /student_dashboard w/out session redirects to login - protected route guard redirects properly" {
            testApplication {
                application { testModule() }
                val client = createClient { followRedirects = false }
                val response = client.get("/student-dashboard")
                checkForRedirect(response, "/login")
            }
        }

        // Authenticated get routes
        "GET /teacher-dashboard with valid teacher session returns 200 OK with HTML - authenticated teacher access works" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                client.get("/teacher-dashboard").also { checkForHtml(it) }
            }
        }

        "GET /student-dashboard with valid student session returns 200 OK with HTML - authenticated student access works" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                client.get("/student-dashboard").also { checkForHtml(it) }
            }
        }

        "GET /user-stats w/ valid student session returns 200 OK w/ HTML - user stats page loads" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                val response = client.get("/user-stats")
                checkForHtml(response)
            }
        }

        "GET /user-stats w/ valid teacher session returns 200 OK w/ HTML - user stats page loads" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response = client.get("/user-stats")
                checkForHtml(response)
            }
        }

        "GET /user-stats w/out session redirects to login - protected route guard works" {
            testApplication {
                application { testModule() }
                val client = createClient { followRedirects = false }
                val response = client.get("/user-stats")
                checkForRedirect(response, "/login")
            }
        }
    })
