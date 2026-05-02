package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
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

class GameRoutingTest :
    StringSpec({

        // invalid path fields
        "GET /leaderboard/NonInteger returns error message - non-int ID rejected" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                val response = client.get("/leaderboard/NonInteger")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Invalid gameID"
            }
        }

        "GET /leaderboard/99999 returns error message - missing Game ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                val response = client.get("/leaderboard/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Game not found"
            }
        }

        "GET /game/99999 returns error message - invalid game ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                val response = client.get("/game/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Game not found"
            }
        }

        // tests w/ live game state on game and question flow
        "POST /start-game/{gameId} w/ valid ID returns 200 OK w/ HTML - game starts, question visible on host view" {
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

                val response = client.post("/start-game/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "COMP2870 Quiz"
                response.bodyAsText() shouldContain "What is a finite state automation?"
            }
        }

        "POST /start-game/99999 w/ valid session returns error - missing game ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response = client.post("/start-game/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Game not found"
            }
        }

        "POST /show-correct-answer/{gameId} w/ valid ID returns 200 OK w/ HTML - host sees correct answer page" {
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
                client.post("/start-game/$gameId")

                val response = client.post("/show-correct-answer/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "COMP2870 Quiz"
                response.bodyAsText() shouldContain "What is a finite state automation?"
                response.bodyAsText() shouldContain "A model of computation with an extremely limited amount of memory"
            }
        }

        "POST /show-correct-answer/99999 w/ valid session returns error - missing game ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response = client.post("/show-correct-answer/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Game not found"
            }
        }

        "POST /show-positions/{gameId} w/ valid ID returns 200 OK w/ HTML - host sees leaderboard" {
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
                client.post("/start-game/$gameId")
                client.post("/show-correct-answer/$gameId")

                val response = client.post("/show-positions/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "COMP2870 Quiz"
            }
        }

        "POST /show-positions/99999 w/ valid session returns error - missing game ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response = client.post("/show-positions/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Game not found"
            }
        }

        "POST /show-positions/{gameId} w/ 1 incorrect player triggers auto-ghost - auto ghost logic works" {
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
                client.post("/show-correct-answer/$gameId")

                gameSession.lastIncorrectPlayers = listOf("linguin0")

                val response = client.post("/show-positions/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "COMP2870 Quiz"
                gameSession.autoGhostedPlayer shouldBe "linguin0"
                gameSession.ghostPlayers shouldContain "linguin0"
            }
        }

        "POST /next-question/{gameId} on last question redirects to leaderboard - game ends correctly and displays leaderboard" {
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
                client.post("/start-game/$gameId")
                client.post("/show-correct-answer/$gameId")
                client.post("/show-positions/$gameId")

                val response = client.post("/next-question/$gameId")
                checkForRedirect(response, "/leaderboard/")
            }
        }

        "POST /next-question/{gameId} mid-game returns 200 OK w/ HTML - next question served to host" {
            testApplication {
                application { testModule() }
                val client =
                    createClient {
                        install(HttpCookies)
                        followRedirects = false
                    }
                preLogin(client, "JoanaKennah", "password456")
                val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

                val question = addQuestion(quiz, "What is a Turing machine?", "furthereducation")
                addAnswer(
                    question,
                    "A theoretical model of computation",
                    "A physical computer built by Turing",
                    "A sorting algorithm",
                    null,
                    1,
                )

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
                client.post("/start-game/$gameId")
                client.post("/show-correct-answer/$gameId")
                client.post("/show-positions/$gameId")

                val response = client.post("/next-question/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "COMP2870 Quiz"
                response.bodyAsText() shouldContain "What is a Turing machine?"
            }
        }

        "POST /next-question/99999 w/ valid session returns error - missing game ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "JoanaKennah", "password456")
                val response = client.post("/next-question/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Game not found"
            }
        }

        // polling endpoints
        "GET /game-started/{gameId} returns false before game starts - gamestart polling works" {
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

                val response = client.get("/game-started/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "\"started\":false"
            }
        }

        "GET /game-started/{gameId} returns true after game starts - gamestart polling works" {
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
                client.post("/start-game/$gameId")

                val response = client.get("/game-started/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "\"started\":true"
            }
        }

        "GET /question-index/{gameId} returns correct index and finished false during game - polling for next Q works" {
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
                client.post("/start-game/$gameId")

                val response = client.get("/question-index/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "\"index\":0"
                response.bodyAsText() shouldContain "\"finished\":false"
            }
        }

        "GET /question-index/{gameId} returns finished true after game ends - polling next Q at end works" {
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
                client.post("/start-game/$gameId")
                client.post("/show-correct-answer/$gameId")
                client.post("/show-positions/$gameId")
                client.post("/next-question/$gameId")

                val response = client.get("/question-index/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "\"finished\":true"
            }
        }

        "GET /minigame-active/{gameId} returns true while minigame is active - polling for minigame works" {
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

                gameSession.mathsGameState = MathsGameState()

                val response = client.get("/minigame-active/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "\"active\":true"
            }
        }

        "GET /minigame-active/{gameId} returns false when minigame is finished - polling minigame end works" {
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

                gameSession.mathsGameState = MathsGameState()
                gameSession.mathsGameState!!.isFinished = true

                val response = client.get("/minigame-active/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "\"active\":false"
            }
        }

        "GET /minigame-active/{gameId} returns false when no minigame active - no minigame handled" {
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
                client.post("/start-game/$gameId")

                val response = client.get("/minigame-active/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "\"active\":false"
            }
        }

        // minigame redirect polling
        "GET /minigame-redirect/{gameId} returns null before any minigame starts - no minigame handled" {
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
                client.post("/start-game/$gameId")

                val response = client.get("/minigame-redirect/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "\"redirect\":null"
            }
        }

        "GET /minigame-redirect/{gameId} returns null when maths not yet started - isStarted check works" {
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

                gameSession.mathsGameState = MathsGameState()

                val response = client.get("/minigame-redirect/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "\"redirect\":null"
            }
        }

        "GET /minigame-redirect/{gameId} returns maths route after maths begin - maths redirect works" {
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

                gameSession.mathsGameState = MathsGameState()
                gameSession.mathsGameState!!.isStarted = true

                val response = client.get("/minigame-redirect/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "/minigame-question/$gameId"
            }
        }

        "GET /minigame-redirect/{gameId} returns potions route after potions begin - potions redirect works" {
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
                gameSession.potionsGameState!!.isStarted = true

                val response = client.get("/minigame-redirect/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "/potions-start/$gameId"
            }
        }

        "GET /minigame-redirect/{gameId} returns rooms route after rooms begin - rooms redirect works" {
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

                gameSession.roomsGameState = RoomsGameState()
                gameSession.roomsGameState!!.isStarted = true

                val response = client.get("/minigame-redirect/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "/rooms-start/$gameId"
            }
        }

        "GET /minigame-redirect/{gameId} returns words route after words begin - words redirect works" {
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

                gameSession.wordsGameState = WordsGameState()
                gameSession.wordsGameState!!.isStarted = true

                val response = client.get("/minigame-redirect/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "/words-start/$gameId"
            }
        }
    })
