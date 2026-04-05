package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.*
import kotlin.test.Test
import kotlin.test.assertEquals
import io.kotest.matchers.string.shouldContain
import io.ktor.server.testing.*
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

class DatabaseTest: StringSpec({
    "User" {
        testApplication {
            application { testModule() }
            val response = client.get("/login").also { checkForHtml(it) }
        }
    }
})

fun checkForHtml(response: HttpResponse) {
    response.status shouldBe HttpStatusCode.OK
    response.headers["Content-Type"]?.shouldContain("text/html")
}