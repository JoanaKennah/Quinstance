package com.example

import com.example.database.objects.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.*
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*

class RoomsMinigameRoutingTest :
    StringSpec({

        // rooms minigame - begin route
        "POST /rooms-begin/{gameId} host sees rooms playing screen and isStarted flips - rooms begin works" {
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
                                    append("minigames", "true")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
                val gameSession = activeSessions[gameId.toInt()]!!
                client.post("/start-game/$gameId")

                gameSession.lastIncorrectPlayers = listOf("linguin0", "JoanaKennah")
                gameSession.roomsGameState = RoomsGameState()

                val response = client.post("/rooms-begin/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Pick a room to enter"
                gameSession.roomsGameState!!.isStarted shouldBe true
            }
        }

        "POST /rooms-begin/99999 w/ valid session returns error - missing game ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response = client.post("/rooms-begin/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "No active session found"
            }
        }

        // rooms minigame - wait screen
        "GET /rooms-wait/{gameId} danger player sees uh-oh screen - danger player routing works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "linguin0", "password123")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "true")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
                val gameSession = activeSessions[gameId.toInt()]!!
                client.post("/start-game/$gameId")

                gameSession.lastIncorrectPlayers = listOf("linguin0")
                gameSession.roomsGameState = RoomsGameState()

                val response = client.get("/rooms-wait/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Uh oh! You could become a ghost!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        "GET /rooms-wait/{gameId} safe player sees safe screen - safe player routing works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "linguin0", "password123")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "true")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
                val gameSession = activeSessions[gameId.toInt()]!!
                client.post("/start-game/$gameId")

                gameSession.lastIncorrectPlayers = listOf("otherplayer")
                gameSession.lastCorrectPlayers = listOf("linguin0")
                gameSession.roomsGameState = RoomsGameState()

                val response = client.get("/rooms-wait/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are safe!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        // rooms minigame - start screen
        "GET /rooms-start/{gameId} danger player sees pick screen - danger player routing works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "linguin0", "password123")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "true")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
                val gameSession = activeSessions[gameId.toInt()]!!
                client.post("/start-game/$gameId")

                gameSession.lastIncorrectPlayers = listOf("linguin0")
                gameSession.roomsGameState = RoomsGameState()

                val response = client.get("/rooms-start/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Pick a room to enter!"
            }
        }

        "GET /rooms-start/{gameId} safe player sees safe screen - safe player routing works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "linguin0", "password123")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "true")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
                val gameSession = activeSessions[gameId.toInt()]!!
                client.post("/start-game/$gameId")

                gameSession.lastIncorrectPlayers = listOf("otherplayer")
                gameSession.lastCorrectPlayers = listOf("linguin0")
                gameSession.roomsGameState = RoomsGameState()

                val response = client.get("/rooms-start/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are safe!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        // rooms minigame - pick
        "POST /rooms-pick/{gameId} records pick and redirects to waiting screen - room pick works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "linguin0", "password123")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "true")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
                val gameSession = activeSessions[gameId.toInt()]!!
                client.post("/start-game/$gameId")

                gameSession.lastIncorrectPlayers = listOf("linguin0")
                gameSession.roomsGameState = RoomsGameState()

                val response =
                    client.post("/rooms-pick/$gameId") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("room", "B")
                                },
                            ),
                        )
                    }
                checkForRedirect(response, "/rooms-selected/$gameId")
                gameSession.roomsGameState!!.playerPicks["linguin0"] shouldBe "B"
            }
        }

        "POST /rooms-pick/{gameId} invalid room returns error - validation works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "linguin0", "password123")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "true")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
                val gameSession = activeSessions[gameId.toInt()]!!
                client.post("/start-game/$gameId")

                gameSession.lastIncorrectPlayers = listOf("linguin0")
                gameSession.roomsGameState = RoomsGameState()

                val response =
                    client.post("/rooms-pick/$gameId") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("room", "Z")
                                },
                            ),
                        )
                    }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Invalid selection"
            }
        }

        // rooms minigame - end scenarios
        "POST /rooms-end/{gameId} busiest room becomes ghost room - ghost room determination works and isFinished flags" {
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
                                    append("minigames", "true")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
                val gameSession = activeSessions[gameId.toInt()]!!
                client.post("/start-game/$gameId")

                gameSession.lastIncorrectPlayers = listOf("linguin0", "JoanaKennah", "MinghaoLe123")
                gameSession.roomsGameState = RoomsGameState()
                gameSession.roomsGameState!!.playerPicks["linguin0"] = "A"
                gameSession.roomsGameState!!.playerPicks["JoanaKennah"] = "A"
                gameSession.roomsGameState!!.playerPicks["MinghaoLe123"] = "B"

                client.post("/rooms-end/$gameId")

                gameSession.roomsGameState!!.ghostRoom shouldBe "A"
                gameSession.roomsGameState!!.newGhosts shouldContain "linguin0"
                gameSession.roomsGameState!!.newGhosts shouldContain "JoanaKennah"
                gameSession.roomsGameState!!.newGhosts shouldNotContain "MinghaoLe123"
                gameSession.ghostPlayers shouldContain "linguin0"
                gameSession.ghostPlayers shouldContain "JoanaKennah"
                gameSession.roomsGameState!!.isFinished shouldBe true
            }
        }

        "POST /rooms-end/{gameId} all players same room triggers all safe - all safe logic works" {
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
                                    append("minigames", "true")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
                val gameSession = activeSessions[gameId.toInt()]!!
                client.post("/start-game/$gameId")

                gameSession.lastIncorrectPlayers = listOf("linguin0", "JoanaKennah")
                gameSession.roomsGameState = RoomsGameState()
                gameSession.roomsGameState!!.playerPicks["linguin0"] = "C"
                gameSession.roomsGameState!!.playerPicks["JoanaKennah"] = "C"

                client.post("/rooms-end/$gameId")

                gameSession.roomsGameState!!.allSafe shouldBe true
                gameSession.roomsGameState!!.newGhosts shouldBe emptyList()
                gameSession.ghostPlayers.size shouldBe 0
                gameSession.roomsGameState!!.isFinished shouldBe true
            }
        }

        "POST /rooms-end/{gameId} non-picker gets random room assigned - random pick for late players works" {
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
                                    append("minigames", "true")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
                val gameSession = activeSessions[gameId.toInt()]!!
                client.post("/start-game/$gameId")

                gameSession.lastIncorrectPlayers = listOf("linguin0", "JoanaKennah")
                gameSession.roomsGameState = RoomsGameState()
                gameSession.roomsGameState!!.playerPicks["linguin0"] = "A"

                client.post("/rooms-end/$gameId")

                gameSession.roomsGameState!!.playerPicks.containsKey("JoanaKennah") shouldBe true
                val assignedPick = gameSession.roomsGameState!!.playerPicks["JoanaKennah"]!!
                assignedPick shouldBeIn listOf("A", "B", "C")
            }
        }

        // rooms minigame - client result screens
        "GET /rooms-result/{gameId} newly ghosted player sees ghost screen - ghost result routing works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "linguin0", "password123")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "true")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
                val gameSession = activeSessions[gameId.toInt()]!!
                client.post("/start-game/$gameId")

                gameSession.lastIncorrectPlayers = listOf("linguin0")
                gameSession.roomsGameState = RoomsGameState()
                gameSession.roomsGameState!!.newGhosts = listOf("linguin0")
                gameSession.ghostPlayers.add("linguin0")

                val response = client.get("/rooms-result/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are now a ghost!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        "GET /rooms-result/{gameId} safe player sees safe screen - safe result routing works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "linguin0", "password123")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "true")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
                val gameSession = activeSessions[gameId.toInt()]!!
                client.post("/start-game/$gameId")

                gameSession.lastIncorrectPlayers = listOf("linguin0")
                gameSession.roomsGameState = RoomsGameState()
                gameSession.roomsGameState!!.newGhosts = listOf("JoanaKennah")
                gameSession.ghostPlayers.add("JoanaKennah")

                val response = client.get("/rooms-result/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are safe!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        "GET /rooms-result/{gameId} existing ghost sees safe screen w/ veil text - existing ghost routing works" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "linguin0", "password123")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val createResponse =
                    client.post("/create-lobby/${quiz.id.value}") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("timeLimit", "30")
                                    append("theme", "default")
                                    append("minigames", "true")
                                },
                            ),
                        )
                    }
                val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
                val gameSession = activeSessions[gameId.toInt()]!!
                client.post("/start-game/$gameId")

                gameSession.lastIncorrectPlayers = listOf("JoanaKennah")
                gameSession.roomsGameState = RoomsGameState()
                gameSession.roomsGameState!!.newGhosts = listOf("JoanaKennah")
                gameSession.ghostPlayers.add("linguin0")
                gameSession.ghostPlayers.add("JoanaKennah")

                val response = client.get("/rooms-result/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are safe!"
                response.bodyAsText() shouldContain "(inside the veil)"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }
    })
