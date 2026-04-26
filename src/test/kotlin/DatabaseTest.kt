package com.example
import com.example.database.TestDatabase
import com.example.database.objects.Game
import com.example.database.objects.InUseCode
import com.example.database.objects.Question
import com.example.database.objects.Quinstance
import com.example.database.objects.Quiz
import com.example.database.objects.User
import com.example.database.tables.GameTable
import com.example.database.tables.InUseTable
import com.example.database.tables.QuinstanceTable
import com.example.database.tables.QuizTable
import com.example.database.tables.UserTable
import com.example.database.tables.UserTable.email
import com.example.database.tables.UserTable.key_stage
import com.example.database.tables.UserTable.name
import com.example.database.tables.UserTable.password
import com.example.database.tables.UserTable.role
import com.example.database.tables.UserTable.username
import com.password4j.Password
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldHaveLength
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration

class DatabaseTest :
    StringSpec({
        // Testing the functions used for Login / Signup

        "Retrieving user with empty username throws an IllegalArgumentException" {
            testApplication {
                application { testModule() }
                shouldThrow<IllegalArgumentException> {
                    retrieveUser("")
                }
            }
        }

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

        "Add new user 'Emily Williams' with appropriate data" {
            testApplication {
                application { testModule() }
                val user =
                    addUser(
                        "spiritscout",
                        "password111",
                        "Emily Williams",
                        "emily@gmail.com",
                        "furthereducation",
                        "teacher",
                    )
                user shouldNotBe null
            }
        }

        "Add new user 'Alfie Whitham' with a username of length 50 (boundary)" {
            testApplication {
                application { testModule() }
                val user =
                    addUser(
                        "alfiewhithamwithausernamewithlengthfifty0123456789",
                        "normalpassword",
                        "Alfie Whitham",
                        "alfie@gmail.com",
                        "furthereducation",
                        "teacher",
                    )
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
                        "teacher",
                    )
                }
            }
        }

        "Add new user 'Alfie Whitham' with an email of length 50 (boundary)" {
            testApplication {
                application { testModule() }
                val user =
                    addUser(
                        "alfxe87",
                        "normalpassword",
                        "Alfie Whitham",
                        "alfiewhithamhasareallycoolawesomeemail20@gmail.com",
                        "furthereducation",
                        "teacher",
                    )
                user shouldNotBe null
            }
        }

        "Add new user 'Alfie Whitham' with an email of length > 50 should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }
                shouldThrow<IllegalArgumentException> {
                    addUser(
                        "alfxe87",
                        "normalpassword",
                        "Alfie Whitham",
                        "alfiewhithamhasareallycoolawesomeemail2000@gmail.com",
                        "furthereducation",
                        "teacher",
                    )
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
                        "teacher",
                    )
                }
            }
        }

        "Add new user with an empty password should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }
                shouldThrow<IllegalArgumentException> {
                    addUser(
                        "alfxe87",
                        "",
                        "Alfie Whitham",
                        "alfie@gmail.com",
                        "furthereducation",
                        "teacher",
                    )
                }
            }
        }

        "Add new user with an empty name should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }
                shouldThrow<IllegalArgumentException> {
                    addUser(
                        "alfxe87",
                        "normalpassword",
                        "",
                        "alfie@gmail.com",
                        "furthereducation",
                        "teacher",
                    )
                }
            }
        }

        "Add new user with an empty email should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }
                shouldThrow<IllegalArgumentException> {
                    addUser(
                        "alfxe87",
                        "normalpassword",
                        "Alfie Whitham",
                        "",
                        "furthereducation",
                        "teacher",
                    )
                }
            }
        }

        "Add new user with an empty key_stage should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }
                shouldThrow<IllegalArgumentException> {
                    addUser(
                        "alfxe87",
                        "normalpassword",
                        "Alfie Whitham",
                        "alfie@gmail.com",
                        "",
                        "teacher",
                    )
                }
            }
        }

        "Add new user with an empty role should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }
                shouldThrow<IllegalArgumentException> {
                    addUser(
                        "alfxe87",
                        "normalpassword",
                        "Alfie Whitham",
                        "alfie@gmail.com",
                        "furthereducation",
                        "",
                    )
                }
            }
        }

        "New user should have hashed password of length 109" {
            testApplication {
                application { testModule() }
                val user =
                    addUser(
                        "alfxe87",
                        "normalpassword",
                        "Alfie Whitham",
                        "alfie@gmail.com",
                        "furthereducation",
                        "teacher",
                    )
                user shouldNotBe null

                val retrievedUser =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq user!!.username }.firstOrNull()
                    }
                retrievedUser shouldNotBe null
                retrievedUser!!.let {
                    it.password shouldContain "\$100801\$"
                    it.password shouldHaveLength 109
                }
            }
        }

        // Testing functions used for Quizzes (Questions, Quinstances, Answers)
        "Retrieve all (2) quizzes for user 'linguin0'" {
            testApplication {
                application { testModule() }

                val quizzesData =
                    listOf(
                        listOf("COMP2860 Quiz", 2, "Quiz to test my limited knowledge on COMP2860", "computerscience"),
                        listOf("FNAF Quiz", 0, "Freddy Fazbear", "other"),
                    )

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
                foundQuizzes!! shouldHaveSize 0
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

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                    }
                user shouldNotBe null

                val newQuiz =
                    addQuiz(
                        user!!,
                        "My Quiz #1",
                        "This is a test quiz",
                        "other",
                        "furthereducation",
                    )
                newQuiz shouldNotBe null
            }
        }

        "Add a new Quiz with quiz name of length 50" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                    }
                user shouldNotBe null

                val newQuiz =
                    addQuiz(
                        user!!,
                        "My really cool awesome quiz with many characters!!",
                        "This is a test quiz",
                        "other",
                        "furthereducation",
                    )
                newQuiz shouldNotBe null
            }
        }

        "Adding a new Quiz with quiz name of length > 50 should throw an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                    }
                user shouldNotBe null

                shouldThrow<IllegalArgumentException> {
                    addQuiz(
                        user!!,
                        "My really cool awesome quiz with many characters!! Uh oh!!",
                        "This is a test quiz",
                        "other",
                        "furthereducation",
                    )
                }
            }
        }

        "Add a new Quiz with a description of length 256" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                    }
                user shouldNotBe null

                val newQuiz =
                    addQuiz(
                        user!!,
                        "My really cool awesome quiz with many characters!!",
                        "This is a really long, boring, very tedious description for testing purposes. 256 characters may be too little, may be too many, may even be the perfect amount of descriptions! Depends how much you like to type, say, yap, et cetera. I think this is done!!!",
                        "other",
                        "furthereducation",
                    )
                newQuiz shouldNotBe null
                newQuiz.description shouldHaveLength 256
            }
        }

        "Adding a new Quiz with a description of length > 256 should throw an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                    }
                user shouldNotBe null

                shouldThrow<IllegalArgumentException> {
                    addQuiz(
                        user!!,
                        "My Quiz #1",
                        "This is a really long, boring, very tedious description for testing purposes. 256 characters may be too little, may be too many, may even be the perfect amount of descriptions! Depends how much you like to type, say, yap, et cetera. I think this is done!!! Uh oh!",
                        "other",
                        "furthereducation",
                    )
                }
            }
        }

        "Adding a new Quiz with -1 as the number of questions should throw an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                    }
                user shouldNotBe null

                shouldThrow<IllegalArgumentException> {
                    addQuiz(
                        user!!,
                        "My Quiz #1",
                        "This is a test quiz",
                        "other",
                        "furthereducation",
                        num_of_questions = -1,
                    )
                }
            }
        }

        "Add a Question (normal data) to Quiz 'COMP2860 Quiz'" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null
                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                    }

                quiz shouldNotBe null
                quiz!!.num_of_questions shouldBe 1

                val question =
                    addQuestion(
                        quiz,
                        "This is a test question",
                        "furthereducation",
                    )
                question shouldNotBe null
                quiz.num_of_questions shouldBe 2
            }
        }

        "Add a Question to Quiz 'COMP2860 Quiz' with a question of length 256" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null
                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                    }
                quiz shouldNotBe null
                quiz!!.num_of_questions shouldBe 1

                val question =
                    addQuestion(
                        quiz,
                        "This is a test question. I don't know how to make this long to be honest. Hello examiner, how has your day been? Writing tests are very important but do you enjoy writing tests? I think I enjoy it but it can be quite tedious, but is important. What is 2+2?",
                        "furthereducation",
                    )
                question shouldNotBe null
                question.question shouldHaveLength 256
                quiz.num_of_questions shouldBe 2
            }
        }

        "Add a Question to Quiz 'COMP2870' with a question of length > 256" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null
                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                    }
                quiz shouldNotBe null

                shouldThrow<IllegalArgumentException> {
                    addQuestion(
                        quiz!!,
                        "This is a test question. I don't know how to make this long to be honest. Hello examiner, how has your day been? Writing tests are very important but do you enjoy writing tests? I think I enjoy it but it can be quite tedious, but is important. What is 2+2????!",
                        "furthereducation",
                    )
                }
            }
        }

        "Retrieve all questions (2) from Quiz 'COMP2860 Quiz'" {
            testApplication {
                application { testModule() }

                val questionData =
                    listOf(
                        listOf("What is a process?", "furthereducation"),
                        listOf("What is a thread?", "furthereducation"),
                    )

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "linguin0" }.firstOrNull()
                        user shouldNotBe null
                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                    }
                quiz shouldNotBe null
                quiz!!.quiz_name shouldBe "COMP2860 Quiz"

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

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null
                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                    }
                quiz shouldNotBe null
                quiz!!.quiz_name shouldBe "COMP2870 Quiz"

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

                val retrievedQuizzes =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                        user shouldNotBe null

                        Quiz.new {
                            this.user = user!!
                            this.quiz_name = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.key_stage = "furthereducation"
                        }
                        return@transaction Quiz.find { QuizTable.user eq user!!.id }.toList()
                    }
                retrievedQuizzes shouldNotBe null
                retrievedQuizzes shouldHaveSize 1

                val retrievedQuestions = retrieveQuizQuestions(retrievedQuizzes.first())
                retrievedQuestions shouldBe emptyList()
            }
        }

        "Trying to retrieve questions from a non-existing Quiz throws an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val newQuiz =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                        user shouldNotBe null

                        Quiz.new {
                            this.user = user!!
                            this.quiz_name = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.key_stage = "furthereducation"
                        }
                    }
                newQuiz shouldNotBe null

                transaction(TestDatabase.db) { newQuiz.delete() } // To remove quiz from database so it can be invalid.

                shouldThrow<IllegalArgumentException> {
                    retrieveQuizQuestions(newQuiz)
                }
            }
        }

        "Retrieve each Answer for all questions in Quiz 'COMP2860 Quiz'" {
            testApplication {
                application { testModule() }

                val questionAnswerData =
                    listOf(
                        listOf(
                            "A program in execution",
                            "Come to understand or accept something over a period of time",
                            "A summons or writ requiring a person to appear in court",
                            "A program waiting to be executed",
                        ),
                        listOf("A small sequence of programmed instructions", "A piece of yarn or twine", null, null),
                    )

                val questions =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "linguin0" }.firstOrNull()
                        user shouldNotBe null

                        val quiz = Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                        quiz shouldNotBe null

                        quiz!!.quiz_name shouldBe "COMP2860 Quiz"
                        val quinstances = Quinstance.find { QuinstanceTable.quiz eq quiz.id }.toList()
                        return@transaction buildList { for (q in quinstances) add(q.question) }
                    }
                questions shouldNotBe null
                questions shouldHaveSize 2

                for (i in 0..1) {
                    val answer = retrieveQuestionAnswer(questions[i])
                    answer shouldNotBe null

                    answer!!.correct_answer shouldBe questionAnswerData[i][0]
                    answer.option2 shouldBe questionAnswerData[i][1]
                    answer.option3 shouldBe questionAnswerData[i][2]
                    answer.option4 shouldBe questionAnswerData[i][3]
                }
            }
        }

        "Retrieve Answer for question in Quiz 'COMP2870'" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null

                        val quiz = Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                        quiz shouldNotBe null

                        val quinstance = Quinstance.find { QuinstanceTable.id eq quiz!!.id }.firstOrNull()
                        quinstance shouldNotBe null
                        return@transaction quinstance!!.question
                    }
                question shouldNotBe null
                question.question shouldBe "What is a finite state automation?"

                val answer = retrieveQuestionAnswer(question)
                answer shouldNotBe null
                answer!!.correct_answer shouldBe "A model of computation with an extremely limited amount of memory"
                answer.option2 shouldBe "A machine that only runs a finite amount of times"
                answer.option3 shouldBe "I have ran out of fake answers"
                answer.option4 shouldBe null
            }
        }

        "Trying to retrieve an Answer from a Question with (somehow) no Answer returns null" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                        user shouldNotBe null

                        Quiz.new {
                            this.user = user!!
                            this.quiz_name = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.key_stage = "furthereducation"
                        }

                        return@transaction Question.new {
                            this.question = "Test question"
                            this.key_stage = "keystage1"
                        }
                    }
                question shouldNotBe null

                val answer = retrieveQuestionAnswer(question)
                answer shouldBe null
            }
        }

        "Adding an Answer (normal data, first option is correct) to a Question without an existing Answer" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                        user shouldNotBe null

                        Quiz.new {
                            this.user = user!!
                            this.quiz_name = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.key_stage = "furthereducation"
                        }

                        return@transaction Question.new {
                            this.question = "Test question"
                            this.key_stage = "keystage1"
                        }
                    }
                question shouldNotBe null

                val answer =
                    addAnswer(
                        question,
                        "Option 1",
                        "Option 2",
                        "Option 3",
                        "Option 4",
                        0,
                    )
                answer shouldNotBe null
                answer!!.correct_answer shouldBe "Option 1"
                answer.option2 shouldBe "Option 2"
                answer.option3 shouldBe "Option 3"
                answer.option4 shouldBe "Option 4"
            }
        }

        "Add an Answer (normal data, second option is correct) to a Question without an existing Answer" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                        user shouldNotBe null

                        Quiz.new {
                            this.user = user!!
                            this.quiz_name = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.key_stage = "furthereducation"
                        }

                        return@transaction Question.new {
                            this.question = "Test question"
                            this.key_stage = "keystage1"
                        }
                    }
                question shouldNotBe null

                val answer =
                    addAnswer(
                        question,
                        "Option 1",
                        "Option 2",
                        "Option 3",
                        "Option 4",
                        1,
                    )
                answer shouldNotBe null
                answer!!.correct_answer shouldBe "Option 2"
                answer.option2 shouldBe "Option 1"
                answer.option3 shouldBe "Option 3"
                answer.option4 shouldBe "Option 4"
            }
        }

        "Add an Answer (normal data, third option is correct) to a Question without an existing Answer" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                        user shouldNotBe null

                        Quiz.new {
                            this.user = user!!
                            this.quiz_name = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.key_stage = "furthereducation"
                        }

                        return@transaction Question.new {
                            this.question = "Test question"
                            this.key_stage = "keystage1"
                        }
                    }
                question shouldNotBe null

                val answer =
                    addAnswer(
                        question,
                        "Option 1",
                        "Option 2",
                        "Option 3",
                        "Option 4",
                        2,
                    )
                answer shouldNotBe null
                answer!!.correct_answer shouldBe "Option 3"
                answer.option2 shouldBe "Option 1"
                answer.option3 shouldBe "Option 2"
                answer.option4 shouldBe "Option 4"
            }
        }

        "Add an Answer (normal data, fourth option is correct) to a Question without an existing Answer" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.new {
                            this.question = "Test question"
                            this.key_stage = "keystage1"
                        }
                    }
                question shouldNotBe null

                val answer =
                    addAnswer(
                        question,
                        "Option 1",
                        "Option 2",
                        "Option 3",
                        "Option 4",
                        3,
                    )
                answer shouldNotBe null
                answer!!.correct_answer shouldBe "Option 4"
                answer.option2 shouldBe "Option 1"
                answer.option3 shouldBe "Option 2"
                answer.option4 shouldBe "Option 3"
            }
        }

        "Add an Answer with null as the third option" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                        user shouldNotBe null

                        val quiz =
                            Quiz.new {
                                this.user = user!!
                                this.quiz_name = "Test Quiz"
                                this.description = "This is a test quiz"
                                this.category = "other"
                                this.key_stage = "furthereducation"
                            }
                        quiz shouldNotBe null

                        return@transaction Question.new {
                            this.question = "Test question"
                            this.key_stage = "keystage1"
                        }
                    }

                question shouldNotBe null

                val answer =
                    addAnswer(
                        question,
                        "Option 1",
                        "Option 2",
                        null,
                        "Option 4",
                        0,
                    )
                answer shouldNotBe null
                answer!!.correct_answer shouldBe "Option 1"
                answer.option2 shouldBe "Option 2"
                answer.option3 shouldBe null
                answer.option4 shouldBe "Option 4"
            }
        }

        "Add an Answer with null as the fourth option" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                        user shouldNotBe null

                        val quiz =
                            Quiz.new {
                                this.user = user!!
                                this.quiz_name = "Test Quiz"
                                this.description = "This is a test quiz"
                                this.category = "other"
                                this.key_stage = "furthereducation"
                            }
                        quiz shouldNotBe null

                        return@transaction Question.new {
                            this.question = "Test question"
                            this.key_stage = "keystage1"
                        }
                    }
                question shouldNotBe null

                val answer =
                    addAnswer(
                        question,
                        "Option 1",
                        "Option 2",
                        "Option 3",
                        null,
                        0,
                    )
                answer shouldNotBe null
                answer!!.correct_answer shouldBe "Option 1"
                answer.option2 shouldBe "Option 2"
                answer.option3 shouldBe "Option 3"
                answer.option4 shouldBe null
            }
        }

        "Adding an Answer to a Question which already has an Answer returns null" {
            testApplication {
                application { testModule() }

                val questions =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "linguin0" }.firstOrNull()
                        user shouldNotBe null

                        val quizzes = Quiz.find { QuizTable.user eq user!!.id }.toList()
                        quizzes shouldHaveSize 2

                        val quiz = quizzes.firstOrNull()
                        quiz shouldNotBe null

                        val quinstances = Quinstance.find { QuinstanceTable.quiz eq quiz!!.id }.toList()
                        return@transaction buildList {
                            for (q in quinstances) {
                                add(Question.findById(q.question.id))
                            }
                        }
                    }
                questions shouldNotBe null
                questions shouldHaveSize 2

                val answer =
                    addAnswer(
                        questions.first()!!,
                        "Option 1",
                        "Option 2",
                        "Option 3",
                        "Option 4",
                        0,
                    )
                answer shouldBe null
            }
        }

        "Adding an Answer to a Question that doesn't exist should throw an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                        user shouldNotBe null

                        val quiz =
                            Quiz.new {
                                this.user = user!!
                                this.quiz_name = "Test Quiz"
                                this.description = "This is a test quiz"
                                this.category = "other"
                                this.key_stage = "furthereducation"
                            }
                        quiz shouldNotBe null

                        Question.new {
                            this.question = "Test question"
                            this.key_stage = "keystage1"
                        }
                    }
                question shouldNotBe null

                transaction(TestDatabase.db) { question.delete() }

                shouldThrow<IllegalArgumentException> {
                    addAnswer(
                        question,
                        "Option 1",
                        "Option 2",
                        "Option 3",
                        "Option 4",
                        0,
                    )
                }
            }
        }

        "Adding an Answer with an empty option 1 should throw an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null

                        val quiz = Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                        quiz shouldNotBe null

                        return@transaction Quinstance.find { QuinstanceTable.quiz eq quiz!!.id }.firstOrNull()!!.question
                    }

                shouldThrow<IllegalArgumentException> {
                    addAnswer(
                        question,
                        "",
                        "Option 2",
                        "Option 3",
                        "Option 4",
                        0,
                    )
                }
            }
        }

        "Adding an Answer with an empty option 2 should throw an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null

                        val quiz = Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                        quiz shouldNotBe null

                        return@transaction Quinstance.find { QuinstanceTable.quiz eq quiz!!.id }.firstOrNull()!!.question
                    }

                shouldThrow<IllegalArgumentException> {
                    addAnswer(
                        question,
                        "Option 1",
                        "",
                        "Option 3",
                        "Option 4",
                        0,
                    )
                }
            }
        }

        "Adding an Answer with an a negative correctIndex should throw an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null

                        val quiz = Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                        quiz shouldNotBe null

                        return@transaction Quinstance.find { QuinstanceTable.quiz eq quiz!!.id }.firstOrNull()!!.question
                    }

                shouldThrow<IllegalArgumentException> {
                    addAnswer(
                        question,
                        "Option 1",
                        "Option 2",
                        "Option 3",
                        "Option 4",
                        -1,
                    )
                }
            }
        }

        "Retrieve Game using given gameID for Quiz 'COMP2860 Quiz'" {
            testApplication {
                application { testModule() }

                val gameId = 1
                val game = retrieveGame(gameId)

                game shouldNotBe null
                transaction(TestDatabase.db) {
                    game!!.quiz.quiz_name shouldBe "COMP2860 Quiz"
                }
            }
        }

        "Retrieve Game using given gameID for Quiz 'COMP2870 Quiz'" {
            testApplication {
                application { testModule() }

                val gameId = 2
                val game = retrieveGame(gameId)

                game shouldNotBe null
                transaction(TestDatabase.db) {
                    game!!.quiz.quiz_name shouldBe "COMP2870 Quiz"
                }
            }
        }

        "Retrieving a Game with an invalid gameID should return null" {
            testApplication {
                application { testModule() }

                val gameId = 5
                val game = retrieveGame(gameId)

                game shouldBe null
            }
        }

        "Retrieving a Game with a negative gameID should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val gameId = -1
                shouldThrow<IllegalArgumentException> {
                    retrieveGame(gameId)
                }
            }
        }

        "Add a Game with Quiz 'COMP2860' (normal data) to GameTable" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "linguin0" }.firstOrNull()
                        user shouldNotBe null

                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                    }
                quiz shouldNotBe null

                val timeBefore = java.time.LocalDateTime.now()
                val game = addGame(quiz!!)
                game shouldNotBe null

                val convertedTimeStamp = java.time.LocalDateTime.parse(game.timeStamp)

                val timeBetween = Duration.between(timeBefore, convertedTimeStamp).toMillis()
                timeBetween shouldBeInRange 0L..10L
            }
        }

        "Add a Game with Quiz 'COMP2870' (normal data) to GameTable" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null

                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                    }
                quiz shouldNotBe null

                val timeBefore = java.time.LocalDateTime.now()
                val game = addGame(quiz!!)
                game shouldNotBe null

                val convertedTimeStamp = java.time.LocalDateTime.parse(game.timeStamp)

                val timeBetween = Duration.between(timeBefore, convertedTimeStamp).toMillis()
                timeBetween shouldBeInRange 0L..10L
            }
        }

        "Adding a Game with a Quiz that doesn't exist throws an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null

                        Quiz.new {
                            this.user = user!!
                            this.quiz_name = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.key_stage = "furthereducation"
                        }
                    }
                quiz shouldNotBe null
                transaction(TestDatabase.db) { quiz.delete() }

                shouldThrow<IllegalArgumentException> {
                    addGame(quiz)
                }
            }
        }

        "Retrieve room code for Game with Quiz 'COMP2860' (normal data)" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        Game.findById(1)
                    }
                game shouldNotBe null

                val roomCode = retrieveRoomCode(game!!)
                roomCode shouldNotBe null
                roomCode shouldBe "ABCD1234"
            }
        }

        "Retrieve room code for Game with Quiz 'COMP2870' (normal data)" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        Game.findById(2)
                    }
                game shouldNotBe null

                val roomCode = retrieveRoomCode(game!!)
                roomCode shouldNotBe null
                roomCode shouldBe "EFGH5678"
            }
        }

        "Retrieving the room code for a Game that doesn't exist throws IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null

                        val quiz = Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                        quiz shouldNotBe null

                        Game.new {
                            this.quiz = quiz!!
                            this.timeStamp = java.time.LocalDateTime.now().toString()
                        }
                    }
                game shouldNotBe null

                transaction(TestDatabase.db) { game.delete() }

                shouldThrow<IllegalArgumentException> {
                    retrieveRoomCode(game)
                }
            }
        }

        "Retrieve InUseCode object from InUseTable using room code 'ABCD1234' (normal data)" {
            testApplication {
                application { testModule() }

                val roomCode = "ABCD1234"

                val inUseCode = retrieveInUseCode(roomCode)
                inUseCode shouldNotBe null

                transaction(TestDatabase.db) {
                    inUseCode!!.game.id.toString() shouldBe "1"
                    inUseCode.game.quiz.quiz_name shouldBe "COMP2860 Quiz"
                }
            }
        }

        "Retrieve InUseCode object from InUseTable using room code 'EFGH5678' (normal data)" {
            testApplication {
                application { testModule() }

                val roomCode = "EFGH5678"

                val inUseCode = retrieveInUseCode(roomCode)
                inUseCode shouldNotBe null

                transaction(TestDatabase.db) {
                    inUseCode!!.game.id.toString() shouldBe "2"
                    inUseCode.game.quiz.quiz_name shouldBe "COMP2870 Quiz"
                }
            }
        }

        "Retrieving InUseCode object with empty Room Code throws IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val roomCode = ""

                shouldThrow<IllegalArgumentException> {
                    retrieveInUseCode(roomCode)
                }
            }
        }

        "Retrieving InUseCode object with a Room Code with invalid characters throws IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val roomCode = "#!#!#!#!"

                shouldThrow<IllegalArgumentException> {
                    retrieveInUseCode(roomCode)
                }
            }
        }

        "Retrieving InUseCode object with a Room Code with length < 8 throws IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val roomCode = "1234"

                shouldThrow<IllegalArgumentException> {
                    retrieveInUseCode(roomCode)
                }
            }
        }

        "Retrieving InUseCode object with a Room Code with length > 8 throws IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val roomCode = "ERROR!ERROR!"

                shouldThrow<IllegalArgumentException> {
                    retrieveInUseCode(roomCode)
                }
            }
        }

        "Room code 'ABCD1234' already exists in InUseTable (roomCodeAlreadyExists returns true)" {
            testApplication {
                application { testModule() }

                val check = roomCodeAlreadyExists("ABCD1234")
                check shouldBe true
            }
        }

        "Room code 'EFGH5678' already exists in InUseTable (roomCodeAlreadyExists returns true)" {
            testApplication {
                application { testModule() }

                val check = roomCodeAlreadyExists("EFGH5678")
                check shouldBe true
            }
        }

        "Room code 'DCBA9876' doesn't exist in InUseTable (roomCodeAlreadyExists returns false)" {
            testApplication {
                application { testModule() }

                val check = roomCodeAlreadyExists("DCBA9876")
                check shouldBe false
            }
        }

        "Trying to check if an empty room code exists throws an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                shouldThrow<IllegalArgumentException> {
                    roomCodeAlreadyExists("")
                }
            }
        }

        "Trying to check if a room code with invalid characters exists throws an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                shouldThrow<IllegalArgumentException> {
                    roomCodeAlreadyExists("#!#!#!#!")
                }
            }
        }

        "Trying to check if a room code of length < 8 exists throws an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                shouldThrow<IllegalArgumentException> {
                    roomCodeAlreadyExists("SMALL")
                }
            }
        }

        "Trying to check if a room code of length > 8 exists throws an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                shouldThrow<IllegalArgumentException> {
                    roomCodeAlreadyExists("HUMoNGoUS")
                }
            }
        }

        "Add InUseCode object for Game with Quiz 'COMP2860 Quiz' (normal data)" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val game = Game.findById(1)
                        game shouldNotBe null
                        game!!.quiz.quiz_name shouldBe "COMP2860 Quiz"
                        return@transaction game
                    }

                // Room code is generated using GenerateRoomCode, for tests I will be using placeholders.
                val inUseCode = addInUseCode("HIJK1010", game)
                inUseCode shouldNotBe null
            }
        }

        "Add InUseCode object for Game with Quiz 'COMP2870 Quiz' (normal data)" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val game = Game.findById(2)
                        game shouldNotBe null
                        game!!.quiz.quiz_name shouldBe "COMP2870 Quiz"
                        return@transaction game
                    }

                // Room code is generated using GenerateRoomCode, for tests I will be using placeholders.
                val inUseCode = addInUseCode("LMNO1234", game)
                inUseCode shouldNotBe null
            }
        }

        "Adding InUseCode object with an empty room code throws an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val game = Game.findById(2)
                        game shouldNotBe null
                        game!!.quiz.quiz_name shouldBe "COMP2870 Quiz"
                        return@transaction game
                    }

                shouldThrow<IllegalArgumentException> {
                    addInUseCode("", game)
                }
            }
        }

        "Adding InUseCode object with invalid characters throws an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val game = Game.findById(2)
                        game shouldNotBe null
                        game!!.quiz.quiz_name shouldBe "COMP2870 Quiz"
                        return@transaction game
                    }

                shouldThrow<IllegalArgumentException> {
                    addInUseCode("!#!#!#!#", game)
                }
            }
        }

        "Adding InUseCode object with a non-unique room code throws an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                transaction(TestDatabase.db) {
                    val game = Game.findById(1)
                    game shouldNotBe null
                    game!!.quiz.quiz_name shouldBe "COMP2860 Quiz"

                    InUseCode.new {
                        this.room_code = "DCBA9876"
                        this.game = game
                    }
                }
                val game =
                    transaction(TestDatabase.db) {
                        val game = Game.findById(2)
                        game shouldNotBe null
                        game!!.quiz.quiz_name shouldBe "COMP2870 Quiz"
                        return@transaction game
                    }

                shouldThrow<IllegalArgumentException> {
                    addInUseCode("DCBA9876", game)
                }
            }
        }

        "Adding InUseCode object for a Game that doesn't exist throws IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null

                        val quiz = Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                        quiz shouldNotBe null

                        Game.new {
                            this.quiz = quiz!!
                            this.timeStamp = java.time.LocalDateTime.now().toString()
                        }
                    }
                game shouldNotBe null

                transaction(TestDatabase.db) { game.delete() }

                shouldThrow<IllegalArgumentException> {
                    addInUseCode("LOLHI123", game)
                }
            }
        }

        "Retrieve Game object using InUseCode for Quiz 'COMP2860 Quiz' (normal data)" {
            testApplication {
                application { testModule() }
                val roomCode = "ABCD1234"

                val inUseCode =
                    transaction(TestDatabase.db) {
                        InUseCode.find { InUseTable.room_code eq roomCode }.firstOrNull()
                    }
                inUseCode shouldNotBe null
                inUseCode!!.room_code shouldBe roomCode

                val game = retrieveInUseCodeGame(inUseCode)
                game shouldNotBe null

                transaction(TestDatabase.db) {
                    game!!.quiz.quiz_name shouldBe "COMP2860 Quiz"
                }
            }
        }

        "Retrieve Game object using InUseCode for Quiz 'COMP2870 Quiz' (normal data)" {
            testApplication {
                application { testModule() }
                val roomCode = "EFGH5678"

                val inUseCode =
                    transaction(TestDatabase.db) {
                        InUseCode.find { InUseTable.room_code eq roomCode }.firstOrNull()
                    }
                inUseCode shouldNotBe null
                inUseCode!!.room_code shouldBe roomCode

                val game = retrieveInUseCodeGame(inUseCode)
                game shouldNotBe null

                transaction(TestDatabase.db) {
                    game!!.quiz.quiz_name shouldBe "COMP2870 Quiz"
                }
            }
        }

        "Retrieving Game object using an InUseCode object that doesn't exist should throw an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val inUseCode =
                    transaction(TestDatabase.db) {
                        val game = Game.findById(2)
                        game shouldNotBe null
                        game!!.quiz.quiz_name shouldBe "COMP2870 Quiz"

                        InUseCode.new {
                            this.room_code = "TEST1234"
                            this.game = game
                        }
                    }

                transaction(TestDatabase.db) { inUseCode.delete() }

                shouldThrow<IllegalArgumentException> {
                    retrieveInUseCodeGame(inUseCode)
                }
            }
        }

        "Delete InUseCode object for Game with Quiz 'COMP2860 Quiz'" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val foundGame = Game.findById(1)
                        foundGame shouldNotBe null
                        foundGame!!.quiz.quiz_name shouldBe "COMP2860 Quiz"
                        return@transaction foundGame
                    }

                deleteInUseCode(game)

                transaction(TestDatabase.db) {
                    val deleteCheck = InUseCode.find { InUseTable.game eq game.id}.firstOrNull()
                    deleteCheck shouldBe null
                }
            }
        }

        "Delete InUseCode object for Game with Quiz 'COMP2870 Quiz'" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val foundGame = Game.findById(2)
                        foundGame shouldNotBe null
                        foundGame!!.quiz.quiz_name shouldBe "COMP2870 Quiz"
                        return@transaction foundGame
                    }

                deleteInUseCode(game)

                transaction(TestDatabase.db) {
                    val deleteCheck = InUseCode.find { InUseTable.game eq game.id}.firstOrNull()
                    deleteCheck shouldBe null
                }
            }
        }

        "Deleting InUseCode object for a Game that doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val newGame =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null

                        val quiz = Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                        quiz shouldNotBe null

                        val game =
                            Game.new {
                                this.quiz = quiz!!
                                this.timeStamp = java.time.LocalDateTime.now().toString()
                            }
                        game shouldNotBe null

                        InUseCode.new {
                            this.game = game
                            this.room_code = "TEST1234"
                        }
                        return@transaction game
                    }

                transaction(TestDatabase.db) { newGame.delete() }

                shouldThrow<IllegalArgumentException> {
                    deleteInUseCode(newGame)
                }
            }
        }

        "Retrieve UserScore for User 'linguin0' with Game playing Quiz 'COMP2860'" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { username eq "linguin0" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.findById(1)
                    }
                game shouldNotBe null

                val userScore = retrieveUserScore(game!!, user!!)
                userScore shouldNotBe null
                userScore!!.score shouldBe 250
            }
        }

        "Retrieve null if there is no UserScore associated with a Game playing Quiz 'COMP2860'" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { username eq "JoanaKennah" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.findById(2)
                    }
                game shouldNotBe null

                val userScore = retrieveUserScore(game!!, user!!)
                userScore shouldBe null
            }
        }

        "Retrieving UserScore for a Game that doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { username eq "linguin0" }.firstOrNull()
                    }
                user shouldNotBe null

                val quiz =
                    transaction(TestDatabase.db) {
                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                    }
                quiz shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.new {
                            this.quiz = quiz!!
                            this.timeStamp = java.time.LocalDateTime.now().toString()
                        }
                    }

                transaction(TestDatabase.db) { game.delete() }

                shouldThrow<IllegalArgumentException> {
                    retrieveUserScore(game, user!!)
                }
            }
        }

        "Retrieving UserScore for User who doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val fakeUser =
                    transaction(TestDatabase.db) {
                        val hashedPass = Password.hash("test123").addRandomSalt(8).withScrypt()
                        User.new {
                            this.username = "Test User"
                            this.password = hashedPass.result
                            this.name = "Test Test"
                            this.email = "test@gmail.com"
                            this.key_stage = "furthereducation"
                            this.role = "student"
                        }
                    }
                val game =
                    transaction(TestDatabase.db) {
                        Game.findById(1)
                    }
                game shouldNotBe null

                transaction(TestDatabase.db) { fakeUser.delete() }

                shouldThrow<IllegalArgumentException> {
                    retrieveUserScore(game!!, fakeUser)
                }
            }
        }

        "Retrieve all UserScore objects for a Game playing Quiz 'COMP2860'" {
            testApplication {
                application { testModule() }

                val userScoreData =
                    listOf(
                        Triple(1, "linguin0", 250),
                        Triple(2, "JoanaKennah", 1000),
                        Triple(3, "MinghaoLe123", 500),
                    ).sortedByDescending { it.third }

                val game =
                    transaction(TestDatabase.db) {
                        Game.findById(1)
                    }
                game shouldNotBe null

                val allUserScores = retrieveAllUserScoresDescending(game!!)
                allUserScores shouldHaveSize 3

                for (i in 0..2) {
                    val (id, username, score) = allUserScores[i]
                    id shouldBe userScoreData[i].first
                    username shouldBe userScoreData[i].second
                    score shouldBe userScoreData[i].third
                }
            }
        }
    })
