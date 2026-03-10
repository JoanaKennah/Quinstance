package com.example

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.core.eq
import com.password4j.Password
import com.example.database.ProjectDatabase
import com.example.database.tables.*
import com.example.database.objects.*

/**
 * Creates all required tables if they do not already exist.
 */
fun configureDatabase() {
    // Establishes connection from ProjectDatabase object
    val dbConnection = ProjectDatabase.database

    transaction(dbConnection) {
        SchemaUtils.create(AnswerTable,
            InUseTable,
            QuestionTable,
            QuinstanceTable,
            QuizTable,
            SessionTable,
            UserScoreTable,
            UserTable)
    }

}

/**
 * Retrieves user from UserTable using their username, returns null if no user found
 */
fun retrieveUser(username: String) : User? {
    val foundUser = User.find { UserTable.username eq username }
    return foundUser.firstOrNull()
}

/**
 * Adds a new User to UserTable
 */
fun addUser(username: String, password: String, name: String, email_address: String, type: String) : User? {
    // Check to ensure all parameters are not empty
    for (parameter in listOf(username, name, email_address, type)) {
        require(parameter.isNotEmpty())
    }
    // Check to see if user already exists with the same username
    val userExists = retrieveUser(username)
    if (userExists == null) {
        val hash = Password.hash(password).addRandomSalt(8).withScrypt()
        val newUser = User.new {
            this.username = username
            this.password = hash.result
            this.name = name
            this.email_address = email_address
            this.type = type
        }
        return newUser
    }; return null
}

/**
 * Deletes given user from UserTable (possibly used for Admin purposes)
 **/
fun deleteUser(user : User) {
    // Checks if the user exists in the table before deleting it
    val userExists = retrieveUser(user.username)
    if (userExists != null) {
        user.delete()
    } else {
        // Throws an exception as it shouldn't be possible for a website user to have this error
        throw Exception("User doesn't exist")
    }

}