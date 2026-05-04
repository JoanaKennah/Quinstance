package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.testing.testApplication

class MathsMinigameRoutingTest :
    StringSpec({

        // maths minigame - begin route
        "POST /maths-begin/{gameId} host sees playing screen and isStarted flips - maths begin works" {
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
                gameSession.mathsGameState = MathsGameState()

                val response = client.post("/maths-begin/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Complete as many maths questions as possible to avoid becoming a ghost!"
                gameSession.mathsGameState!!.isStarted shouldBe true
            }
        }

        "POST /maths-begin/99999 w/ valid session returns error - missing game ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response = client.post("/maths-begin/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "No active session found"
            }
        }

        // maths minigame - start screen
        "GET /minigame-start/{gameId} danger player sees uh-oh screen - danger player routing works" {
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
                gameSession.mathsGameState = MathsGameState()

                val response = client.get("/minigame-start/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Uh oh! You could become a ghost!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        "GET /minigame-start/{gameId} safe player sees safe screen - safe player routing works" {
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
                gameSession.mathsGameState = MathsGameState()

                val response = client.get("/minigame-start/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are safe!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        // maths minigame - pick & answer routes
        "GET /minigame-question/{gameId} returns 200 OK w/ HTML - maths question served to danger player" {
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
                gameSession.mathsGameState = MathsGameState()

                val response = client.get("/minigame-question/$gameId")
                checkForHtml(response)
                response.bodyAsText().let { body ->
                    listOf("+", "-", "×", "÷").any { it in body } shouldBe true
                }
            }
        }

        "GET /minigame-correct/{gameId} increments score and redirects to next question - correct answer handled" {
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
                gameSession.mathsGameState = MathsGameState()

                val response = client.get("/minigame-correct/$gameId")
                checkForRedirect(response, "/minigame-question/$gameId")
                gameSession.mathsGameState!!.mathsScores["linguin0"] shouldBe 1
                gameSession.mathsGameState!!.questionIndex["linguin0"] shouldBe 1
            }
        }

        "GET /minigame-incorrect/{gameId} returns 200 OK w/ HTML - incorrect answer stun screen served" {
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
                gameSession.mathsGameState = MathsGameState()

                val response = client.get("/minigame-incorrect/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "That was incorrect, you have been temporarily stunned"
            }
        }

        // maths minigame - end routes and ghost logic
        "POST /minigame-end/{gameId} lowest scoring player becomes ghost - ghost determination works" {
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
                gameSession.mathsGameState = MathsGameState()
                gameSession.mathsGameState!!.mathsScores["linguin0"] = 1
                gameSession.mathsGameState!!.mathsScores["JoanaKennah"] = 0

                client.post("/minigame-end/$gameId")

                gameSession.mathsGameState!!.ghostPlayers shouldContain "JoanaKennah"
                gameSession.mathsGameState!!.ghostPlayers shouldNotContain "linguin0"
                gameSession.ghostPlayers shouldContain "JoanaKennah"
                gameSession.mathsGameState!!.isFinished shouldBe true
            }
        }

        "POST /minigame-end/{gameId} tied lowest score makes all tied players ghosts - tie handling works" {
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
                gameSession.mathsGameState = MathsGameState()

                client.post("/minigame-end/$gameId")

                gameSession.mathsGameState!!.ghostPlayers shouldContain "linguin0"
                gameSession.mathsGameState!!.ghostPlayers shouldContain "JoanaKennah"
                gameSession.ghostPlayers shouldContain "linguin0"
                gameSession.ghostPlayers shouldContain "JoanaKennah"
            }
        }

        "POST /minigame-end/{gameId} w/ 3+ danger players awards bonus points - bonus points logic works" {
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

                val game = retrieveGame(gameId.toInt())!!
                val userLinguin = retrieveUser("linguin0")!!
                val userJoana = retrieveUser("JoanaKennah")!!
                val userMinghao = retrieveUser("MinghaoLe123")!!
                addUserScore(game, userLinguin)
                addUserScore(game, userJoana)
                addUserScore(game, userMinghao)

                gameSession.lastIncorrectPlayers = listOf("linguin0", "JoanaKennah", "MinghaoLe123")
                gameSession.mathsGameState = MathsGameState()
                gameSession.mathsGameState!!.mathsScores["linguin0"] = 3
                gameSession.mathsGameState!!.mathsScores["JoanaKennah"] = 2
                gameSession.mathsGameState!!.mathsScores["MinghaoLe123"] = 1

                client.post("/minigame-end/$gameId")

                val linguin0Score = retrieveUserScore(game, userLinguin)!!.score
                val joanaScore = retrieveUserScore(game, userJoana)!!.score
                linguin0Score shouldBe 200
                joanaScore shouldBe 100
            }
        }

        // maths minigame - client result screens
        "GET /minigame-result/{gameId} newly ghosted player sees ghost screen - ghost result routing works" {
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
                gameSession.mathsGameState = MathsGameState()
                gameSession.mathsGameState!!.ghostPlayers = listOf("linguin0")
                gameSession.ghostPlayers.add("linguin0")

                val response = client.get("/minigame-result/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Total Score:"
                response.bodyAsText() shouldContain "You are now a ghost!"
            }
        }

        "GET /minigame-result/{gameId} safe player sees safe screen - safe result routing works" {
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
                gameSession.mathsGameState = MathsGameState()
                gameSession.mathsGameState!!.ghostPlayers = listOf("JoanaKennah")
                gameSession.ghostPlayers.add("JoanaKennah")

                val response = client.get("/minigame-result/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Total Score:"
                response.bodyAsText() shouldContain "You are safe!"
            }
        }

        "GET /minigame-result/{gameId} existing ghost sees safe screen w/ veil text - existing ghost routing works" {
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
                gameSession.mathsGameState = MathsGameState()
                gameSession.mathsGameState!!.ghostPlayers = listOf("JoanaKennah")
                gameSession.ghostPlayers.add("linguin0")
                gameSession.ghostPlayers.add("JoanaKennah")

                val response = client.get("/minigame-result/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Total Score:"
                response.bodyAsText() shouldContain "inside the veil"
            }
        }
    })
