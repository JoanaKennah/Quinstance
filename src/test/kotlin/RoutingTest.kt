

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

    

    

})

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