package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.*
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest: StringSpec({
    "Path / returns Welcome.peb file" {
        testApplication {
            application { module() }
            val response = client.get("/")
            response.status shouldBe HttpStatusCode.OK
            response.headers["Content-Type"]?.shouldContain("text/html")
        }
    }
})
