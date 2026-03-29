package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.*
import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.server.testing.*

class DatabaseTest: StringSpec({
    "TestDatabase initialises correctly" {
        testApplication {
            application { testModule() }

        }
    }
})