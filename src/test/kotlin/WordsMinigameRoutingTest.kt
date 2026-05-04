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

class WordsMinigameRoutingTest :
    StringSpec({

        // words minigame - begin route
        "POST /words-begin/{gameId} host sees playing screen and isStarted flips - words begin works" {
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

                val response = client.post("/words-begin/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Make the longest"
                gameSession.wordsGameState!!.isStarted shouldBe true
            }
        }

        // words minigame - wait screen
        "GET /words-wait/{gameId} danger player sees uh-oh screen - danger player routing works" {
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
                gameSession.wordsGameState = WordsGameState()

                val response = client.get("/words-wait/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Uh oh! You could become a ghost!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        "GET /words-wait/{gameId} safe player sees safe screen - safe player routing works" {
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
                gameSession.wordsGameState = WordsGameState()

                val response = client.get("/words-wait/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are safe!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        // words minigame - grid screen
        "GET /words-start/{gameId} returns 200 OK w/ HTML and grid - populated grid served to client" {
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

                gameSession.wordsGameState = WordsGameState()

                val response = client.get("/words-start/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "Create a word with these letters!"
                response.bodyAsText() shouldContain "cell"
            }
        }

        "GET /words-start/99999 w/ valid session returns error - missing game ID handled" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                preLogin(client, "linguin0", "password123")
                val response = client.get("/words-start/99999")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "No active session found"
            }
        }

        // words minigame - word submission
        "POST /words-submit/{gameId} valid word redirects to waiting screen - valid submission works" {
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

                gameSession.wordsGameState =
                    WordsGameState(
                        grid =
                            listOf(
                                "C",
                                "A",
                                "T",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                            ),
                    )

                val response =
                    client.post("/words-submit/$gameId") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("word", "CAT")
                                },
                            ),
                        )
                    }
                checkForRedirect(response, "/words-selected/$gameId")
                gameSession.wordsGameState!!.playerWords["linguin0"] shouldBe "CAT"
            }
        }

        "POST /words-submit/{gameId} word with letters not in grid rejected - letter validation works" {
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

                gameSession.wordsGameState =
                    WordsGameState(
                        grid =
                            listOf(
                                "C",
                                "A",
                                "T",
                                "J",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                            ),
                    )

                val response =
                    client.post("/words-submit/$gameId") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("word", "JAZZ")
                                },
                            ),
                        )
                    }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Please enter a valid word"
            }
        }

        "POST /words-submit/{gameId} fake word rejected by dictionary - dictionary validation works" {
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

                gameSession.wordsGameState =
                    WordsGameState(
                        grid =
                            listOf(
                                "X",
                                "X",
                                "Y",
                                "Z",
                                "Z",
                                "Y",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                                "S",
                            ),
                    )

                val response =
                    client.post("/words-submit/$gameId") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("word", "XYZXYZ")
                                },
                            ),
                        )
                    }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Please enter a valid word"
            }
        }

        // words minigame - waiting screen after submission
        "GET /words-selected/{gameId} shows submitted word on waiting screen - words selected works" {
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

                gameSession.wordsGameState = WordsGameState()
                gameSession.wordsGameState!!.playerWords["linguin0"] = "CAT"

                val response = client.get("/words-selected/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You submitted:"
                response.bodyAsText() shouldContain "CAT"
            }
        }

        "GET /words-selected/{gameId} w/ no word submitted returns error - missing word handled" {
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

                gameSession.wordsGameState = WordsGameState()

                val response = client.get("/words-selected/$gameId")
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "No word submitted"
            }
        }

        // words minigame - end routes & ghost logic
        "POST /words-end/{gameId} shortest word player becomes ghost - ghost determination works" {
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
                gameSession.wordsGameState = WordsGameState()
                gameSession.wordsGameState!!.playerWords["linguin0"] = "CATS"
                gameSession.wordsGameState!!.playerWords["JoanaKennah"] = "CAT"

                client.post("/words-end/$gameId")

                gameSession.wordsGameState!!.ghostPlayers shouldContain "JoanaKennah"
                gameSession.wordsGameState!!.ghostPlayers shouldNotContain "linguin0"
                gameSession.ghostPlayers shouldContain "JoanaKennah"
                gameSession.wordsGameState!!.isFinished shouldBe true
            }
        }

        "POST /words-end/{gameId} tied shortest word makes all tied players ghosts - tie handling works" {
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
                gameSession.wordsGameState = WordsGameState()
                gameSession.wordsGameState!!.playerWords["linguin0"] = "CAT"
                gameSession.wordsGameState!!.playerWords["JoanaKennah"] = "ACT"

                client.post("/words-end/$gameId")

                gameSession.wordsGameState!!.ghostPlayers shouldContain "linguin0"
                gameSession.wordsGameState!!.ghostPlayers shouldContain "JoanaKennah"
                gameSession.ghostPlayers shouldContain "linguin0"
                gameSession.ghostPlayers shouldContain "JoanaKennah"
            }
        }

        "POST /words-end/{gameId} non-submitter becomes ghost - late submission handled" {
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
                gameSession.wordsGameState = WordsGameState()
                gameSession.wordsGameState!!.playerWords["linguin0"] = "CAT"

                client.post("/words-end/$gameId")

                gameSession.wordsGameState!!.playerWords["JoanaKennah"] shouldBe ""
                gameSession.wordsGameState!!.ghostPlayers shouldContain "JoanaKennah"
                gameSession.ghostPlayers shouldContain "JoanaKennah"
            }
        }

        "POST /words-end/{gameId} w/ 3+ danger players awards bonus points - bonus points logic works" {
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
                gameSession.wordsGameState = WordsGameState()
                gameSession.wordsGameState!!.playerWords["linguin0"] = "CATNIP"
                gameSession.wordsGameState!!.playerWords["JoanaKennah"] = "CATS"
                gameSession.wordsGameState!!.playerWords["MinghaoLe123"] = "CAT"

                client.post("/words-end/$gameId")

                val linguin0Score = retrieveUserScore(game, userLinguin)!!.score
                val joanaScore = retrieveUserScore(game, userJoana)!!.score
                linguin0Score shouldBe 200
                joanaScore shouldBe 100
            }
        }

        // words minigame - client result screens
        "GET /words-result/{gameId} newly ghosted player sees ghost screen - ghost result routing works" {
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
                gameSession.wordsGameState = WordsGameState()
                gameSession.wordsGameState!!.ghostPlayers = listOf("linguin0")
                gameSession.ghostPlayers.add("linguin0")

                val response = client.get("/words-result/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are now a ghost!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        "GET /words-result/{gameId} safe player sees safe screen - safe result routing works" {
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
                gameSession.wordsGameState = WordsGameState()
                gameSession.wordsGameState!!.ghostPlayers = listOf("JoanaKennah")
                gameSession.ghostPlayers.add("JoanaKennah")

                val response = client.get("/words-result/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are safe!"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }

        "GET /words-result/{gameId} existing ghost sees safe screen w/ veil text - existing ghost routing works" {
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
                gameSession.wordsGameState = WordsGameState()
                gameSession.wordsGameState!!.ghostPlayers = listOf("JoanaKennah")
                gameSession.ghostPlayers.add("linguin0")
                gameSession.ghostPlayers.add("JoanaKennah")

                val response = client.get("/words-result/$gameId")
                checkForHtml(response)
                response.bodyAsText() shouldContain "You are safe!"
                response.bodyAsText() shouldContain "(inside the veil)"
                response.bodyAsText() shouldContain "Total Score:"
            }
        }
    })
