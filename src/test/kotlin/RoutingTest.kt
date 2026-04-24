package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.*
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.collections.shouldContainAny
import io.kotest.matchers.collections.shouldBeIn
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.HttpClient


class RoutingTest : StringSpec({
    //Public get routes
    "GET / returns 200 with HTML - root path works" {
        testApplication {
            application { testModule() }
            client.get("/").also { checkForHtml(it) }
        }
    }

    "GET /home returns 200 with HTML - homepage loads" {
        testApplication {
            application { testModule() }
            client.get("/home").also { checkForHtml(it) }
        }
    }

    "GET /login returns 200 with HTML - login page loads" {
        testApplication {
            application { testModule() }
            client.get("/login").also { checkForHtml(it) }
        }
    }

    "GET /signup returns 200 with HTML - signup page loads" {
        testApplication {
            application { testModule() }
            client.get("/signup").also { checkForHtml(it) }
        }
    }

    //protected routes, no session
    "GET /teacher_dashboard w/out session redirects to login - protected route guard redirects properly" {
        testApplication {
            application { testModule() }
            val client = createClient {
                followRedirects = false
            }
            val response = client.get("/teacher-dashboard")
            checkForRedirect(response, "/login")
        }
    }

    "GET /student_dashboard w/out session redirects to login - protected route guard redirects properly" {
        testApplication {
            application { testModule() }
            val client = createClient {
                followRedirects = false
            }
            val response = client.get("/student-dashboard")
            checkForRedirect(response, "/login")
        }
    }

    "GET /create-quiz w/out session redirects to login - protected route guard redirects properly" {
        testApplication {
            application { testModule() }
            val client = createClient {
                followRedirects = false
            }
            val response = client.get("/create-quiz")
            checkForRedirect(response, "/login")
        }
    }

    "GET /view-quizzes w/out session redirects to login - protected route guard redirects properly" {
        testApplication {
            application { testModule() }
            val client = createClient {
                followRedirects = false
            }
            val response = client.get("/view-quizzes")
            checkForRedirect(response, "/login")
        }
    }

    "GET /join-lobby w/out session redirects to login - protected route guard redirects properly" {
        testApplication {
            application { testModule() }
            val client = createClient {
                followRedirects = false
            }
            val response = client.get("/join-lobby")
            checkForRedirect(response, "/login")
        }
    }

    //login post routes
    "POST /login w/ wrong password returns 200 OK with error message - invalid creds handled" {
        testApplication {
            application { testModule() }
            val response = client.post("/login") {
                setBody(FormDataContent(Parameters.build {
                    append("username", "linguin0")
                    append("password", "wrong")
                }))
            }
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Incorrect password"

        }
    }

    "POST /login w/ missing password returns 200 OK with error message - missing creds handled" {
        testApplication {
            application { testModule() }
            val response = client.post("/login") {
                setBody(FormDataContent(Parameters.build {
                    append("username", "linguin0")
                }))
            }
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Missing input"

        }
    }

    "POST /login w/ valid student creds sends user to /student-dashboard - student login works" {
        testApplication {
            application { testModule() }
            val client = createClient { install(HttpCookies) }
            val response = client.post("/login") {
                setBody(FormDataContent(Parameters.build {
                    append("username", "linguin0")
                    append("password", "password123")
                }))
            }
            checkForRedirect(response,"/student-dashboard")

        }
    }

    "POST /login w/ valid teacher creds sends user to /teacher-dashboard - teacher login works" {
        testApplication {
            application { testModule() }
            val client = createClient { install(HttpCookies) }
            val response = client.post("/login") {
                setBody(FormDataContent(Parameters.build {
                    append("username", "JoanaKennah")
                    append("password", "password456")
                }))
            }
            checkForRedirect(response,"/teacher-dashboard")

        }
    }

    //signup post routes
    "POST /signup w/password mismatch returns 200 OK with error message - confirm password mismatch handled" {
        testApplication {
            application { testModule() }
            val response = client.post("/signup") {
                setBody(FormDataContent(Parameters.build {
                    append("firstname", "Test")
                    append("lastname", "User")
                    append("email", "test@test.com")
                    append("username", "testuser")
                    append("password", "password")
                    append("confirmpassword", "passwordmismatch")
                    append("role", "student")
                    append("keystage", "furthereducation")
                }))
            }
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Passwords don't match"
        }
    }

    "POST /signup w/ dupe username returns 200 OK with error message - dupe username handled" {
        testApplication {
            application { testModule() }
            val response = client.post("/signup") {
                setBody(FormDataContent(Parameters.build {
                    append("firstname", "Ethan")
                    append("lastname", "Harris")
                    append("email", "ethan@gmail.com")
                    append("username", "linguin0")
                    append("password", "password123")
                    append("confirmpassword", "password123")
                    append("role", "student")
                    append("keystage", "furthereducation")
                }))
            }
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Username already exists"
        }
    }

    "POST /signup w/ valid student data sends user to /student-dashboard - student signup success works" {
        testApplication {
            application { testModule() }
            val client = createClient { install(HttpCookies) }
            val response = client.post("/signup") {
                setBody(FormDataContent(Parameters.build {
                    append("firstname", "Test")
                    append("lastname", "User")
                    append("email", "test@gmail.com")
                    append("username", "newuser")
                    append("password", "password123")
                    append("confirmpassword", "password123")
                    append("role", "student")
                    append("keystage", "furthereducation")
                }))
            }
            checkForRedirect(response, "/student-dashboard")
        }
    }
    

    //Authenticated get routes
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

    "GET /join-lobby with valid student session returns 200 OK with HTML - authenticated student access works" {
        testApplication {
            application { testModule() } 
            val client = createClient { install(HttpCookies) }
            preLogin(client, "linguin0", "password123")
            client.get("/join-lobby").also { checkForHtml(it) }
        }
    }

    // -- edge cases --
    //non-int path parameters check with toIntOrNull
    "GET /view-quiz-questions/NonInteger returns error message - non-int ID rejected" {
        testApplication {
            application { testModule() }
            val response = client.get("/view-quiz-questions/NonInteger")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Quiz ID Invalid"
        }
    }

    "GET /add-question/NonInteger returns error message - non-int ID rejected" {
        testApplication {
            application { testModule() }
            val response = client.get("/add-question/NonInteger")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Invalid Quiz ID"
        }
    }

    "GET /view-quiz/NonInteger returns error message - non-int ID rejected" {
        testApplication {
            application { testModule() }
            val client = createClient { install(HttpCookies) }
            preLogin(client, "linguin0", "password123")
            val response = client.get("/view-quiz/NonInteger")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Invalid quiz ID"
        }
    }

    "GET /leaderboard/NonInteger returns error message - non-int ID rejected" {
        testApplication {
            application { testModule() }
            val client = createClient { install(HttpCookies) }
            preLogin(client, "linguin0", "password123")
            val response = client.get("/leaderboard/NonInteger")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Invalid game ID"
        }
    }
    
    //valid int but no record in database
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
            response.bodyAsText() shouldContain "No Quiz Found"
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

    //post route missing fields
    "POST /create-quiz w/ missing quiz name returns error message - createquiz missing field handled" {
        testApplication {
            application { testModule() }
            val client = createClient { install(HttpCookies) } 
            preLogin(client, "JoanaKennah", "password456") 
            val response = client.post("/create-quiz") {
                setBody(FormDataContent(Parameters.build {
                    append("description", "A test quiz")
                    append("keystage", "furthereducation")
                    append("category", "computerscience")
                }))
            }
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Please enter a quiz name"
        }
    }

    //invalid or missing room code 
    "POST /join-lobby w/ invalid room code returns error message - invalid room code handled" {
        testApplication {
            application { testModule() } 
            val client = createClient { install(HttpCookies) }
            preLogin(client, "linguin0", "password123")
            val response = client.post("/join-lobby") {
                setBody(FormDataContent(Parameters.build {
                    append("roomCode", "XXXXXXXX")
                }))
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
            val response = client.post("/join-lobby") {
                setBody(FormDataContent(Parameters.build {
                }))
            }
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Please enter a room code"
        }
    }

    //invalid path fields on auth. routes
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

    "GET /game/99999 returns error message - invalid game ID handled" {
        testApplication {
            application { testModule() }
            val client = createClient { install(HttpCookies) }
            preLogin(client, "linguin0", "password123")
            val response = client.get("/game/99999")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Game Not Found"
        }
    }

    //Tests round two
    //tests not requiring live game state
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
            val client = createClient { 
                install(HttpCookies)
                //inspect http response directly not redirect
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val response = client.post("/create-quiz") {
                //create quiz data
                setBody(FormDataContent(Parameters.build {
                    append("quizName", "Test Quiz")
                    append("description", "A test quiz description")
                    append("keystage", "furthereducation")
                    append("category", "other")
                }))
            }
            response.status shouldBe HttpStatusCode.Found
            response.headers["Location"] shouldContain "/view-quiz-questions/"
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
            val client = createClient {
                install(HttpCookies)
                //inspect http response directly not redirect
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()
            val response = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    //build quiz data
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            response.status shouldBe HttpStatusCode.Found
            response.headers["Location"] shouldContain "/lobby-server/"
        }
    }

    "POST /create-lobby/99999 w/ valid session returns error - missing quiz ID handled" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
            }
            preLogin(client, "JoanaKennah", "password456")
            val response = client.post("/create-lobby/99999") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Quiz not found"
        }
    }

    "GET /lobby-server/{gameId} w/ valid ID returns 200 OK w/ HTML - lobby server page loads" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            // post to create-lobby to get valid gameId
            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            //trim gameId off redirect location
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            // create a lobby to get a valid gameId
            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")

            // close lobby manually
            client.post("/close-lobby/$gameId")

            // verify game no longer exists in DB 
            val response = client.get("/lobby-server/$gameId")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Game not found"
        }
    }

    //simple checks on authenticated routes with logged in user
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

    //tests w/ live game state on game and question flow
    //start-game calls websockets w/ no connected clients but we are just testing HTML response
    //host view is served question via a call.respond(PebbleContent), independent of socket
    "POST /start-game/{gameId} w/ valid ID returns 200 OK w/ HTML - game starts, question visible on host view" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            //create lobby to gen valid gameId
            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            //trim URL to get ID
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")

            //start game and check content passed
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            //create and start game
            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            client.post("/start-game/$gameId")

            //show correct answer
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

    //models a full question flow, key test is intermediate leaderboard
    "POST /show-positions/{gameId} w/ valid ID returns 200 OK w/ HTML - host sees leaderboard" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            // create and start game with minigames disabled
            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            client.post("/start-game/$gameId")
            client.post("/show-correct-answer/$gameId")

            // show positions
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

    //testing auto-ghost for single incorrect player
    "POST /show-positions/{gameId} w/ 1 incorrect player triggers auto-ghost - auto ghost logic works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!

            client.post("/start-game/$gameId")
            client.post("/show-correct-answer/$gameId")

            //manually set one incorrect player
            gameSession.lastIncorrectPlayers = listOf("linguin0")

            val response = client.post("/show-positions/$gameId")
            checkForHtml(response)
            //host still sees leaderboard
            response.bodyAsText() shouldContain "COMP2870 Quiz"
            //verify player was auto ghosted and added to main list
            gameSession.autoGhostedPlayer shouldBe "linguin0"
            gameSession.ghostPlayers shouldContain "linguin0"
        }
    }

    "POST /next-question/{gameId} on last question redirects to leaderboard - game ends correctly and displays leaderboard" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            // create and start game
            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            //run through question flow
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            client.post("/start-game/$gameId")
            client.post("/show-correct-answer/$gameId")
            client.post("/show-positions/$gameId")

            //advance to next question should end game w/ 1 question
            val response = client.post("/next-question/$gameId")
            checkForRedirect(response, "/leaderboard/")
        }
    }

    "POST /next-question/{gameId} mid-game returns 200 OK w/ HTML - next question served to host" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            //add second question to the quiz to model game flow
            val question = addQuestion(quiz, "What is a Turing machine?", "furthereducation")
            addAnswer(question,
                "A theoretical model of computation",
                "A physical computer built by Turing",
                "A sorting algorithm",
                null,
                1)

            //create & start game
            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            client.post("/start-game/$gameId")
            client.post("/show-correct-answer/$gameId")
            client.post("/show-positions/$gameId")

            //advance to next question & check content
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
            response.bodyAsText() shouldContain "No active session found for this game"
        }
    }

    //MINIGAME TESTS
    //Maths minigame - game start
    "GET /minigame-start/{gameId} danger player sees uh-oh screen - danger player routing works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            //set up lobby
            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            //trim gameId off URL
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //manually set linguin0 as danger player - bypasses two player mininum condition
            gameSession.lastIncorrectPlayers = listOf("linguin0")
            gameSession.mathsGameState = MathsGameState()

            //check for uh-oh screen
            val response = client.get("/minigame-start/$gameId")
            checkForHtml(response)
            response.bodyAsText() shouldContain "Uh oh! You could become a ghost!"
            response.bodyAsText() shouldContain "Total Score:"
        }
    }

    "GET /minigame-start/{gameId} safe player sees safe screen - safe player routing works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            //linguin0 NOT in lastIncorrectPlayers so should see safe screen
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //add linguin0 to safe players
            gameSession.lastIncorrectPlayers = listOf("otherplayer")
            gameSession.lastCorrectPlayers = listOf("linguin0")
            gameSession.mathsGameState = MathsGameState()

            val response = client.get("/minigame-start/$gameId")
            checkForHtml(response)
            response.bodyAsText() shouldContain "You are safe!"
            response.bodyAsText() shouldContain "Total Score:"
        }
    }

    //maths minigame - pick & answer routes
    "GET /minigame-question/{gameId} returns 200 OK w/ HTML - maths question served to danger player" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            gameSession.lastIncorrectPlayers = listOf("linguin0")
            gameSession.mathsGameState = MathsGameState()

            val response = client.get("/minigame-question/$gameId")
            checkForHtml(response)
            //verify valid maths question is displayed, check for operator
            response.bodyAsText().let { body ->
                body shouldContainAny listOf("+", "-", "×", "÷")
            }
        }
    }

    "GET /minigame-correct/{gameId} increments score and redirects to next question - correct answer handled" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            gameSession.lastIncorrectPlayers = listOf("linguin0")
            gameSession.mathsGameState = MathsGameState()

            val response = client.get("/minigame-correct/$gameId")
            //should redirect to next question
            checkForRedirect(response, "/minigame-question/$gameId")
            //verify score was incremented
            gameSession.mathsGameState!!.mathsScores["linguin0"] shouldBe 1
            //verify Q index was incremented
            gameSession.mathsGameState!!.questionIndex["linguin0"] shouldBe 1
        }
    }

    "GET /minigame-incorrect/{gameId} returns 200 OK w/ HTML - incorrect answer stun screen served" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
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

    //maths minigame - end routes and ghost logic
    "POST /minigame-end/{gameId} lowest scoring player becomes ghost - ghost determination works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //set up two danger players w/ different scores
            gameSession.lastIncorrectPlayers = listOf("linguin0", "JoanaKennah")
            gameSession.mathsGameState = MathsGameState()
            //linguin0 scored 1, JoanaKennah scored 0 - JoanaKennah should become ghost
            gameSession.mathsGameState!!.mathsScores["linguin0"] = 1
            gameSession.mathsGameState!!.mathsScores["JoanaKennah"] = 0

            client.post("/minigame-end/$gameId")

            //verify ghost determination
            gameSession.mathsGameState!!.ghostPlayers shouldContain "JoanaKennah"
            gameSession.mathsGameState!!.ghostPlayers shouldNotContain "linguin0"
            gameSession.ghostPlayers shouldContain "JoanaKennah"
            gameSession.mathsGameState!!.isFinished shouldBe true
        }
    }

    "POST /minigame-end/{gameId} tied lowest score makes all tied players ghosts - tie handling works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //both players score zero
            gameSession.lastIncorrectPlayers = listOf("linguin0", "JoanaKennah")
            gameSession.mathsGameState = MathsGameState()

            client.post("/minigame-end/$gameId")

            //both zero scores should be ghosts
            gameSession.mathsGameState!!.ghostPlayers shouldContain "linguin0"
            gameSession.mathsGameState!!.ghostPlayers shouldContain "JoanaKennah"
            gameSession.ghostPlayers shouldContain "linguin0"
            gameSession.ghostPlayers shouldContain "JoanaKennah"
        }
    }

    "POST /minigame-end/{gameId} w/ 3+ danger players awards bonus points - bonus points logic works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //create 3 danger players for bonus points to trigger
            //create UserScore entries for bonus points to count
            val game = retrieveGame(gameId.toInt())!!
            val userLinguin = retrieveUser("linguin0")!!
            val userJoana = retrieveUser("JoanaKennah")!!
            val userMinghao = retrieveUser("MinghaoLe123")!!
            addUserScore(game, userLinguin)
            addUserScore(game, userJoana)
            addUserScore(game, userMinghao)

            gameSession.lastIncorrectPlayers = listOf("linguin0", "JoanaKennah", "MinghaoLe123")
            gameSession.mathsGameState = MathsGameState()
            //linguin0 scores 3, JoanaKennah scores 2, MinghaoLe123 scores 1
            gameSession.mathsGameState!!.mathsScores["linguin0"] = 3
            gameSession.mathsGameState!!.mathsScores["JoanaKennah"] = 2
            gameSession.mathsGameState!!.mathsScores["MinghaoLe123"] = 1

            client.post("/minigame-end/$gameId")

            //verify bonus points awarded
            val linguin0Score = retrieveUserScore(game, userLinguin)!!.score
            val joanaScore = retrieveUserScore(game, userJoana)!!.score
            linguin0Score shouldBe 200
            joanaScore shouldBe 100
        }
    }

    //possible client results of minigame
    "GET /minigame-result/{gameId} newly ghosted player sees ghost screen - ghost result routing works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //set up linguin0 as new ghost
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //linguin0 in danger but survived
            gameSession.lastIncorrectPlayers = listOf("linguin0")
            gameSession.mathsGameState = MathsGameState()
            //JoanaKennah ghosted
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //linguin0 is existing ghost from a previous minigame
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

    //potions minigame - client screen at start
    "GET /potions-start/{gameId} danger player sees pick screen - danger player routing works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //manually set linguin0 as danger player
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //set linguin0 safe
            gameSession.lastIncorrectPlayers = listOf("otherplayer")
            gameSession.lastCorrectPlayers = listOf("linguin0")
            gameSession.potionsGameState = PotionsGameState()

            val response = client.get("/potions-start/$gameId")
            checkForHtml(response)
            response.bodyAsText() shouldContain "You are safe!"
            response.bodyAsText() shouldContain "Total Score:"
        }
    }

    //player pick checks
    "POST /potions-pick/{gameId} records pick and redirects to waiting screen - potion pick works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            gameSession.lastIncorrectPlayers = listOf("linguin0")
            gameSession.potionsGameState = PotionsGameState()

            //set pick potion 2
            val response = client.post("/potions-pick/$gameId") {
                setBody(FormDataContent(Parameters.build {
                    append("potion", "2")
                }))
            }
            //should redirect to waiting screen
            checkForRedirect(response, "/potions-selected/$gameId")
            //check pick was recorded
            gameSession.potionsGameState!!.playerPicks["linguin0"] shouldBe 2
        }
    }

    //potions end scenarios
    "POST /potions-end/{gameId} player who picked ghost potion becomes ghost - ghost determination works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            gameSession.lastIncorrectPlayers = listOf("linguin0", "JoanaKennah")
            // force ghost potion to be 2
            gameSession.potionsGameState = PotionsGameState(ghostPotion = 2)
            // linguin0 picks ghost potion, JoanaKennah picks safe potion
            gameSession.potionsGameState!!.playerPicks["linguin0"] = 2
            gameSession.potionsGameState!!.playerPicks["JoanaKennah"] = 3

            client.post("/potions-end/$gameId")

            // verify ghost determination
            gameSession.potionsGameState!!.newGhosts shouldContain "linguin0"
            gameSession.potionsGameState!!.newGhosts shouldNotContain "JoanaKennah"
            gameSession.ghostPlayers shouldContain "linguin0"
            gameSession.potionsGameState!!.isFinished shouldBe true
        }
    }

    "POST /potions-end/{gameId} non-picker gets random potion assigned - random pick for late players works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            gameSession.lastIncorrectPlayers = listOf("linguin0", "JoanaKennah")
            gameSession.potionsGameState = PotionsGameState(ghostPotion = 2)
            //only linguin0 picks - JoanaKennah late pick
            gameSession.potionsGameState!!.playerPicks["linguin0"] = 3

            client.post("/potions-end/$gameId")

            //verify JoanaKennah was assigned random pot
            gameSession.potionsGameState!!.playerPicks.containsKey("JoanaKennah") shouldBe true
            val assignedPick = gameSession.potionsGameState!!.playerPicks["JoanaKennah"]!!
            assignedPick shouldBeIn listOf(1, 2, 3, 4)
        }
    }

    //potions player result scenarios
    "GET /potions-result/{gameId} newly ghosted player sees ghost screen - ghost result routing works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //set linguin0 as newly ghosted
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //linguin0 was dangerPlayer but picked safe pot
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //linguin0 existing ghost, JoanaKennah newly ghosted
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

    //rooms minigame - client start screens
    //same structure as prev. two
    "GET /rooms-start/{gameId} danger player sees pick screen - danger player routing works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
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

    "POST /rooms-pick/{gameId} records pick and redirects to waiting screen - room pick works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            gameSession.lastIncorrectPlayers = listOf("linguin0")
            gameSession.roomsGameState = RoomsGameState()

            //pick a room 
            val response = client.post("/rooms-pick/$gameId") {
                setBody(FormDataContent(Parameters.build {
                    append("room", "B")
                }))
            }
            checkForRedirect(response, "/rooms-selected/$gameId")
            //verify pick recorded
            gameSession.roomsGameState!!.playerPicks["linguin0"] shouldBe "B"
        }
    }

    "POST /rooms-pick/{gameId} invalid room returns error - validation works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            gameSession.lastIncorrectPlayers = listOf("linguin0")
            gameSession.roomsGameState = RoomsGameState()

            //pick invalid room - check catch in route works
            val response = client.post("/rooms-pick/$gameId") {
                setBody(FormDataContent(Parameters.build {
                    append("room", "Z")
                }))
            }
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "Invalid selection"
        }
    }

    //room end cases - includes ties & allSafe to cover all logic
    "POST /rooms-end/{gameId} busiest room becomes ghost room - ghost room determination works and isFinished flags" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            gameSession.lastIncorrectPlayers = listOf("linguin0", "JoanaKennah", "MinghaoLe123")
            gameSession.roomsGameState = RoomsGameState()
            //two pick A, one picks B - A should be ghost room
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            gameSession.lastIncorrectPlayers = listOf("linguin0", "JoanaKennah")
            gameSession.roomsGameState = RoomsGameState()
            //all players pick same room
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            gameSession.lastIncorrectPlayers = listOf("linguin0", "JoanaKennah")
            gameSession.roomsGameState = RoomsGameState()
            //only linguin0 picks - JoanaKennah late pick
            gameSession.roomsGameState!!.playerPicks["linguin0"] = "A"

            client.post("/rooms-end/$gameId")

            //verify JoanaKennah assigned random pick
            gameSession.roomsGameState!!.playerPicks.containsKey("JoanaKennah") shouldBe true
            val assignedPick = gameSession.roomsGameState!!.playerPicks["JoanaKennah"]!!
            assignedPick shouldBeIn listOf("A", "B", "C")
        }
    }

    //rooms client results scenarios
    "GET /rooms-result/{gameId} newly ghosted player sees ghost screen - ghost result routing works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //linguin0 new ghost
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //linguin0 was dangerPlayer, picked safe room
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "linguin0", "password123")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //linguin0 existing ghost, JoanaKennah new ghost
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

    /* Polling endpoint tests - 
    checking current state of game of lobby since HTTP cannot push updates -
    replaces websocket tests for now, checks JSON values returned */
    "GET /game-started/{gameId} returns false before game starts - gamestart polling works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")

            //before game starts should return false
            val response = client.get("/game-started/$gameId")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "\"started\":false"
        }
    }

    "GET /game-started/{gameId} returns true after game starts - gamestart polling works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            client.post("/start-game/$gameId")

            //after game starts should return true
            val response = client.get("/game-started/$gameId")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "\"started\":true"
        }
    }

    "GET /lobby-players/{gameId} returns empty list before players join - polling for players works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")

            //no players joined yet
            val response = client.get("/lobby-players/$gameId")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe "[]"
        }
    }

    "GET /lobby-players/{gameId} returns player list after player joins - polling for players works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")

            //linguin0 joins lobby
            val game = retrieveGame(gameId.toInt())!!
            val user = retrieveUser("linguin0")!!
            addUserScore(game, user)

            val response = client.get("/lobby-players/$gameId")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "linguin0"
        }
    }

    "GET /question-index/{gameId} returns correct index and finished false during game - polling for next Q works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            client.post("/start-game/$gameId")

            //on first question, index should be 0 and finished false
            val response = client.get("/question-index/$gameId")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "\"index\":0"
            response.bodyAsText() shouldContain "\"finished\":false"
        }
    }

    "GET /question-index/{gameId} returns finished true after game ends - polling next Q at end works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            client.post("/start-game/$gameId")
            client.post("/show-correct-answer/$gameId")
            client.post("/show-positions/$gameId")

            //advance past last question to end game
            client.post("/next-question/$gameId")

            //game finished so session removed - should return finished true
            val response = client.get("/question-index/$gameId")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "\"finished\":true"
        }
    }

    "GET /minigame-active/{gameId} returns true while minigame is active - polling for minigame works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //initialise maths minigame - isFinished defaults to false
            gameSession.mathsGameState = MathsGameState()

            val response = client.get("/minigame-active/$gameId")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "\"active\":true"
        }
    }

    "GET /minigame-active/{gameId} returns false when minigame is finished - polling minigame end works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            //initialise and mark minigame finished
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "false")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            client.post("/start-game/$gameId")

            //no mathsGameState initialised
            val response = client.get("/minigame-active/$gameId")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "\"active\":false"
        }
    }

    "GET /potions-picks/{gameId} returns empty columns before any picks - polling for column players works" {
        testApplication {
            application { testModule() }
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            gameSession.potionsGameState = PotionsGameState()

            //no picks yet, all columns should be empty
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            gameSession.potionsGameState = PotionsGameState()
            //set two player picks, should appear in column 1
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
            val client = createClient {
                install(HttpCookies)
                followRedirects = false
            }
            preLogin(client, "JoanaKennah", "password456")
            val quiz = retrieveUserQuizzes("JoanaKennah")!!.first()

            val createResponse = client.post("/create-lobby/${quiz.id.value}") {
                setBody(FormDataContent(Parameters.build {
                    append("timeLimit", "30")
                    append("theme", "default")
                    append("minigames", "true")
                }))
            }
            val gameId = createResponse.headers["Location"]!!.substringAfterLast("/")
            val gameSession = activeSessions[gameId.toInt()]!!
            client.post("/start-game/$gameId")

            gameSession.potionsGameState = PotionsGameState(ghostPotion = 2)
            gameSession.potionsGameState!!.playerPicks["linguin0"] = 2
            gameSession.potionsGameState!!.playerPicks["JoanaKennah"] = 3
            //mark linguin0 as ghost after reveal
            gameSession.potionsGameState!!.newGhosts = listOf("linguin0")

            val response = client.get("/potions-picks/$gameId")
            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldContain "\"ghostUsers\":[\"linguin0\"]"
        }
    }

})

// -- helper functions --
//checks for a 200 OK and correct landing
fun checkForHtml(response: HttpResponse) {
    response.status shouldBe HttpStatusCode.OK
    response.headers["Content-Type"]?.shouldContain("text/html")
}

//Checks for a 302 found and correct landing
fun checkForRedirect(response: HttpResponse, expectedLocation: String) {
    response.status shouldBe HttpStatusCode.Found
    response.headers["Location"] shouldContain expectedLocation
} 

//handles login request for some tests (inc auth. get routes)
suspend fun preLogin(client: HttpClient, username: String, password: String) {
    client.post("/login") {
        setBody(FormDataContent(Parameters.build {
            append("username", username)
            append("password", password)
        }))
    }
}