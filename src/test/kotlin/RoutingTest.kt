package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.*
import io.kotest.matchers.string.shouldContain
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
            val response = client.get("/teacher-dashboard")
            checkForRedirect(response, "/login")
        }
    }

    "GET /student_dashboard w/out session redirects to login - protected route guard redirects properly" {
        testApplication {
            application { testModule() }
            val response = client.get("/student-dashboard")
            checkForRedirect(response, "/login")
        }
    }

    "GET /create-quiz w/out session redirects to login - protected route guard redirects properly" {
        testApplication {
            application { testModule() }
            val response = client.get("/create-quiz")
            checkForRedirect(response, "/login")
        }
    }

    "GET /view-quizzes w/out session redirects to login - protected route guard redirects properly" {
        testApplication {
            application { testModule() }
            val response = client.get("/view-quizzes")
            checkForRedirect(response, "/login")
        }
    }

    "GET /join-lobby w/out session redirects to login - protected route guard redirects properly" {
        testApplication {
            application { testModule() }
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