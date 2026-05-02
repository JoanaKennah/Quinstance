package com.example

import io.kotest.core.spec.style.StringSpec
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

class AuthRoutingTest :
    StringSpec({

        // Public get routes
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

        // login post routes
        "POST /login w/ wrong password returns 200 OK with error message - invalid creds handled" {
            testApplication {
                application { testModule() }
                val response =
                    client.post("/login") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("username", "linguin0")
                                    append("password", "wrong")
                                },
                            ),
                        )
                    }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Incorrect password"
            }
        }

        "POST /login w/ missing password returns 200 OK with error message - missing creds handled" {
            testApplication {
                application { testModule() }
                val response =
                    client.post("/login") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("username", "linguin0")
                                },
                            ),
                        )
                    }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Missing input"
            }
        }

        "POST /login w/ valid student creds sends user to /student-dashboard - student login works" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                val response =
                    client.post("/login") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("username", "linguin0")
                                    append("password", "password123")
                                },
                            ),
                        )
                    }
                checkForRedirect(response, "/student-dashboard")
            }
        }

        "POST /login w/ valid teacher creds sends user to /teacher-dashboard - teacher login works" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                val response =
                    client.post("/login") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("username", "JoanaKennah")
                                    append("password", "password456")
                                },
                            ),
                        )
                    }
                checkForRedirect(response, "/teacher-dashboard")
            }
        }

        // signup post routes
        "POST /signup w/password mismatch returns 200 OK with error message - confirm password mismatch handled" {
            testApplication {
                application { testModule() }
                val response =
                    client.post("/signup") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("firstname", "Test")
                                    append("lastname", "User")
                                    append("email", "test@test.com")
                                    append("username", "testuser")
                                    append("password", "password")
                                    append("confirmpassword", "passwordmismatch")
                                    append("role", "student")
                                    append("keystage", "furthereducation")
                                },
                            ),
                        )
                    }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Passwords do not match"
            }
        }

        "POST /signup w/ dupe username returns 200 OK with error message - dupe username handled" {
            testApplication {
                application { testModule() }
                val response =
                    client.post("/signup") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("firstname", "Ethan")
                                    append("lastname", "Harris")
                                    append("email", "ethan@gmail.com")
                                    append("username", "linguin0")
                                    append("password", "password123")
                                    append("confirmpassword", "password123")
                                    append("role", "student")
                                    append("keystage", "furthereducation")
                                },
                            ),
                        )
                    }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "Username already exists"
            }
        }

        "POST /signup w/ valid student data sends user to /student-dashboard - student signup success works" {
            testApplication {
                application { testModule() }
                val client = createClient { install(HttpCookies) }
                val response =
                    client.post("/signup") {
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("firstname", "Test")
                                    append("lastname", "User")
                                    append("email", "test@gmail.com")
                                    append("username", "newuser")
                                    append("password", "password123")
                                    append("confirmpassword", "password123")
                                    append("role", "student")
                                    append("keystage", "furthereducation")
                                },
                            ),
                        )
                    }
                checkForRedirect(response, "/student-dashboard")
            }
        }
    })
