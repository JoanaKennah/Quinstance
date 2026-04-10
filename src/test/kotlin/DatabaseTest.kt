package com.example

import com.example.database.TestDatabase
import com.example.database.objects.Quiz
import com.example.database.tables.QuizTable
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.core.eq
import io.kotest.core.spec.style.StringSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.*
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.server.testing.*
import com.password4j.Password
import io.kotest.matchers.collections.shouldHaveSize
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
                foundQuizzes shouldHaveSize 2

                for (i in 0..1) {
                    foundQuizzes[i].quiz_name shouldBe quizzesData[i][0]
                    foundQuizzes[i].num_of_questions shouldBe quizzesData[i][1]
                    foundQuizzes[i].description shouldBe quizzesData[i][2]
                    foundQuizzes[i].category shouldBe quizzesData[i][3]
                }
            }
        }
    }

    "Retrieve all (1) quizzes for user 'JoanaKennah'" {
        testApplication {
            application { testModule() }

            val foundQuizzes = retrieveUserQuizzes("JoanaKennah")
            foundQuizzes shouldNotBe null

            if (foundQuizzes != null) {
                foundQuizzes shouldHaveSize 1

                foundQuizzes[0].quiz_name shouldBe "COMP2870 Quiz"
                foundQuizzes[0].num_of_questions shouldBe 1
                foundQuizzes[0].description shouldBe "University of Leeds COMP2870 Quiz for Revision :)"
                foundQuizzes[0].category shouldBe "computerscience"
            }
        }
    }

    "Retrieve empty list of quizzes for user 'MinghaoLe123' who has not made a quiz yet" {
        testApplication {
            application { testModule() }

            val foundQuizzes = retrieveUserQuizzes("MinghaoLe123")
            foundQuizzes shouldNotBe null
            if (foundQuizzes != null) foundQuizzes shouldHaveSize 0
        }
    }

    "Return null when trying to find quizzes made by a user who does not exist" {
        testApplication {
            application { testModule() }

            val foundQuizzes = retrieveUserQuizzes("FNAFLover123")
            foundQuizzes shouldBe null
        }
    }

    "Add a new Quiz (normal data) under user 'MinghaoLe123'" {
        testApplication {
            application { testModule() }

            val user = retrieveUser("MinghaoLe123")
            if (user != null) {
                val newQuiz = addQuiz(user,
                    "My Quiz #1",
                    "This is a test quiz",
                    "other",
                    0)
                newQuiz shouldNotBe null
            }
        }
    }

    "Add a new Quiz with quiz name of length 50" {
        testApplication {
            application { testModule() }

            val user = retrieveUser("MinghaoLe123")
            if (user != null) {
                val newQuiz = addQuiz(user,
                    "My really cool awesome quiz with many characters!!",
                    "This is a test quiz",
                    "other",
                    0)
                newQuiz shouldNotBe null
            }
        }
    }

    "Adding a new Quiz with quiz name of length > 50 should throw an IllegalArgumentException" {
        testApplication {
            application { testModule() }

            val user = retrieveUser("MinghaoLe123")
            if (user != null) {
                shouldThrow<IllegalArgumentException> {
                    addQuiz(user,
                        "My really cool awesome quiz with many characters!! Uh oh!!",
                        "This is a test quiz",
                        "other",
                        0)
                }
            }
        }
    }

    "Add a new Quiz with a description of length 256" {
        testApplication {
            application { testModule() }

            val user = retrieveUser("MinghaoLe123")
            if (user != null) {
                val newQuiz = addQuiz(user,
                    "My really cool awesome quiz with many characters!!",
                    "This is a really long, boring, very tedious description for testing purposes. 256 characters may be too little, may be too many, may even be the perfect amount of descriptions! Depends how much you like to type, say, yap, et cetera. I think this is done!!!",
                    "other",
                    0)
                newQuiz shouldNotBe null
                newQuiz.description shouldHaveLength 256
            }
        }
    }

    "Adding a new Quiz with a description of length > 256 should throw an IllegalArgumentException" {
        testApplication {
            application { testModule() }

            val user = retrieveUser("MinghaoLe123")
            if (user != null) {
                shouldThrow<IllegalArgumentException> {
                    addQuiz(user,
                        "My Quiz #1",
                        "This is a really long, boring, very tedious description for testing purposes. 256 characters may be too little, may be too many, may even be the perfect amount of descriptions! Depends how much you like to type, say, yap, et cetera. I think this is done!!! Uh oh!",
                        "other",
                        0)
                }
            }
        }
    }

    "Adding a new Quiz with -1 as the number of questions should throw an IllegalArgumentException" {
        testApplication {
            application { testModule() }

            val user = retrieveUser("MinghaoLe123")
            if (user != null) {
                shouldThrow<IllegalArgumentException> {
                    addQuiz(user,
                        "My Quiz #1",
                        "This is a test quiz",
                        "other",
                        -1)
                }
            }
        }
    }

    "Add a Question (normal data) to Quiz 'COMP2860 Quiz'" {
        testApplication {
            application { testModule() }

            // Assumes Minghao only has one quiz. This may not be appropriate for a test tho
            val retrievedQuizzes = retrieveUserQuizzes("JoanaKennah")
            retrievedQuizzes shouldNotBe null

            val quiz = retrievedQuizzes!!.first()
            quiz shouldNotBe null
            quiz.num_of_questions shouldBe 1

            val question = addQuestion(quiz,
                "This is a test question",
                "furthereducation")
            question shouldNotBe null

            quiz.num_of_questions shouldBe 2
        }
    }

    "Add a Question to Quiz 'COMP2860 Quiz' with a question of length 256" {
        testApplication {
            application { testModule() }

            // Assumes Minghao only has one quiz. This may not be appropriate for a test tho
            val retrievedQuizzes = retrieveUserQuizzes("JoanaKennah")
            retrievedQuizzes shouldNotBe null

            val quiz = retrievedQuizzes!!.first()
            quiz shouldNotBe null
            quiz.num_of_questions shouldBe 1

            val question = addQuestion(quiz,
                "This is a test question. I don't know how to make this long to be honest. Hello examiner, how has your day been? Writing tests are very important but do you enjoy writing tests? I think I enjoy it but it can be quite tedious, but is important. What is 2+2?",
                "furthereducation")
            question shouldNotBe null
            question.question shouldHaveLength 256
            quiz.num_of_questions shouldBe 2
        }
    }

    "Add a Question to Quiz 'COMP2870' with a question of length > 256" {
        testApplication {
            application { testModule() }

            val retrievedQuizzes = retrieveUserQuizzes("JoanaKennah")
            retrievedQuizzes shouldNotBe null

            val quiz = retrievedQuizzes!!.first()
            quiz shouldNotBe null

            shouldThrow<IllegalArgumentException> {
                addQuestion(
                    quiz,
                    "This is a test question. I don't know how to make this long to be honest. Hello examiner, how has your day been? Writing tests are very important but do you enjoy writing tests? I think I enjoy it but it can be quite tedious, but is important. What is 2+2????!",
                    "furthereducation")
            }
        }
    }

    "Retrieve all questions (2) from Quiz 'COMP2860 Quiz'" {
        testApplication {
            application { testModule() }

            val questionData = listOf(
                listOf("What is a process?", "furthereducation"),
                listOf("What is a thread?", "furthereducation"))

            val retrievedQuizzes = retrieveUserQuizzes("linguin0")
            retrievedQuizzes shouldNotBe null

            val quiz = retrievedQuizzes!!.first()
            quiz shouldNotBe null
            quiz.quiz_name shouldBe "COMP2860 Quiz"

            val quizQuestions = retrieveQuizQuestions(quiz)
            quizQuestions shouldNotBe null
            quizQuestions.size shouldBe 2

            for (i in 0..1) {
                quizQuestions[i] shouldNotBe null
                quizQuestions[i].question shouldBe questionData[i][0]
                quizQuestions[i].key_stage shouldBe questionData[i][1]
            }
        }
    }

    "Retrieve all questions (1) from Quiz 'COMP2870 Quiz'" {
        testApplication {
            application { testModule() }

            val retrievedQuizzes = retrieveUserQuizzes("JoanaKennah")
            retrievedQuizzes shouldNotBe null

            val quiz = retrievedQuizzes!!.first()
            quiz shouldNotBe null
            quiz.quiz_name shouldBe "COMP2870 Quiz"

            val quizQuestions = retrieveQuizQuestions(quiz)
            quizQuestions shouldNotBe null
            quizQuestions.size shouldBe 1

            quizQuestions[0].question shouldBe "What is a finite state automation?"
            quizQuestions[0].key_stage shouldBe "furthereducation"
        }
    }

    "Retrieve empty list of questions from new Quiz 'Test Quiz'" {
        testApplication {
            application { testModule() }

            val user = retrieveUser("MinghaoLe123")
            if (user != null) {
                val newQuiz = addQuiz(user,
                    "Test Quiz",
                    "This is a test quiz",
                    "other",
                    0)
                newQuiz shouldNotBe null

                val retrievedQuizzes = retrieveUserQuizzes("MinghaoLe123")
                retrievedQuizzes shouldNotBe null

                val retrievedQuestions = retrieveQuizQuestions(newQuiz)
                retrievedQuestions shouldBe emptyList()
            }
        }
    }
})