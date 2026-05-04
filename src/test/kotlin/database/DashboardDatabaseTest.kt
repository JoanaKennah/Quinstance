package database

import com.example.UserAnswers
import com.example.database.TestDatabase
import com.example.database.objects.User
import com.example.database.tables.UserTable
import com.example.retrieveUser
import com.example.retrieveUserQuizAttempts
import com.example.testModule
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Unit tests for Database functions primarily used in DashboardRouting.kt.
 * @see retrieveUserQuizAttempts
 */
class DashboardDatabaseTest :
    StringSpec({
        // Tests for retrieveUserQuizAttempts
        beforeTest {
            TestDatabase.create()
        }

        "Retrieve all UserQuizAttempt(s) for User 'JoanaKennah'" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "JoanaKennah" }.firstOrNull()
                    }
                user shouldNotBe null

                val userQuizAttempts = retrieveUserQuizAttempts(user!!)
                userQuizAttempts shouldNotBe null
                userQuizAttempts shouldHaveSize 1

                val quizAttempt = userQuizAttempts.first()
                quizAttempt shouldNotBe null
                quizAttempt.quizName shouldBe "COMP2860 Quiz"
                quizAttempt.score shouldBe 1000
            }
        }

        "Retrieve all UserQuizAttempt(s) for User 'MinghaoLe123'" {
            testApplication {
                application { testModule() }

                val quizAttemptData =
                    listOf(
                        listOf("COMP2860 Quiz", 500),
                        listOf("COMP2870 Quiz", 750),
                    )

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                    }
                user shouldNotBe null

                val userQuizAttempts = retrieveUserQuizAttempts(user!!)
                userQuizAttempts shouldNotBe null
                userQuizAttempts shouldHaveSize 2

                for (i in 0..1) {
                    userQuizAttempts[i].quizName shouldBe quizAttemptData[i][0]
                    userQuizAttempts[i].score shouldBe quizAttemptData[i][1]
                }
            }
        }

        "Retrieve empty list of UserQuizAttempt(s) for User 'linguin0'" {
            testApplication {
                application { testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "linguin0" }.firstOrNull()
                    }
                user shouldNotBe null

                val userQuizAttempts = retrieveUserQuizAttempts(user!!)
                userQuizAttempts shouldNotBe null
                userQuizAttempts shouldHaveSize 0
            }
        }

        "Retrieving UserQuizAttempt(s) for a User that doesn't exist throws IllegalArgumentException" {
            testApplication {
                application{ testModule() }

                val user =
                    transaction(TestDatabase.db) {
                        User.find { UserTable.username eq "MinghaoLe123" }.firstOrNull()
                    }
                user shouldNotBe null
                transaction(TestDatabase.db) { user!!.delete() }

                shouldThrow<IllegalArgumentException> {
                    retrieveUserQuizAttempts(user!!)
                }
            }
        }
    })
