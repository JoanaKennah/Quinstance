package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.*
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.server.testing.*

class ApplicationTest: StringSpec({
    "Path / returns welcome.peb file" {
        testApplication {
            application { testModule() }
            client.get("/").also { checkForHtml(it) }
        }
    }

    // May need more tests for the welcome page if we include photos/graphics.

    "Path /login returns login.peb file" {
        testApplication {
            application { testModule() }
            client.get("/login").also { checkForHtml(it) }
        }
    }
})

fun checkForHtml(response: HttpResponse) {
    response.status shouldBe HttpStatusCode.OK
    response.headers["Content-Type"]?.shouldContain("text/html")
}