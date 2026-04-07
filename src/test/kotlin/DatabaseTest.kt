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
import io.kotest.matchers.string.shouldHaveLength

class DatabaseTest: StringSpec({
    // Testing the functions used for Login / Signup

    "Retrieve correct user data for user with username 'linguin0'" {
        testApplication {
            application { testModule() }
            val user = retrieveUser("linguin0")
            println(user)
            user.let {
                it shouldNotBe null
                it?.username shouldBe "linguin0"
                Password.check("password123", it?.password).withScrypt() shouldBe true
                it?.name shouldBe "Ethan Harris"
                it?.email shouldBe "ethan@gmail.com"
                it?.key_stage shouldBe "furthereducation"
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
                it?.key_stage shouldBe "furthereducation"
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
                it?.key_stage shouldBe "furthereducation"
                it?.role shouldBe "teacher"
            }
        }
    }

    "Retrieving user that doesn't exist returns null" {
        testApplication {
            application { testModule() }
            val user = retrieveUser("testUser")
            user shouldBe null
        }
    }

    "Retrieving user with empty username throws an IllegalArgumentException" {
        testApplication {
            application { testModule() }
            shouldThrow<IllegalArgumentException> {
                retrieveUser("")
            }
        }
    }

    "Add new user 'Emily Williams' with appropriate data" {
        testApplication {
            application { testModule() }
            val user = addUser(
                "spiritscout",
                "password111",
                "Emily Williams",
                "emily@gmail.com",
                "furthereducation",
                "teacher")
            user shouldNotBe null
        }
    }

    "Add new user 'Alfie Whitham' with a username of length 50 (boundary)" {
        testApplication {
            application { testModule() }
            val user = addUser(
                "alfiewhithamwithausernamewithlengthfifty0123456789",
                "normalpassword",
                "Alfie Whitham",
                "alfie@gmail.com",
                "furthereducation",
                "teacher")
            user shouldNotBe null
        }
    }

    "Add new user 'Alfie Whitham' with a username of length > 50 should throw IllegalArgumentException" {
        testApplication {
            application { testModule() }
            shouldThrow<IllegalArgumentException> {
                addUser(
                    "alfiewhithamwithausernamewithlengthfifty0123456789999",
                    "normalpassword",
                    "Alfie Whitham",
                    "alfie@gmail.com",
                    "furthereducation",
                    "teacher"
                )
            }
        }
    }

    "Add new user 'Alfie Whitham' with an email of length 50 (boundary)" {
        testApplication {
            application { testModule() }
            val user = addUser(
                "alfxe87",
                "normalpassword",
                "Alfie Whitham",
                "alfiewhithamhasareallycoolawesomeemail20@gmail.com",
                "furthereducation",
                "teacher")
            user shouldNotBe null
        }
    }

    "Add new user 'Alfie Whitham' with an email of length > 50 should throw IllegalArgumentException" {
        testApplication {
            application { testModule() }
            shouldThrow<IllegalArgumentException> {
                addUser("alfxe87",
                        "normalpassword",
                        "Alfie Whitham",
                        "alfiewhithamhasareallycoolawesomeemail2000@gmail.com",
                        "furthereducation",
                        "teacher")
            }
        }
    }

    "Add new user with an empty username should throw IllegalArgumentException" {
        testApplication {
            application { testModule() }
            shouldThrow<IllegalArgumentException> {
                addUser(
                    "",
                    "normalpassword",
                    "Alfie Whitham",
                    "alfie@gmail.com",
                    "furthereducation",
                    "teacher")
            }
        }
    }

    "Add new user with an empty password should throw IllegalArgumentException" {
        testApplication {
            application { testModule() }
            shouldThrow<IllegalArgumentException> {
                addUser("alfxe87",
                        "",
                        "Alfie Whitham",
                        "alfie@gmail.com",
                        "furthereducation",
                        "teacher")
            }
        }
    }

    "Add new user with an empty name should throw IllegalArgumentException" {
        testApplication {
            application { testModule() }
            shouldThrow<IllegalArgumentException> {
                addUser("alfxe87",
                        "normalpassword",
                        "",
                        "alfie@gmail.com",
                        "furthereducation",
                        "teacher")
            }
        }
    }

    "Add new user with an empty email should throw IllegalArgumentException" {
        testApplication {
            application { testModule() }
            shouldThrow<IllegalArgumentException> {
                addUser("alfxe87",
                        "normalpassword",
                        "Alfie Whitham",
                        "",
                        "furthereducation",
                        "teacher")
            }
        }
    }

    "Add new user with an empty key_stage should throw IllegalArgumentException" {
        testApplication {
            application { testModule() }
            shouldThrow<IllegalArgumentException> {
                addUser("alfxe87",
                        "normalpassword",
                        "Alfie Whitham",
                        "alfie@gmail.com",
                        "",
                        "teacher")
            }
        }
    }

    "Add new user with an empty role should throw IllegalArgumentException" {
        testApplication {
            application { testModule() }
            shouldThrow<IllegalArgumentException> {
                addUser("alfxe87",
                        "normalpassword",
                        "Alfie Whitham",
                        "alfie@gmail.com",
                        "furthereducation",
                        "")
            }
        }
    }

    "New user should have hashed password of length 109" {
        testApplication {
            application { testModule() }
            val user = addUser("alfxe87",
                                "normalpassword",
                                "Alfie Whitham",
                                "alfie@gmail.com",
                                "furthereducation",
                                "teacher")
            user shouldNotBe null
            if (user != null) {
                val retrievedUser = retrieveUser(user.username)
                retrievedUser shouldNotBe null
                if (retrievedUser != null) {
                    retrievedUser.password shouldContain "\$100801\$"
                    retrievedUser.password shouldHaveLength 109
                }
            }
        }
    }

    // Testing functions used for Quizzes (Questions, Quinstances, Answers)
    "Retrieve all (2) quizzes for user 'linguin0'" {
        testApplication {
            application { testModule() }

            val quizzesData = listOf(
                listOf("COMP2860 Quiz", 2, "Quiz to test my limited knowledge on COMP2860", "computerscience"),
                listOf("FNAF Quiz", 0, "Freddy Fazbear", "other"))

            val foundQuizzes = retrieveUserQuizzes("linguin0")
            foundQuizzes shouldNotBe null

            if (foundQuizzes != null) {
                for (i in 0..1) {
                    foundQuizzes[i].quiz_name shouldBe quizzesData[i][0]
                    foundQuizzes[i].num_of_questions shouldBe quizzesData[i][1]
                    foundQuizzes[i].description shouldBe quizzesData[i][2]
                    foundQuizzes[i].category shouldBe quizzesData[i][3]
                }
            }

        }
    }
})