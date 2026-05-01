package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.*
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*

class QuizRoutingTest :
    StringSpec({

        // protected routes, no session
        "GET /create-quiz w/out session redirects to login - protected route guard redirects properly" {
            testApplication {
                application { testModule() }
                val client = createClient { followRedirects = false }
                val response = client.get("/create-quiz")
                checkForRedirect(response, "/login")
            }
        }

        "GET /view-quizzes w/out session redirects to login - protected route guard redirects properly" {
            testApplication {
                application { testModule() }
                val client = createClient { followRedirects = false }
                val response = client.get("/view-quizzes")
                checkForRedirect(response, "/login")
            }
        }

        // non-int path parameters
        "GET /view-quiz-questions/NonInteger returns error message - non-int ID rejected" {
            testApplication {
                application { testModule() }
                val response = client.get("/view-quiz-questions/NonInteger")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Invalid quizID"
            }
        }

        "GET /add-question/NonInteger returns error message - non-int ID rejected" {
            testApplication {
                application { testModule() }
                val response = client.get("/add-question/NonInteger")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Invalid quizID"
            }
        }

        "GET /view-quiz/NonInteger returns error message - non-int ID rejected" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                val response = client.get("/view-quiz/NonInteger")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Invalid quizID"
            }
        }

        // valid int but no record in database
        "GET /view-quiz-questions/99999 returns error message - missing Quiz ID handled" {
            testApplication {
                application { testModule() }
                val response = client.get("/view-quiz-questions/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Quiz not found"
            }
        }

        "GET /add-question/99999 returns error message - missing Quiz ID handled" {
            testApplication {
                application { testModule() }
                val response = client.get("/add-question/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Quiz not found"
            }
        }

        "GET /view-quiz/99999 returns error message - missing Quiz ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                val response = client.get("/view-quiz/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Quiz not found"
            }
        }

        // post route missing fields
        "POST /create-quiz w/ missing quiz name returns error message - createquiz missing field handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response =
                    client.post("/create-quiz") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("description", "A test quiz")
                                    append("keystage", "furthereducation")
                                    append("category", "computerscience")
                                },
                            ),
                        )
                    }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Missing input(s)"
            }
        }

        // authenticated routes
        "GET /create-quiz with valid teacher session returns 200 OK with HTML - authenticated teacher access works" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                client.get("/create-quiz").also { checkForHtml(it) }
            }
        }

        "GET /view-quizzes with valid student session returns 200 OK with HTML - authenticated student access works" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                client.get("/view-quizzes").also { checkForHtml(it) }
            }
        }

        "GET /view-quiz-questions/{quizId} w/ valid ID returns 200 OK w/ HTML - view quiz questions page loads" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                val quiz = retrieveUserQuizzes("linguin0")!!.first()
                val response = client.get("/view-quiz-questions/${quiz.id.value}")
                checkForHtml(response)
                response.bodyAsText() shouldContain "COMP2860 Quiz"
                response.bodyAsText() shouldContain "What is a process?"
            }
        }

        "GET /view-quiz/{quizId} w/ valid ID returns 200 OK w/ HTML - view quiz page loads" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                val quiz = retrieveUserQuizzes("linguin0")!!.first()
                val response = client.get("/view-quiz/${quiz.id.value}")
                checkForHtml(response)
                response.bodyAsText() shouldContain "COMP2860 Quiz"
                response.bodyAsText() shouldContain "Quiz to test my limited knowledge on COMP2860"
            }
        }

        "POST /create-quiz w/ valid data redirects to quiz questions page - quiz creation works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "JoanaKennah", "password456")
                val response =
                    client.post("/create-quiz") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("quizName", "Test Quiz")
                                    append("description", "A test quiz description")
                                    append("keystage", "furthereducation")
                                    append("category", "other")
                                },
                            ),
                        )
                    }
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldContain "/view-quiz-questions/"
            }
        }
    })
