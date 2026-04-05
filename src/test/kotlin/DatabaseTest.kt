package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.*
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.server.testing.*
import com.password4j.Password

class DatabaseTest: StringSpec({
    "Retrieve correct user data for user with username 'linguin0'" {
        testApplication {
            application { testModule() }
            val user = retrieveUser("linguin0")
            user.let {
                it shouldNotBe null
                it?.username shouldBe "linguin0"
                Password.check("password123", it?.password).withScrypt() shouldBe true
                it?.name shouldBe "Ethan Harris"
                it?.email shouldBe "ethan@gmail.com"
                it?.key_stage shouldBe "Further Education"
                it?.role shouldBe "student"
            }
        }
    }

    "Retrieve correct user data for user with username 'JoanaKennah'" {
        testApplication {
            application { testModule() }
            val user = retrieveUser("JoanaKennah")
            user.let {
                it shouldNotBe null
                it?.username shouldBe "JoanaKennah"
                Password.check("password456", it?.password).withScrypt() shouldBe true
                it?.name shouldBe "Joana Kennah"
                it?.email shouldBe "joanakennah@gmail.com"
                it?.key_stage shouldBe "Further Education"
                it?.role shouldBe "teacher"
            }
        }
    }

    "Retrieve correct user data for user with username 'MinghaoLe123'" {
        testApplication {
            application { testModule() }
            val user = retrieveUser("MinghaoLe123")
            user.let {
                it shouldNotBe null
                it?.username shouldBe "MinghaoLe123"
                Password.check("password789", it?.password).withScrypt() shouldBe true
                it?.name shouldBe "Minghao Le"
                it?.email shouldBe "tgqj4326@leeds.ac.uk"
                it?.key_stage shouldBe "Further Education"
                it?.role shouldBe "teacher"
            }
        }
    }

    "User doesn't exist returns null" {
        testApplication {
            application { testModule() }
            val user = retrieveUser("testUser")
            user shouldBe null
        }
    }

    "User with empty username throws an IllegalArgumentException" {
        testApplication {
            application { testModule() }
            shouldThrow<IllegalArgumentException> {
                retrieveUser("")
            }
        }
    }

    "Add new user 'Emily Williams'" {
        testApplication {
            application { testModule() }
            val user = addUser("spiritscout",
                "password111",
                "Emily Williams",
                "emily@gmail.com",
                "Further Education",
                "teacher")
        }
    }

})