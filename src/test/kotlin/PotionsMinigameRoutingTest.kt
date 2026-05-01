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

class PotionsMinigameRoutingTest :
    StringSpec({

        // potions minigame - begin route
        "POST /potions-begin/{gameId} host sees potions server screen and isStarted flips - potions begin works" {
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
                gameSession.potionsGameState = PotionsGameState()

                val response = client.post("/potions-begin/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Potions"
                gameSession.potionsGameState!!.isStarted shouldBe true
            }
        }

        "POST /potions-begin/99999 w/ valid session returns error - missing game ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response = client.post("/potions-begin/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "No active session found"
            }
        }

        // potions minigame - wait screen
        "GET /potions-wait/{gameId} danger player sees uh-oh screen - danger player routing works" {
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
                gameSession.potionsGameState = PotionsGameState()

                val response = client.get("/potions-wait/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Uh oh! You could become a ghost!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        "GET /potions-wait/{gameId} safe player sees safe screen - safe player routing works" {
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
                gameSession.potionsGameState = PotionsGameState()

                val response = client.get("/potions-wait/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are safe!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        // potions minigame - start screen
        "GET /potions-start/{gameId} danger player sees pick screen - danger player routing works" {
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
                gameSession.potionsGameState = PotionsGameState()

                val response = client.get("/potions-start/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Pick a potion to drink!"
            }
        }

        "GET /potions-start/{gameId} safe player sees safe screen - safe player routing works" {
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
                gameSession.potionsGameState = PotionsGameState()

                val response = client.get("/potions-start/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are safe!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        // potions minigame - pick
        "POST /potions-pick/{gameId} records pick and redirects to waiting screen - potion pick works" {
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
                gameSession.potionsGameState = PotionsGameState()

                val response =
                    client.post("/potions-pick/$gameId") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("potion", "2")
                                },
                            ),
                        )
                    }
                checkForRedirect(response, "/potions-selected/$gameId")
                gameSession.potionsGameState!!.playerPicks["linguin0"] shouldBe 2
            }
        }

        // potions minigame - end scenarios
        "POST /potions-end/{gameId} player who picked ghost potion becomes ghost - ghost determination works" {
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
                gameSession.potionsGameState = PotionsGameState(ghostPotion = 2)
                gameSession.potionsGameState!!.playerPicks["linguin0"] = 2
                gameSession.potionsGameState!!.playerPicks["JoanaKennah"] = 3

                client.post("/potions-end/$gameId")

                gameSession.potionsGameState!!.newGhosts shouldContain "linguin0"
                gameSession.potionsGameState!!.newGhosts shouldNotContain "JoanaKennah"
                gameSession.ghostPlayers shouldContain "linguin0"
                gameSession.potionsGameState!!.isFinished shouldBe true
            }
        }

        "POST /potions-end/{gameId} non-picker gets random potion assigned - random pick for late players works" {
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
                gameSession.potionsGameState = PotionsGameState(ghostPotion = 2)
                gameSession.potionsGameState!!.playerPicks["linguin0"] = 3

                client.post("/potions-end/$gameId")

                gameSession.potionsGameState!!.playerPicks.containsKey("JoanaKennah") shouldBe true
                val assignedPick = gameSession.potionsGameState!!.playerPicks["JoanaKennah"]!!
                assignedPick shouldBeIn listOf(1, 2, 3, 4)
            }
        }

        // potions minigame - client result screens
        "GET /potions-result/{gameId} newly ghosted player sees ghost screen - ghost result routing works" {
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
                gameSession.potionsGameState = PotionsGameState(ghostPotion = 2)
                gameSession.potionsGameState!!.newGhosts = listOf("linguin0")
                gameSession.ghostPlayers.add("linguin0")

                val response = client.get("/potions-result/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are now a ghost!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        "GET /potions-result/{gameId} safe player sees safe screen - safe result routing works" {
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
                gameSession.potionsGameState = PotionsGameState(ghostPotion = 2)
                gameSession.potionsGameState!!.newGhosts = listOf("JoanaKennah")
                gameSession.ghostPlayers.add("JoanaKennah")

                val response = client.get("/potions-result/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are safe!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        "GET /potions-result/{gameId} existing ghost sees safe screen w/ veil text - existing ghost routing works" {
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
                gameSession.potionsGameState = PotionsGameState(ghostPotion = 2)
                gameSession.potionsGameState!!.newGhosts = listOf("JoanaKennah")
                gameSession.ghostPlayers.add("linguin0")
                gameSession.ghostPlayers.add("JoanaKennah")

                val response = client.get("/potions-result/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are safe!"
                response.bodyAsText() shouldContain "(inside the veil)"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        // potions minigame - polling
        "GET /potions-picks/{gameId} returns empty columns before any picks - polling for column players works" {
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

                gameSession.potionsGameState = PotionsGameState()

                val response = client.get("/potions-picks/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "\"column1\":[]"
                response.bodyAsText() shouldContain "\"column2\":[]"
                response.bodyAsText() shouldContain "\"column3\":[]"
                response.bodyAsText() shouldContain "\"ghostUsers\":[]"
            }
        }

        "GET /potions-picks/{gameId} returns players in columns after picks - polling for column players works" {
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

                gameSession.potionsGameState = PotionsGameState()
                gameSession.potionsGameState!!.playerPicks["linguin0"] = 2
                gameSession.potionsGameState!!.playerPicks["JoanaKennah"] = 3

                val response = client.get("/potions-picks/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "linguin0"
                response.bodyAsText() shouldContain "JoanaKennah"
            }
        }

        "GET /potions-picks/{gameId} returns ghost users after reveal - potion ghosting works" {
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

                gameSession.potionsGameState = PotionsGameState(ghostPotion = 2)
                gameSession.potionsGameState!!.playerPicks["linguin0"] = 2
                gameSession.potionsGameState!!.playerPicks["JoanaKennah"] = 3
                gameSession.potionsGameState!!.newGhosts = listOf("linguin0")

                val response = client.get("/potions-picks/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "\"ghostUsers\":[\"linguin0\"]"
            }
        }
    })
