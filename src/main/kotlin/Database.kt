package com.example

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.core.eq
import com.password4j.Password
import com.example.database.ProjectDatabase
import com.example.database.tables.*
import com.example.database.objects.*
import kotlin.text.isNotEmpty

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
fun addUser(username: String, password: String, name: String, email: String, key_stage: String, role: String) : User? {
    // Check to ensure all parameters are not empty
    for (parameter in listOf(username, name, email, key_stage, role)) {
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
            this.email = email
            this.key_stage = key_stage
            this.role = role
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

/**
 * Creates and adds a new Question to QuestionTable
 */
fun addQuestion(quiz: Quiz, question: String, key_stage: String) : Question {
    for (parameter in listOf(question, key_stage)) {
        require(parameter.isNotEmpty())
    }
    // Don't need to check if Question already exists
    val newQuestion = Question.new {
        this.question = question
        this.key_stage = key_stage
    }
    val newQuinstance = Quinstance.new {
        this.quiz = quiz
        this.question = newQuestion
    }
    println("Quinstance ID: ${newQuinstance.id}")
    return newQuestion
}

/**
 * Deletes given question from QuestionTable. Returns true if the question is successfully deleted
 */
fun deleteQuestion(question: Question): Boolean {
    // Finds if the given question exists, then deletes it
    val foundQuestion = Question.find { QuestionTable.id eq question.id }
    if (foundQuestion.firstOrNull() != null) {
        question.delete()
        return true
    }
    return false
}

// retrieveQuizQuestions would go here ?

/**
 * Creates and adds a new Quiz to QuizTable
 */
fun addQuiz(user: User, quiz_name: String, description: String, category: String, num_of_questions: Int = 0) : Quiz {
    // Checks the User exists
    require(retrieveUser(user.username) != null) { "User must not be null" }
    require(num_of_questions >= 0) { "Number of questions must be greater than or equal to 0" }
    // Checks string parameters are no empty
    for (parameter in listOf(quiz_name, description, category)) {
        require(parameter.isNotEmpty()) { "Parameter $parameter must not be empty" }
    }
    // Don't need to check if Quiz already exists
    val newQuiz = Quiz.new {
        this.user = user // this is weird i need to double check it
        this.quiz_name = quiz_name
        this.description = description
        this.category = category
        this.num_of_questions = num_of_questions
    }
    return newQuiz
}

/**
 * Deletes given Quiz from QuizTable. Returns true if the quiz is successfully deleted
 */
fun deleteQuiz(quiz: Quiz) : Boolean {
    // Finds if given Quiz exists, then deletes it
    val foundQuiz = Quiz.find { QuizTable.id eq quiz.id }
    if (foundQuiz.firstOrNull() != null) {
        quiz.delete()
        return true
    }
    return false
}

/**
 * Retrieve all Quizzes that a User has created. Will return null if the User passed in does not exist. An empty list
 * can be returned if the User has no quizzes.
 */
fun retrieveUserQuizzes(username: String) : MutableList<Quiz>? {
    // Returns null if the user doesn't exist
    val currentUser = retrieveUser(username) ?: return null
    val userID = currentUser.id
    val quizList = mutableListOf<Quiz>()

    // Finds all quizzes associated with the specified User
    val foundQuizzes = Quiz.find { QuizTable.user eq userID }
    for (quiz in foundQuizzes) {
        quizList.add(quiz)
    }
    return quizList
}