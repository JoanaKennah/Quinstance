package database

import com.example.database.TestDatabase
import com.example.database.objects.Game
import com.example.database.objects.InUseCode
import com.example.database.objects.Quiz
import com.example.database.objects.User
import com.example.database.tables.InUseTable
import com.example.database.tables.QuizTable
import com.example.database.tables.UserTable
import com.example.deleteInUseCode
import com.example.retrieveAllUserScoresDescending
import com.example.retrieveGame
import com.example.retrieveQuizFromGameID
import com.example.retrieveUserScore
import com.example.testModule
import com.password4j.Password
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime

/**
 * Unit tests for Database functions primarily used in GameRouting.kt.
 * @see retrieveGame
 * @see retrieveQuizFromGameID
 * @see retrieveUserScore
 * @see deleteInUseCode
 * @see retrieveAllUserScoresDescending
 */
class GameDatabaseTest :
    StringSpec({
        beforeTest {
            TestDatabase.create()
        }

        // Tests for retrieveGame()
        "Retrieve Game using given gameID for Quiz 'COMP2860 Quiz'" {
            testApplication {
                application { testModule() }

                val gameId = 1
                val game = retrieveGame(gameId)

                game shouldNotBe null
                transaction(TestDatabase.db) {
                    game!!.quiz.quizName shouldBe "COMP2860 Quiz"
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
                    game!!.quiz.quizName shouldBe "COMP2870 Quiz"
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

        "Retrieving a Game with a gameID of 0 should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val gameId = 0
                shouldThrow<IllegalArgumentException> {
                    retrieveGame(gameId)
                }
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

        // Tests for retrieveQuizFromGameID()
        "Retrieve Quiz 'COMP2860 Quiz' using an associated Game ID (1)" {
            testApplication {
                application { testModule() }

                val gameId = 1
                val quiz = retrieveQuizFromGameID(gameId)

                quiz shouldNotBe null
                quiz!!.quizName shouldBe "COMP2860 Quiz"
                transaction(TestDatabase.db) { quiz.user.username shouldBe "linguin0" }
            }
        }

        "Retrieve Quiz 'COMP2870 Quiz' using an associated Game ID (2)" {
            testApplication {
                application { testModule() }

                val gameId = 2
                val quiz = retrieveQuizFromGameID(gameId)

                quiz shouldNotBe null
                quiz!!.quizName shouldBe "COMP2870 Quiz"
                transaction(TestDatabase.db) { quiz.user.username shouldBe "JoanaKennah" }
            }
        }

        "Retrieving a Quiz using a Game ID that doesn't exist should return null" {
            testApplication {
                application { testModule() }

                val gameId = 10
                val quiz = retrieveQuizFromGameID(gameId)
                quiz shouldBe null
            }
        }

        "Retrieving a Quiz using a Game ID of 0 show throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val gameId = 0
                shouldThrow<IllegalArgumentException> {
                    retrieveQuizFromGameID(gameId)
                }
            }
        }

        "Retrieving a Quiz using a negative Game ID show throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val gameId = -1
                shouldThrow<IllegalArgumentException> {
                    retrieveQuizFromGameID(gameId)
                }
            }
        }

        // Tests for retrieveUserScore()
        "Retrieve UserScore for User 'MinghaoLe123' with Game playing Quiz 'COMP2860'" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.Companion.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.Companion.findById(1)
                    }
                game shouldNotBe null

                val userScore = retrieveUserScore(game!!, user!!)
                userScore shouldNotBe null
                userScore!!.score shouldBe 500
            }
        }

        "Retrieve null if there is no UserScore associated with a Game playing Quiz 'COMP2860'" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.Companion.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.Companion.findById(2)
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
                        User.Companion.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                    }
                user shouldNotBe null

                val quiz =
                    transaction(TestDatabase.db) {
                        Quiz.Companion.find { QuizTable.user eq user!!.id }.firstOrNull()
                    }
                quiz shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.Companion.new {
                            this.quiz = quiz!!
                            this.timeStamp =
                                LocalDateTime
                                    .now()
                                    .toString()
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
                        User.Companion.new {
                            this.username = "Test User"
                            this.password = hashedPass.result
                            this.name = "Test Test"
                            this.email = "test@gmail.com"
                            this.keyStage = "furthereducation"
                            this.role = "student"
                        }
                    }
                val game =
                    transaction(TestDatabase.db) {
                        Game.Companion.findById(1)
                    }
                game shouldNotBe null

                transaction(TestDatabase.db) { fakeUser.delete() }

                shouldThrow<IllegalArgumentException> {
                    retrieveUserScore(game!!, fakeUser)
                }
            }
        }

        // Tests for deleteInUseCode()
        "Delete InUseCode object for Game with Quiz 'COMP2860 Quiz'" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val foundGame = Game.Companion.findById(1)
                        foundGame shouldNotBe null
                        foundGame!!.quiz.quizName shouldBe "COMP2860 Quiz"
                        return@transaction foundGame
                    }

                deleteInUseCode(game)

                transaction(TestDatabase.db) {
                    val deleteCheck = InUseCode.Companion.find { InUseTable.game eq game.id }.firstOrNull()
                    deleteCheck shouldBe null
                }
            }
        }

        "Delete InUseCode object for Game with Quiz 'COMP2870 Quiz'" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val foundGame = Game.Companion.findById(2)
                        foundGame shouldNotBe null
                        foundGame!!.quiz.quizName shouldBe "COMP2870 Quiz"
                        return@transaction foundGame
                    }

                deleteInUseCode(game)

                transaction(TestDatabase.db) {
                    val deleteCheck = InUseCode.Companion.find { InUseTable.game eq game.id }.firstOrNull()
                    deleteCheck shouldBe null
                }
            }
        }

        "Deleting InUseCode object for a Game that doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val newGame =
                    transaction(TestDatabase.db) {
                        val user = User.Companion.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null

                        val quiz = Quiz.Companion.find { QuizTable.user eq user!!.id }.firstOrNull()
                        quiz shouldNotBe null

                        val game =
                            Game.Companion.new {
                                this.quiz = quiz!!
                                this.timeStamp =
                                    LocalDateTime
                                        .now()
                                        .toString()
                            }
                        game shouldNotBe null

                        InUseCode.Companion.new {
                            this.game = game
                            this.roomCode = "TEST1234"
                        }
                        return@transaction game
                    }

                transaction(TestDatabase.db) { newGame.delete() }

                shouldThrow<IllegalArgumentException> {
                    deleteInUseCode(newGame)
                }
            }
        }

        // Tests for retrieveAllUserScoresDescending()
        "Retrieve all UserScore objects for a Game playing Quiz 'COMP2860'" {
            testApplication {
                application { testModule() }

                val userScoreData =
                    listOf(
                        Triple(2, "JoanaKennah", 1000),
                        Triple(3, "MinghaoLe123", 500),
                    ).sortedByDescending { it.third }

                val game =
                    transaction(TestDatabase.db) {
                        Game.Companion.findById(1)
                    }
                game shouldNotBe null

                val allUserScores = retrieveAllUserScoresDescending(game!!)
                allUserScores shouldHaveSize 2

                for (i in 0..1) {
                    val (id, username, score) = allUserScores[i]
                    id shouldBe userScoreData[i].first
                    username shouldBe userScoreData[i].second
                    score shouldBe userScoreData[i].third
                }
            }
        }

        "Retrieve empty list if there are no user scores for a Game" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.Companion.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                    }
                user shouldNotBe null

                val quiz =
                    transaction(TestDatabase.db) {
                        Quiz.Companion.find { QuizTable.user eq user!!.id }.firstOrNull()
                    }
                quiz shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.Companion.new {
                            this.quiz = quiz!!
                            this.timeStamp =
                                LocalDateTime
                                    .now()
                                    .toString()
                        }
                    }

                val userScores = retrieveAllUserScoresDescending(game)
                userScores shouldHaveSize 0
            }
        }

        "Retrieving UserScores for a Game that doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.Companion.find { UserTable.username eq "linguin0" }.firstOrNull()
                    }
                user shouldNotBe null

                val quiz =
                    transaction(TestDatabase.db) {
                        Quiz.Companion.find { QuizTable.user eq user!!.id }.firstOrNull()
                    }
                quiz shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.Companion.new {
                            this.quiz = quiz!!
                            this.timeStamp =
                                LocalDateTime
                                    .now()
                                    .toString()
                        }
                    }

                transaction(TestDatabase.db) { game.delete() }

                shouldThrow<IllegalArgumentException> {
                    retrieveAllUserScoresDescending(game)
                }
            }
        }
    })
