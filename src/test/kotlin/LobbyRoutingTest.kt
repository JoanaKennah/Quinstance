package com.example

import com.example.database.objects.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.*
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*

class LobbyRoutingTest :
    StringSpec({

        // protected routes, no session
        "GET /join-lobby w/out session redirects to login - protected route guard redirects properly" {
            testApplication {
                application { testModule() }
                val client = createClient { followRedirects = false }
                val response = client.get("/join-lobby")
                checkForRedirect(response, "/login")
            }
        }

        // invalid or missing room code
        "POST /join-lobby w/ invalid room code returns error message - invalid room code handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                val response =
                    client.post("/join-lobby") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("roomCode", "XXXXXXXX")
                                },
                            ),
                        )
                    }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Invalid room code"
            }
        }

        "POST /join-lobby w/ missing room code returns error message - missing room code handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                val response =
                    client.post("/join-lobby") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                },
                            ),
                        )
                    }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Missing input(s)"
            }
        }

        "GET /join-lobby with valid student session returns 200 OK with HTML - authenticated student access works" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                client.get("/join-lobby").also { checkForHtml(it) }
            }
        }

        // invalid path fields
        "GET /lobby-server/99999 returns error message - invalid game ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response = client.get("/lobby-server/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Game not found"
            }
        }

        "GET /create-lobby/{quizId} w/ valid ID returns 200 OK w/ HTML - create lobby page loads" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()
                val response = client.get("/create-lobby/${quiz.id.value}")
                checkForHtml(response)
                response.bodyAsText() shouldContain "COMP2870 Quiz"
            }
        }

        "GET /create-lobby/99999 w/ valid session returns error - missing quiz ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response = client.get("/create-lobby/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Quiz not found"
            }
        }

        "GET /create-lobby/{quizId} w/out session redirects back to login - protected route guard works" {
            testApplication {
                application { testModule() }
                val client = createClient { followRedirects = false }
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()
                val response = client.get("/create-lobby/${quiz.id.value}")
                checkForRedirect(response, "/login")
            }
        }

        "POST /create-lobby/{quizId} w/ valid data redirects to lobby server - lobby creation works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "JoanaKennah", "password456")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()
                val response =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "false")
                                },
                            ),
                        )
                    }
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldContain "/lobby-server/"
            }
        }

        "POST /create-lobby/99999 w/ valid session returns error - missing quiz ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response =
                    client.post("/create-lobby/99999") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "false")
                                },
                            ),
                        )
                    }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Quiz not found"
            }
        }

        "GET /lobby-server/{gameId} w/ valid ID returns 200 OK w/ HTML - lobby server page loads" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "JoanaKennah", "password456")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "false")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")

                val response = client.get("/lobby-server/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "COMP2870 Quiz"
            }
        }

        "GET /lobby-server/99999 w/ valid session returns error - missing game ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response = client.get("/lobby-server/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Game not found"
            }
        }

        "POST /close-lobby/{gameId} w/ valid ID redirects to teacher dashboard - close lobby works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "JoanaKennah", "password456")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "false")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")

                val response = client.post("/close-lobby/$gameId")
                checkForRedirect(response, "/teacher-dashboard")
            }
        }

        "POST /close-lobby/99999 w/ valid session returns error - missing game ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response = client.post("/close-lobby/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Game not found"
            }
        }

        "POST /close-lobby/{gameId} removes game session - game cleaned up after close" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "JoanaKennah", "password456")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "false")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")

                client.post("/close-lobby/$gameId")

                val response = client.get("/lobby-server/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Game not found"
            }
        }

        "GET /lobby-players/{gameId} returns empty list before players join - polling for players works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "JoanaKennah", "password456")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "false")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")

                val response = client.get("/lobby-players/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "[]"
            }
        }

        "GET /lobby-players/{gameId} returns player list after player joins - polling for players works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "JoanaKennah", "password456")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "false")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")

                val game = retrieveGame(gameId.toInt())!!
                val user = retrieveUser("linguin0")!!
                addUserScore(game, user)

                val response = client.get("/lobby-players/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "linguin0"
            }
        }
    })
