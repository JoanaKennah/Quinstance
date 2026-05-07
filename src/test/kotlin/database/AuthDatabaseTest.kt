package database

import com.example.addUser
import com.example.database.TestDatabase
import com.example.database.objects.User
import com.example.database.tables.UserTable
import com.example.retrieveUser
import com.example.testModule
import com.example.deleteUser
import com.password4j.Password
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldHaveLength
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Unit tests for Database functions 'retrieveUser' and 'addUser'
 * @see retrieveUser
 * @see addUser
 */
class AuthDatabaseTest :
    StringSpec({
        // Tests for retrieveUser()
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
                    it?.keyStage shouldBe "furthereducation"
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
                    it?.keyStage shouldBe "furthereducation"
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
                    it?.keyStage shouldBe "furthereducation"
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

        // Tests for addUser()
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

        // Tests for deleteUser()
        "deleteUser should successfully delete an existing user from database" {
            testApplication {
                application { testModule() }
                val user = addUser("testdelete", "password123", "Test Delete", "test@test.com", "furthereducation", "student")!!
                deleteUser(user)
                retrieveUser("testdelete") shouldBe null
            }
        }

        "deleteUser should throw IllegalArgumentException if user does not exist" {
            testApplication {
                application { testModule() }
                val user = addUser("testdelete2", "password123", "Test Delete", "test@test.com", "furthereducation", "student")!!
                deleteUser(user)
                shouldThrow<IllegalArgumentException> {
                    deleteUser(user)
                }
            }
        }
    })
