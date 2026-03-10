package com.example.database

import com.example.database.tables.UserTable
import com.password4j.Password
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Demo databas used for Testing. It is currently set up to test our first Sprint. Contains 3 Users, all with hashed
 * passwords.
 * @param db connection to access the demo Database
 */
object TestDatabase {
    const val URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;"
    const val DRIVER = "org.h2.Driver"

    val db by lazy {
        Database.connect(URL, DRIVER)
    }

    // Creates UserTable and three Users for testing purposes.
    fun create() {
        transaction(db) {
            // Currently just creates UserTable
            SchemaUtils.drop(UserTable)
            SchemaUtils.create(UserTable)

            val hashedPasswordOne = Password.hash("password123").addRandomSalt(8).withScrypt()
            val userOne = UserTable.insertAndGetId {
                it[username] = "linguin0"
                it[password] = hashedPasswordOne.result
                it[name] = "Ethan Harris"
                it[email_address] = "ethan@gmail.com"
                it[type] = "Admin"
            }

            val hashedPasswordTwo = Password.hash("password456").addRandomSalt(8).withScrypt()
            val userTwo = UserTable.insertAndGetId {
                it[username] = "JoanaKennah"
                it[password] = hashedPasswordTwo.result
                it[name] = "Joana Kennah"
                it[email_address] = "joana@outlook.com"
                it[type] = "Admin"
            }

            val hashedPasswordThree = Password.hash("password789").addRandomSalt(8).withScrypt()
            val userThree = UserTable.insertAndGetId {
                it[username] = "MinghaoLe123"
                it[password] = hashedPasswordThree.result
                it[name] = "Minghao Le"
                it[email_address] = "minghao@yahoo.com"
                it[type] = "Admin"
            }
        }
    }
}

