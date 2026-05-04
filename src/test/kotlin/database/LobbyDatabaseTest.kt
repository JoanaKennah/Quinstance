package database

import com.example.addGame
import com.example.addInUseCode
import com.example.addUserScore
import com.example.database.TestDatabase
import com.example.database.objects.Game
import com.example.database.objects.InUseCode
import com.example.database.objects.Quiz
import com.example.database.objects.User
import com.example.database.objects.UserScore
import com.example.database.tables.GameTable
import com.example.database.tables.InUseTable
import com.example.database.tables.QuizTable
import com.example.database.tables.UserScoreTable
import com.example.database.tables.UserTable
import com.example.deleteGameUserScores
import com.example.deleteUserScoreForGivenUser
import com.example.retrieveGamePlayers
import com.example.retrieveInUseCode
import com.example.retrieveInUseCodeGame
import com.example.retrieveQuiz
import com.example.roomCodeAlreadyExists
import com.example.testModule
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
import java.time.LocalDateTime

/**
 * Unit tests for Database functions used in LobbyRouting.
 * @see retrieveQuiz
 * @see addGame
 * @see roomCodeAlreadyExists
 * @see addInUseCode
 * @see retrieveGamePlayers
 * @see addUserScore
 * @see retrieveInUseCode
 * @see retrieveInUseCodeGame
 * @see deleteUserScoreForGivenUser
 */
class LobbyDatabaseTest :
    StringSpec({
        beforeTest {
            TestDatabase.create()
        }

        // Tests for retrieveQuiz
        "Retrieve Quiz 'COMP2860 Quiz' using it's quiz ID (1)" {
            testApplication {
                application { testModule() }

                val quizId = 1
                val quiz = retrieveQuiz(quizId)
                quiz shouldNotBe null

                quiz!!.quizName shouldBe "COMP2860 Quiz"
            }
        }

        "Retrieve Quiz 'COMP2870 Quiz' using it's quiz ID (3)" {
            testApplication {
                application { testModule() }

                val quizId = 3
                val quiz = retrieveQuiz(quizId)
                quiz shouldNotBe null

                quiz!!.quizName shouldBe "COMP2870 Quiz"
            }
        }

        "Retrieving Quiz using a quiz ID that doesn't exist should return null" {
            testApplication {
                application { testModule() }

                val quizId = 10
                val quiz = retrieveQuiz(quizId)
                quiz shouldBe null
            }
        }

        "Retrieving Quiz using a quiz ID of 0 should throw an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val quizId = 0
                shouldThrow<IllegalArgumentException> {
                    retrieveQuiz(quizId)
                }
            }
        }

        "Retrieving Quiz using a negative quiz ID should throw an IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val quizId = -1
                shouldThrow<IllegalArgumentException> {
                    retrieveQuiz(quizId)
                }
            }
        }

        // Tests for addGame()
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

                val timeBefore = LocalDateTime.now()
                val game = addGame(quiz!!)
                game shouldNotBe null

                val convertedTimeStamp = LocalDateTime.parse(game.timeStamp)

                val timeBetween = Duration.between(timeBefore, convertedTimeStamp).toMillis()
                timeBetween shouldBeInRange 0L..50L
            }
        }

        "Add a Game with Quiz 'COMP2870 Quiz' (normal data) to GameTable" {
            testApplication {
                application { testModule() }

                val quiz =
                    transaction(TestDatabase.db) {
                        val user = User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                        user shouldNotBe null

                        Quiz.find { QuizTable.user eq user!!.id }.firstOrNull()
                    }
                quiz shouldNotBe null

                val timeBefore = LocalDateTime.now()
                val game = addGame(quiz!!)
                game shouldNotBe null

                val convertedTimeStamp = LocalDateTime.parse(game.timeStamp)

                val timeBetween = Duration.between(timeBefore, convertedTimeStamp).toMillis()
                timeBetween shouldBeInRange 0L..50L
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
                            this.quizName = "Test Quiz"
                            this.description = "This is a test quiz"
                            this.category = "other"
                            this.keyStage = "furthereducation"
                        }
                    }
                quiz shouldNotBe null
                transaction(TestDatabase.db) { quiz.delete() }

                shouldThrow<IllegalArgumentException> {
                    addGame(quiz)
                }
            }
        }

        // Tests for roomCodeAlreadyExists()
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

        // Tests for addInUseCode()
        "Add InUseCode object for Game with Quiz 'COMP2860 Quiz' (normal data)" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val game = Game.findById(1)
                        game shouldNotBe null
                        game!!.quiz.quizName shouldBe "COMP2860 Quiz"
                        return@transaction game
                    }

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
                        game!!.quiz.quizName shouldBe "COMP2870 Quiz"
                        return@transaction game
                    }

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
                        game!!.quiz.quizName shouldBe "COMP2870 Quiz"
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
                        game!!.quiz.quizName shouldBe "COMP2870 Quiz"
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
                    game!!.quiz.quizName shouldBe "COMP2860 Quiz"

                    InUseCode.new {
                        this.roomCode = "DCBA9876"
                        this.game = game
                    }
                }
                val game =
                    transaction(TestDatabase.db) {
                        val game = Game.findById(2)
                        game shouldNotBe null
                        game!!.quiz.quizName shouldBe "COMP2870 Quiz"
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
                            this.timeStamp =
                                LocalDateTime
                                    .now()
                                    .toString()
                        }
                    }
                game shouldNotBe null

                transaction(TestDatabase.db) { game.delete() }

                shouldThrow<IllegalArgumentException> {
                    addInUseCode("LOLHI123", game)
                }
            }
        }

        // Tests for retrieveGamePlayers()
        "Retrieve all players (usernames) (2) in the Game playing Quiz 'COMP2860 Quiz'" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val foundGame = Game.find { GameTable.id eq 1 }.firstOrNull()
                        foundGame shouldNotBe null
                        foundGame!!.quiz.quizName shouldBe "COMP2860 Quiz"
                        return@transaction foundGame
                    }

                val gamePlayers = retrieveGamePlayers(game)
                gamePlayers shouldHaveSize 2
                gamePlayers shouldContain "JoanaKennah"
                gamePlayers shouldContain "MinghaoLe123"
            }
        }

        "Retrieve all players (usernames) (1) in the Game playing Quiz 'COMP2870 Quiz'" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val foundGame = Game.find { GameTable.id eq 2 }.firstOrNull()
                        foundGame shouldNotBe null
                        foundGame!!.quiz.quizName shouldBe "COMP2870 Quiz"
                        return@transaction foundGame
                    }

                val gamePlayers = retrieveGamePlayers(game)
                gamePlayers shouldHaveSize 1
                gamePlayers shouldContain "MinghaoLe123"
            }
        }

        "Retrieve empty list of players in a new Game playing Quiz 'COMP2860 Quiz'" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val quiz = Quiz.find { QuizTable.id eq 1 }.firstOrNull()
                        quiz shouldNotBe null

                        Game.new {
                            this.quiz = quiz!!
                            this.timeStamp =
                                LocalDateTime
                                    .now()
                                    .toString()
                        }
                    }

                val gamePlayers = retrieveGamePlayers(game)
                gamePlayers shouldHaveSize 0
            }
        }

        "Retrieving all players (usernames) in a Game that doesn't exist throws IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val foundGame = Game.find { GameTable.id eq 1 }.firstOrNull()
                        foundGame shouldNotBe null
                        foundGame!!.quiz.quizName shouldBe "COMP2860 Quiz"
                        return@transaction foundGame
                    }

                transaction(TestDatabase.db) { game.delete() }

                shouldThrow<IllegalArgumentException> {
                    retrieveGamePlayers(game)
                }
            }
        }

        // Tests for retrieveInUseCode()
        "Retrieve InUseCode object from InUseTable using room code 'ABCD1234' (normal data)" {
            testApplication {
                application { testModule() }

                val roomCode = "ABCD1234"
                val inUseCode = retrieveInUseCode(roomCode)
                inUseCode shouldNotBe null

                transaction(TestDatabase.db) {
                    inUseCode!!.game.id.toString() shouldBe "1"
                    inUseCode.game.quiz.quizName shouldBe "COMP2860 Quiz"
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
                    inUseCode.game.quiz.quizName shouldBe "COMP2870 Quiz"
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

        // Tests for addInUseCode()
        "Add User Score for User 'linguin0' playing a Game with Quiz 'COMP2870 Quiz'" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "linguin0" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.find { GameTable.id eq 2 }.firstOrNull()
                    }
                game shouldNotBe null

                val userScore = addUserScore(game!!, user!!)
                userScore shouldNotBe null

                transaction(TestDatabase.db) {
                    userScore.game.id.value shouldBe game.id.value
                    userScore.user.id.value shouldBe user.id.value
                    userScore.score shouldBe 0
                }
            }
        }

        "Add User Score for User 'MinghaoLe123' playing a Game with Quiz 'COMP2870 Quiz'" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "linguin0" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.find { GameTable.id eq 2 }.firstOrNull()
                    }
                game shouldNotBe null

                val userScore = addUserScore(game!!, user!!)
                userScore shouldNotBe null

                transaction(TestDatabase.db) {
                    userScore.game.id.value shouldBe game.id.value
                    userScore.user.id.value shouldBe user.id.value
                    userScore.score shouldBe 0
                }
            }
        }

        "Adding User Score for User 'JoanaKennah' playing a Game that doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.find { GameTable.id eq 1 }.firstOrNull()
                    }
                game shouldNotBe null
                transaction(TestDatabase.db) { game!!.delete() }

                shouldThrow<IllegalArgumentException> {
                    addUserScore(game!!, user!!)
                }
            }
        }

        "Adding User Score for a User that doesn't exist playing a Game with Quiz 'COMP2860 Quiz' should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.find { GameTable.id eq 1 }.firstOrNull()
                    }
                game shouldNotBe null
                transaction(TestDatabase.db) { user!!.delete() }

                shouldThrow<IllegalArgumentException> {
                    addUserScore(game!!, user!!)
                }
            }
        }

        "Adding User Score for a User that doesn't exist playing a Game that doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.find { GameTable.id eq 1 }.firstOrNull()
                    }
                game shouldNotBe null
                transaction(TestDatabase.db) {
                    user!!.delete()
                    game!!.delete()
                }

                shouldThrow<IllegalArgumentException> {
                    addUserScore(game!!, user!!)
                }
            }
        }

        // Tests for retrieveInUseCodeGame()
        "Retrieve Game object using InUseCode for Quiz 'COMP2860 Quiz' (normal data)" {
            testApplication {
                application { testModule() }
                val roomCode = "ABCD1234"

                val inUseCode =
                    transaction(TestDatabase.db) {
                        InUseCode.find { InUseTable.room_code eq roomCode }.firstOrNull()
                    }
                inUseCode shouldNotBe null
                inUseCode!!.roomCode shouldBe roomCode

                val game = retrieveInUseCodeGame(inUseCode)
                game shouldNotBe null

                transaction(TestDatabase.db) {
                    game!!.quiz.quizName shouldBe "COMP2860 Quiz"
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
                inUseCode!!.roomCode shouldBe roomCode

                val game = retrieveInUseCodeGame(inUseCode)
                game shouldNotBe null

                transaction(TestDatabase.db) {
                    game!!.quiz.quizName shouldBe "COMP2870 Quiz"
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
                        game!!.quiz.quizName shouldBe "COMP2870 Quiz"

                        InUseCode.new {
                            this.roomCode = "TEST1234"
                            this.game = game
                        }
                    }

                transaction(TestDatabase.db) { inUseCode.delete() }

                shouldThrow<IllegalArgumentException> {
                    retrieveInUseCodeGame(inUseCode)
                }
            }
        }

        // Tests for deleteUserScoreForGivenUser()
        "Delete User Score for User 'MinghaoLe123' playing a Game with Quiz 'COMP2870 Quiz'" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.find { GameTable.id eq 2 }.firstOrNull()
                    }
                game shouldNotBe null

                val deleteSuccess = deleteUserScoreForGivenUser(game!!, user!!)
                deleteSuccess shouldBe true

                val check =
                    transaction(TestDatabase.db) {
                        UserScore.find { UserScoreTable.user eq user.id }.filter { it.game == game }
                    }
                check shouldHaveSize 0
            }
        }

        "Delete User Score for User 'JoanaKennah' playing a Game with Quiz 'COMP2860 Quiz'" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.find { GameTable.id eq 1 }.firstOrNull()
                    }
                game shouldNotBe null

                val deleteSuccess = deleteUserScoreForGivenUser(game!!, user!!)
                deleteSuccess shouldBe true

                val check =
                    transaction(TestDatabase.db) {
                        UserScore.find { UserScoreTable.user eq user.id }.filter { it.game == game }
                    }
                check shouldHaveSize 0
            }
        }

        "Deleting User Score for User 'JoanaKennah' playing a Game that doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.find { GameTable.id eq 1 }.firstOrNull()
                    }
                game shouldNotBe null
                transaction(TestDatabase.db) { game!!.delete() }

                shouldThrow<IllegalArgumentException> {
                    deleteUserScoreForGivenUser(game!!, user!!)
                }
            }
        }

        "Deleting User Score for a User that doesn't exist playing a Game with quiz 'COMP2860 Quiz' should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.find { GameTable.id eq 1 }.firstOrNull()
                    }
                game shouldNotBe null
                transaction(TestDatabase.db) { user!!.delete() }

                shouldThrow<IllegalArgumentException> {
                    deleteUserScoreForGivenUser(game!!, user!!)
                }
            }
        }

        "Deleting User Score for a User that doesn't exist playing a Game that doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                    }
                user shouldNotBe null

                val game =
                    transaction(TestDatabase.db) {
                        Game.find { GameTable.id eq 1 }.firstOrNull()
                    }
                game shouldNotBe null
                transaction(TestDatabase.db) {
                    user!!.delete()
                    game!!.delete()
                }

                shouldThrow<IllegalArgumentException> {
                    deleteUserScoreForGivenUser(game!!, user!!)
                }
            }
        }

        // Tests for deleteGameUserScores()
        "Delete all UserScores (2) for Game playing Quiz 'COMP2860 Quiz'" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        Game.find { GameTable.id eq 1 }.firstOrNull()
                    }
                game shouldNotBe null

                val userScores =
                    transaction(TestDatabase.db) {
                        UserScore.find { UserScoreTable.game eq game!!.id }.toList()
                    }
                userScores shouldHaveSize 2

                val deleteSuccess = deleteGameUserScores(game!!)
                deleteSuccess shouldBe true

                val check =
                    transaction(TestDatabase.db) {
                        UserScore.find { UserScoreTable.game eq game.id }.toList()
                    }
                check shouldHaveSize 0
            }
        }

        "Delete all UserScores (1) for Game playing Quiz 'COMP2870 Quiz'" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        Game.find { GameTable.id eq 2 }.firstOrNull()
                    }
                game shouldNotBe null

                val userScores =
                    transaction(TestDatabase.db) {
                        UserScore.find { UserScoreTable.game eq game!!.id }.toList()
                    }
                userScores shouldHaveSize 1

                val deleteSuccess = deleteGameUserScores(game!!)
                deleteSuccess shouldBe true

                val check =
                    transaction(TestDatabase.db) {
                        UserScore.find { UserScoreTable.game eq game.id }.toList()
                    }
                check shouldHaveSize 0
            }
        }

        "Trying to delete UserScore(s) (0) for a new Game playing Quiz 'COMP2860 Quiz' should return False" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        val quiz = Quiz.findById(1)
                        quiz shouldNotBe null

                        Game.new {
                            this.quiz = quiz!!
                            this.timeStamp =
                                LocalDateTime
                                    .now()
                                    .toString()
                        }
                    }
                game shouldNotBe null

                val userScores =
                    transaction(TestDatabase.db) {
                        UserScore.find { UserScoreTable.game eq game.id }.toList()
                    }
                userScores shouldHaveSize 0

                val deleteSuccess = deleteGameUserScores(game)
                deleteSuccess shouldBe false
            }
        }

        "Deleting UserScore(s) for a Game that doesn't exist should throw IllegalArgumentException" {
            testApplication {
                application { testModule() }

                val game =
                    transaction(TestDatabase.db) {
                        Game.find { GameTable.id eq 1 }.firstOrNull()
                    }
                game shouldNotBe null

                transaction(TestDatabase.db) { game!!.delete() }

                shouldThrow<IllegalArgumentException> {
                    deleteGameUserScores(game!!)
                }
            }
        }
    })
