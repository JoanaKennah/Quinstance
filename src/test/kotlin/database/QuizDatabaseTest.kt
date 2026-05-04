package database

import com.example.addAnswer
import com.example.addQuestion
import com.example.addQuiz
import com.example.database.TestDatabase
import com.example.editQuiz
import com.example.deleteQuiz
import com.example.setAnswerOptions
import com.example.editQuestion
import com.example.database.objects.Answer
import com.example.database.objects.Question
import com.example.database.objects.Quinstance
import com.example.database.objects.Quiz
import com.example.database.objects.User
import com.example.database.tables.AnswerTable
import com.example.database.tables.AnswerTable.option2
import com.example.database.tables.AnswerTable.option3
import com.example.database.tables.AnswerTable.option4
import com.example.database.tables.QuestionTable
import com.example.database.tables.QuinstanceTable
import com.example.database.tables.QuizTable
import com.example.database.tables.UserTable
import com.example.deleteQuestion
import com.example.retrieveQuestionAnswer
import com.example.retrieveQuizQuestions
import com.example.retrieveUserQuizzes
import com.example.testModule
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.collections.buildList

/**
 * Unit tests for Database functions mostly used in QuizRouting.kt
 * @see addQuiz
 * @see editQuiz
 * @see addQuestion
 * @see deleteQuestion
 * @see setAnswerOptions
 * @see addAnswer
 * @see retrieveUserQuizzes
 * @see retrieveQuestionAnswer
 * @see editQuestion
 * @see deleteQuiz
 *
 */
class QuizDatabaseTest :
    StringSpec({
        beforeTest {
            TestDatabase.create()
        }

        // Tests for addQuiz
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
                        numOfQuestions = -1,
                    )
                }
            }
        }

        // Tests for editQuiz() TODO()
        "Edit Quiz 'COMP2860 Quiz' and change the quiz name to 'UoL COMP2860 Quiz'" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.findById(1)
                        user shouldNotBe null

                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull { it.quizName == "COMP2860 Quiz" }
                    }
                quiz shouldNotBe null

                val quizData =
                    mapOf(
                        "quizName" to "UoL COMP2860 Quiz",
                    )

                editQuiz(quiz!!, quizData)

                quiz.quizName shouldBe "UoL COMP2860 Quiz"
                quiz.description shouldBe "Quiz to test my limited knowledge on COMP2860"
                quiz.category shouldBe "computerscience"
                quiz.keyStage shouldBe "furthereducation"
            }
        }

        "Edit Quiz 'COMP2860 Quiz' and change the quiz description to 'Quiz to test my (still) limited knowledge on COMP2860'" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.findById(1)
                        user shouldNotBe null

                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull { it.quizName == "COMP2860 Quiz" }
                    }
                quiz shouldNotBe null

                val quizData =
                    mapOf(
                        "description" to "Quiz to test my (still) limited knowledge on COMP2860",
                    )

                editQuiz(quiz!!, quizData)

                quiz.quizName shouldBe "COMP2860 Quiz"
                quiz.description shouldBe "Quiz to test my (still) limited knowledge on COMP2860"
                quiz.category shouldBe "computerscience"
                quiz.keyStage shouldBe "furthereducation"
            }
        }

        "Edit Quiz 'COMP2860 Quiz' and change the quiz category to 'other'" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.findById(1)
                        user shouldNotBe null

                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull { it.quizName == "COMP2860 Quiz" }
                    }
                quiz shouldNotBe null

                val quizData =
                    mapOf(
                        "category" to "other",
                    )

                editQuiz(quiz!!, quizData)

                quiz.quizName shouldBe "COMP2860 Quiz"
                quiz.description shouldBe "Quiz to test my limited knowledge on COMP2860"
                quiz.category shouldBe "other"
                quiz.keyStage shouldBe "furthereducation"
            }
        }

        "Edit Quiz 'COMP2860 Quiz' and change the quiz key stage to 'other'" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.findById(1)
                        user shouldNotBe null

                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull { it.quizName == "COMP2860 Quiz" }
                    }
                quiz shouldNotBe null

                val editData =
                    mapOf(
                        "keyStage" to "nonapplicable",
                    )

                editQuiz(quiz!!, editData)

                quiz.quizName shouldBe "COMP2860 Quiz"
                quiz.description shouldBe "Quiz to test my limited knowledge on COMP2860"
                quiz.category shouldBe "computerscience"
                quiz.keyStage shouldBe "nonapplicable"
            }
        }

        "Trying to edit Quiz 'COMP2860 Quiz' with an empty quizData map should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.findById(1)
                        user shouldNotBe null

                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull { it.quizName == "COMP2860 Quiz" }
                    }
                quiz shouldNotBe null

                val quizData = emptyMap<String, String>()

                shouldThrow<IllegalArgumentException> {
                    editQuiz(quiz!!, quizData)
                }
            }
        }

        "Trying to edit Quiz 'COMP2860 Quiz' with an invalid key should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.findById(1)
                        user shouldNotBe null

                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull { it.quizName == "COMP2860 Quiz" }
                    }
                quiz shouldNotBe null

                val quizData =
                    mapOf(
                        "quiz name" to quiz!!.quizName,
                    )

                shouldThrow<IllegalArgumentException> {
                    editQuiz(quiz, quizData)
                }
            }
        }

        "Trying to edit a Quiz that doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.findById(1)
                        user shouldNotBe null

                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull { it.quizName == "COMP2860 Quiz" }
                    }
                quiz shouldNotBe null

                transaction(TestDatabase.db) { quiz!!.delete() }

                val quizData =
                    mapOf(
                        "quizName" to "Deleted Quiz!",
                    )

                shouldThrow<IllegalArgumentException> {
                    editQuiz(quiz!!, quizData)
                }
            }
        }

        // Tests for retrieveQuizQuestions()
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
                quiz!!.quizName shouldBe "COMP2860 Quiz"

                val quizQuestions = retrieveQuizQuestions(quiz)
                quizQuestions shouldNotBe null
                quizQuestions.size shouldBe 2

                for (i in 0..1) {
                    quizQuestions[i] shouldNotBe null
                    quizQuestions[i].question shouldBe questionData[i][0]
                    quizQuestions[i].keyStage shouldBe questionData[i][1]
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
                quiz!!.quizName shouldBe "COMP2870 Quiz"

                val quizQuestions = retrieveQuizQuestions(quiz)
                quizQuestions shouldNotBe null
                quizQuestions.size shouldBe 1

                quizQuestions[0].question shouldBe "What is a finite state automation?"
                quizQuestions[0].keyStage shouldBe "furthereducation"
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
                            this.quizName = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.keyStage = "furthereducation"
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
                            this.quizName = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.keyStage = "furthereducation"
                        }
                    }
                newQuiz shouldNotBe null

                transaction(TestDatabase.db) { newQuiz.delete() } // To remove quiz from database so it can be invalid.

                shouldThrow<IllegalArgumentException> {
                    retrieveQuizQuestions(newQuiz)
                }
            }
        }

        // Tests for addQuestion() TODO() if quiz object doesnt exist
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
                quiz!!.numOfQuestions shouldBe 1

                val question =
                    addQuestion(
                        quiz,
                        "This is a test question",
                        "furthereducation",
                    )
                question shouldNotBe null
                quiz.numOfQuestions shouldBe 2
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
                quiz!!.numOfQuestions shouldBe 1

                val question =
                    addQuestion(
                        quiz,
                        "This is a test question. I don't know how to make this long to be honest. Hello examiner, how has your day been? Writing tests are very important but do you enjoy writing tests? I think I enjoy it but it can be quite tedious, but is important. What is 2+2?",
                        "furthereducation",
                    )
                question shouldNotBe null
                question.question shouldHaveLength 256
                quiz.numOfQuestions shouldBe 2
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

        "Add a Question to a Quiz that doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null
                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                    }
                quiz shouldNotBe null

                transaction(TestDatabase.db) { quiz!!.delete() }

                shouldThrow<IllegalArgumentException> {
                    addQuestion(
                        quiz!!,
                        "This is a test question",
                        "furthereducation",
                    )
                }
            }
        }

        // Tests for addAnswer()
        "Adding an Answer (normal data, first option is correct) to a Question without an existing Answer" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                        user shouldNotBe null

                        Quiz.new {
                            this.user = user!!
                            this.quizName = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.keyStage = "furthereducation"
                        }

                        return@transaction Question.new {
                            this.question = "Test question"
                            this.keyStage = "keystage1"
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
                answer!!.correctAnswer shouldBe "Option 1"
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
                            this.quizName = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.keyStage = "furthereducation"
                        }

                        return@transaction Question.new {
                            this.question = "Test question"
                            this.keyStage = "keystage1"
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
                answer!!.correctAnswer shouldBe "Option 2"
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
                            this.quizName = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.keyStage = "furthereducation"
                        }

                        return@transaction Question.new {
                            this.question = "Test question"
                            this.keyStage = "keystage1"
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
                answer!!.correctAnswer shouldBe "Option 3"
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
                            this.keyStage = "keystage1"
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
                answer!!.correctAnswer shouldBe "Option 4"
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
                                this.quizName = "Test Quiz"
                                this.description = "This is a test quiz"
                                this.category = "other"
                                this.keyStage = "furthereducation"
                            }
                        quiz shouldNotBe null

                        return@transaction Question.new {
                            this.question = "Test question"
                            this.keyStage = "keystage1"
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
                answer!!.correctAnswer shouldBe "Option 1"
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
                                this.quizName = "Test Quiz"
                                this.description = "This is a test quiz"
                                this.category = "other"
                                this.keyStage = "furthereducation"
                            }
                        quiz shouldNotBe null

                        return@transaction Question.new {
                            this.question = "Test question"
                            this.keyStage = "keystage1"
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
                answer!!.correctAnswer shouldBe "Option 1"
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
                                this.quizName = "Test Quiz"
                                this.description = "This is a test quiz"
                                this.category = "other"
                                this.keyStage = "furthereducation"
                            }
                        quiz shouldNotBe null

                        Question.new {
                            this.question = "Test question"
                            this.keyStage = "keystage1"
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

                        return@transaction Quinstance.find { QuinstanceTable.quiz eq quiz!!.id }
                            .firstOrNull()!!.question
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

                        return@transaction Quinstance.find { QuinstanceTable.quiz eq quiz!!.id }
                            .firstOrNull()!!.question
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

                        return@transaction Quinstance.find { QuinstanceTable.quiz eq quiz!!.id }
                            .firstOrNull()!!.question
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

        // Tests for deleteQuestion()
        "Delete Question 'What is a process?' from Quiz 'COMP2860 Quiz'" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val quiz = Quiz.find { QuizTable.quiz_name eq "COMP2860 Quiz" }.firstOrNull()
                        quiz shouldNotBe null

                        Quinstance.find { QuinstanceTable.quiz eq quiz!!.id }.firstOrNull()!!.question
                    }
                question shouldNotBe null
                question.question shouldBe "What is a process?"

                val delete = deleteQuestion(question)
                delete shouldBe true
            }
        }

        "Delete Question 'What is a finite state automation?' from Quiz 'COMP2870 Quiz'" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        val quiz = Quiz.find { QuizTable.quiz_name eq "COMP2870 Quiz" }.firstOrNull()
                        quiz shouldNotBe null

                        Quinstance.find { QuinstanceTable.quiz eq quiz!!.id }.firstOrNull()!!.question
                    }
                question shouldNotBe null
                question.question shouldBe "What is a finite state automation?"

                val delete = deleteQuestion(question)
                delete shouldBe true
            }
        }

        "Deleting a question that doesn't exist should return false" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.new {
                            this.question = "This is a test question"
                            this.keyStage = "keystagefour"
                        }
                    }
                question shouldNotBe null

                transaction(TestDatabase.db) { question.delete() }
                deleteQuestion(question) shouldBe false
            }
        }

        // Tests for setAnswerOptions() TODO
        "Set answer options with option 1 being the correct option (all options not null)" {
            testApplication {
                application { testModule() }

                val option1 = "Answer 1"
                val option2 = "Answer 2"
                val option3 = "Answer 3"
                val option4 = "Answer 4"

                val setOptions = setAnswerOptions(option1, option2, option3, option4, 0)
                setOptions shouldHaveSize 4

                setOptions[0] shouldBe option1
                setOptions[1] shouldBe option2
                setOptions[2] shouldBe option3
                setOptions[3] shouldBe option4
            }
        }

        "Set answer options with option 2 being the correct option (all options not null)" {
            testApplication {
                application { testModule() }

                val option1 = "Answer 1"
                val option2 = "Answer 2"
                val option3 = "Answer 3"
                val option4 = "Answer 4"

                val setOptions = setAnswerOptions(option1, option2, option3, option4, 1)
                setOptions shouldHaveSize 4

                setOptions[0] shouldBe option2
                setOptions[1] shouldBe option1
                setOptions[2] shouldBe option3
                setOptions[3] shouldBe option4
            }
        }

        "Set answer options with option 3 being the correct option (all options not null)" {
            testApplication {
                application { testModule() }

                val option1 = "Answer 1"
                val option2 = "Answer 2"
                val option3 = "Answer 3"
                val option4 = "Answer 4"

                val setOptions = setAnswerOptions(option1, option2, option3, option4, 2)
                setOptions shouldHaveSize 4

                setOptions[0] shouldBe option3
                setOptions[1] shouldBe option1
                setOptions[2] shouldBe option2
                setOptions[3] shouldBe option4
            }
        }

        "Set answer options with option 4 being the correct option (all options not null)" {
            testApplication {
                application { testModule() }

                val option1 = "Answer 1"
                val option2 = "Answer 2"
                val option3 = "Answer 3"
                val option4 = "Answer 4"

                val setOptions = setAnswerOptions(option1, option2, option3, option4, 3)
                setOptions shouldHaveSize 4

                setOptions[0] shouldBe option4
                setOptions[1] shouldBe option1
                setOptions[2] shouldBe option2
                setOptions[3] shouldBe option3
            }
        }

        "Setting answer options with option 1 being empty should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val option1 = ""
                val option2 = "Answer 2"
                val option3 = "Answer 3"
                val option4 = "Answer 4"

                shouldThrow<IllegalArgumentException> {
                    setAnswerOptions(option1, option2, option3, option4, 1)
                }
            }
        }

        "Setting answer options with option 2 being empty should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val option1 = "Answer 1"
                val option2 = ""
                val option3 = "Answer 3"
                val option4 = "Answer 4"

                shouldThrow<IllegalArgumentException> {
                    setAnswerOptions(option1, option2, option3, option4, 0)
                }
            }
        }

        "Setting answer options with option 3 being empty and the correct answer should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val option1 = "Answer 1"
                val option2 = "Answer 2"
                val option3 = null
                val option4 = "Answer 4"

                shouldThrow<IllegalArgumentException> {
                    setAnswerOptions(option1, option2, option3, option4, 2)
                }
            }
        }

        "Setting answer options with correctIndex < 0 correct answer should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val option1 = "Answer 1"
                val option2 = "Answer 2"
                val option3 = "Answer 3"
                val option4 = "Answer 4"

                shouldThrow<IllegalArgumentException> {
                    setAnswerOptions(option1, option2, option3, option4, -1)
                }
            }
        }

        "Setting answer options with correctIndex > 3 should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val option1 = "Answer 1"
                val option2 = "Answer 2"
                val option3 = "Answer 3"
                val option4 = "Answer 4"

                shouldThrow<IllegalArgumentException> {
                    setAnswerOptions(option1, option2, option3, option4, 4)
                }
            }
        }

        // Tests for retrieveUserQuizzes()
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
                foundQuizzes!! shouldHaveSize 2

                for (i in 0..1) {
                    foundQuizzes[i].quizName shouldBe quizzesData[i][0]
                    foundQuizzes[i].numOfQuestions shouldBe quizzesData[i][1]
                    foundQuizzes[i].description shouldBe quizzesData[i][2]
                    foundQuizzes[i].category shouldBe quizzesData[i][3]
                }
            }
        }

        "Retrieve all (1) quizzes for user 'JoanaKennah'" {
            testApplication {
                application { testModule() }

                val foundQuizzes = retrieveUserQuizzes("JoanaKennah")
                foundQuizzes shouldNotBe null
                foundQuizzes!! shouldHaveSize 1

                foundQuizzes[0].quizName shouldBe "COMP2870 Quiz"
                foundQuizzes[0].numOfQuestions shouldBe 1
                foundQuizzes[0].description shouldBe "University of Leeds COMP2870 Quiz for Revision :)"
                foundQuizzes[0].category shouldBe "computerscience"
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

        // Tests for retrieveQuestionAnswer()
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

                        quiz!!.quizName shouldBe "COMP2860 Quiz"
                        val quinstances = Quinstance.find { QuinstanceTable.quiz eq quiz.id }.toList()
                        return@transaction buildList { for (q in quinstances) add(q.question) }
                    }
                questions shouldNotBe null
                questions shouldHaveSize 2

                for (i in 0..1) {
                    val answer = retrieveQuestionAnswer(questions[i])
                    answer shouldNotBe null

                    answer!!.correctAnswer shouldBe questionAnswerData[i][0]
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
                answer!!.correctAnswer shouldBe "A model of computation with an extremely limited amount of memory"
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
                            this.quizName = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.keyStage = "furthereducation"
                        }

                        return@transaction Question.new {
                            this.question = "Test question"
                            this.keyStage = "keystage1"
                        }
                    }
                question shouldNotBe null

                val answer = retrieveQuestionAnswer(question)
                answer shouldBe null
            }
        }

        // Tests for editQuestion() TODO
        "Edit Question 'What is a process?' to change the question text to 'Why is a process?'" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.find { QuestionTable.question eq "What is a process?" }.firstOrNull()
                    }
                question shouldNotBe null

                val questionData =
                    mapOf(
                        "question" to "Why is a process?",
                    )

                editQuestion(question!!, questionData, null,emptyMap<String, String>())

                question.question shouldBe "Why is a process?"
                question.keyStage shouldBe "furthereducation"
            }
        }

        "Edit Question 'What is a process?' to change the question key stage to 'other'" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.find { QuestionTable.question eq "What is a process?" }.firstOrNull()
                    }
                question shouldNotBe null

                val questionData =
                    mapOf(
                        "keyStage" to "other",
                    )

                editQuestion(question!!, questionData, null,emptyMap<String, String>())

                question.question shouldBe "What is a process?"
                question.keyStage shouldBe "other"
            }
        }

        "Editing Question that doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.find { QuestionTable.question eq "What is a process?" }.firstOrNull()
                    }
                question shouldNotBe null
                transaction(TestDatabase.db) { question!!.delete() }

                val questionData =
                    mapOf(
                        "keyStage" to "other",
                    )

                shouldThrow<IllegalArgumentException> {
                    editQuestion(question!!, questionData, null,emptyMap<String, String>())
                }
            }
        }

        "Edit Question 'What is a process?' to change it's Answer option1 to 'This is an edit'" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.find { QuestionTable.question eq "What is a process?" }.firstOrNull()
                    }
                question shouldNotBe null

                val answer =
                    transaction(TestDatabase.db) {
                        Answer.find { AnswerTable.question eq question!!.id }.firstOrNull()
                    }
                answer shouldNotBe null

                val answerData =
                    mapOf(
                        "option1" to "This is an edit",
                        "option2" to "Come to understand or accept something over a period of time",
                        "option3" to "A summons or writ requiring a person to appear in court",
                        "option4" to "A program waiting to be executed",
                        "correctIndex" to "0",
                    )

                editQuestion(question!!, emptyMap<String, String>(), answer, answerData)

                answer!!.correctAnswer shouldBe "This is an edit"
                answer.option2 shouldBe "Come to understand or accept something over a period of time"
                answer.option3 shouldBe "A summons or writ requiring a person to appear in court"
                answer.option4 shouldBe "A program waiting to be executed"
            }
        }

        "Edit Question 'What is a process?' to change it's Answer option2 to 'This is an edit'" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.find { QuestionTable.question eq "What is a process?" }.firstOrNull()
                    }
                question shouldNotBe null

                val answer =
                    transaction(TestDatabase.db) {
                        Answer.find { AnswerTable.question eq question!!.id }.firstOrNull()
                    }
                answer shouldNotBe null

                val answerData =
                    mapOf(
                        "option1" to "A program in execution",
                        "option2" to "This is an edit",
                        "option3" to "A summons or writ requiring a person to appear in court",
                        "option4" to "A program waiting to be executed",
                        "correctIndex" to "1",
                    )

                editQuestion(question!!, emptyMap<String, String>(), answer, answerData)

                answer!!.correctAnswer shouldBe "This is an edit"
                answer.option2 shouldBe "A program in execution"
                answer.option3 shouldBe "A summons or writ requiring a person to appear in court"
                answer.option4 shouldBe "A program waiting to be executed"
            }
        }

        "Edit Question 'What is a process?' to change it's Answer option3 to 'This is an edit'" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.find { QuestionTable.question eq "What is a process?" }.firstOrNull()
                    }
                question shouldNotBe null

                val answer =
                    transaction(TestDatabase.db) {
                        Answer.find { AnswerTable.question eq question!!.id }.firstOrNull()
                    }
                answer shouldNotBe null

                val answerData =
                    mapOf(
                        "option1" to "A program in execution",
                        "option2" to "Come to understand or accept something over a period of time",
                        "option3" to "This is an edit",
                        "option4" to "A program waiting to be executed",
                        "correctIndex" to "2",
                    )

                editQuestion(question!!, emptyMap<String, String>(), answer, answerData)

                answer!!.correctAnswer shouldBe "This is an edit"
                answer.option2 shouldBe "A program in execution"
                answer.option3 shouldBe "Come to understand or accept something over a period of time"
                answer.option4 shouldBe "A program waiting to be executed"
            }
        }

        "Edit Question 'What is a process?' to change it's Answer option4 to 'This is an edit'" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.find { QuestionTable.question eq "What is a process?" }.firstOrNull()
                    }
                question shouldNotBe null

                val answer =
                    transaction(TestDatabase.db) {
                        Answer.find { AnswerTable.question eq question!!.id }.firstOrNull()
                    }
                answer shouldNotBe null

                val answerData =
                    mapOf(
                        "option1" to "A program in execution",
                        "option2" to "Come to understand or accept something over a period of time",
                        "option3" to "A summons or writ requiring a person to appear in court",
                        "option4" to "This is an edit",
                        "correctIndex" to "3",
                    )

                editQuestion(question!!, emptyMap<String, String>(), answer, answerData)

                answer!!.correctAnswer shouldBe "This is an edit"
                answer.option2 shouldBe "A program in execution"
                answer.option3 shouldBe "Come to understand or accept something over a period of time"
                answer.option4 shouldBe "A summons or writ requiring a person to appear in court"
            }
        }

        "Editing Question 'What is a process?' with a correctIndex that isn't a number should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.find { QuestionTable.question eq "What is a process?" }.firstOrNull()
                    }
                question shouldNotBe null

                val answer =
                    transaction(TestDatabase.db) {
                        Answer.find { AnswerTable.question eq question!!.id }.firstOrNull()
                    }
                answer shouldNotBe null

                val answerData =
                    mapOf(
                        "option1" to "A program in execution",
                        "option2" to "Come to understand or accept something over a period of time",
                        "option3" to "A summons or writ requiring a person to appear in court",
                        "option4" to "This is an edit",
                        "correctIndex" to "f",
                    )

                shouldThrow<IllegalArgumentException> {
                    editQuestion(question!!, emptyMap<String, String>(), answer, answerData)
                }
            }
        }

        "Editing a Question with questionData and answerData being empty should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.find { QuestionTable.question eq "What is a process?" }.firstOrNull()
                    }
                question shouldNotBe null

                val questionData = emptyMap<String, String>()
                val answerData = emptyMap<String, String?>()


                shouldThrow<IllegalArgumentException> {
                    editQuestion(question!!, questionData, null, answerData)
                }
            }
        }

        "Editing a Question that doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.find { QuestionTable.question eq "What is a process?" }.firstOrNull()
                    }
                question shouldNotBe null

                transaction(TestDatabase.db) { question!!.delete() }

                val questionData =
                    mapOf(
                        "question" to "Why is a process?",
                    )

                shouldThrow<IllegalArgumentException> {
                    editQuestion(question!!, questionData, null,emptyMap<String, String>())
                }
            }
        }

        "Editing a Question whose Answer doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.find { QuestionTable.question eq "What is a process?" }.firstOrNull()
                    }
                question shouldNotBe null

                val answer =
                    transaction(TestDatabase.db) {
                        Answer.find { AnswerTable.question eq question!!.id }.firstOrNull()
                    }
                answer shouldNotBe null

                transaction(TestDatabase.db) { answer!!.delete() }

                val questionData =
                    mapOf(
                        "question" to "Why is a process?",
                    )

                val answerData =
                    mapOf(
                        "option1" to "A program in execution",
                        "option2" to "Come to understand or accept something over a period of time",
                        "option3" to "A summons or writ requiring a person to appear in court",
                        "option4" to "This is an edit",
                        "correctIndex" to "3",
                    )

                shouldThrow<IllegalArgumentException> {
                    editQuestion(question!!, questionData, answer, answerData)
                }
            }
        }

        "Editing a Question with an different Question's Answer should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val question =
                    transaction(TestDatabase.db) {
                        Question.find { QuestionTable.question eq "What is a process?" }.firstOrNull()
                    }
                question shouldNotBe null

                val answer =
                    transaction(TestDatabase.db) {
                        Answer.find { AnswerTable.question eq 2 }.firstOrNull()
                    }
                answer shouldNotBe null

                val answerData =
                    mapOf(
                        "option1" to "A program in execution",
                        "option2" to "Come to understand or accept something over a period of time",
                        "option3" to "A summons or writ requiring a person to appear in court",
                        "option4" to "This is an edit",
                        "correctIndex" to "3",
                    )

                shouldThrow<IllegalArgumentException> {
                    editQuestion(question!!, emptyMap(), answer, answerData)
                }
            }
        }

        // Tests for deleteQuiz() TODO
        "Delete Quiz 'COMP2860 Quiz'" {
            val quiz =
                transaction(TestDatabase.db) {
                    Quiz.find { QuizTable.id eq 1 }.firstOrNull()
                }
            quiz shouldNotBe null

            deleteQuiz(quiz!!) shouldBe true

            val checkQuiz = transaction(TestDatabase.db) {
                Quiz.find { QuizTable.id eq 1 }.firstOrNull()
            }
            checkQuiz shouldBe null
        }

        "Deleting Quiz that has already been deleted should return False" {
            val quiz =
                transaction(TestDatabase.db) {
                    Quiz.find { QuizTable.id eq 1 }.firstOrNull()
                }
            quiz shouldNotBe null

            deleteQuiz(quiz!!) shouldBe true
            deleteQuiz(quiz) shouldBe false
        }

        "Deleting Quiz that doesn't exist should return False" {
            val quiz =
                transaction(TestDatabase.db) {
                    Quiz.find { QuizTable.id eq 1 }.firstOrNull()
                }
            quiz shouldNotBe null

            transaction(TestDatabase.db) { quiz!!.delete() }

            deleteQuiz(quiz!!) shouldBe false
        }
    })
