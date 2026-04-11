package com.example

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.replace
import org.jetbrains.exposed.v1.core.eq
import com.password4j.Password
import kotlin.text.isNotEmpty
import com.example.database.ProjectDatabase
import com.example.database.TestDatabase
import com.example.database.tables.*
import com.example.database.objects.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

//  All code here was written by a human :>
/*  Websites used for debugging/learning
    - https://kotlinlang.org
    - https://www.jetbrains.com/help/exposed
    - https://stackoverflow.com
 */

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
            GameTable,
            UserScoreTable,
            UserTable)
    }

}

/**
 * Retrieves user from UserTable using their username, returns null if no user found
 */
fun retrieveUser(username: String) : User? {
    require(username.isNotEmpty()) { "User can't be empty" }
    return transaction(TransactionManager.defaultDatabase) {
        User.find { UserTable.username eq username }.firstOrNull()
    }
}

/**
 * Adds a new User to UserTable. Returns null if user already exists
 */
fun addUser(username: String, password: String, name: String, email: String, key_stage: String, role: String) : User? {
    // Check to ensure all parameters are not empty
    for (parameter in listOf(username, password, name, email, key_stage, role)) {
        require(parameter.isNotEmpty())
    }
    require(username.length <= 50)
    require(email.length <= 50)
    // Check to see if user already exists with the same username
    val userExists = retrieveUser(username)
    if (userExists == null) {
        val hash = Password.hash(password).addRandomSalt(8).withScrypt()
        return transaction(TransactionManager.defaultDatabase) {
            User.new {
                this.username = username
                this.password = hash.result
                this.name = name
                this.email = email
                this.key_stage = key_stage
                this.role = role
            }
        }
    }; return null
}

/**
 * Deletes given user from UserTable (possibly used for Admin purposes)
 **/
fun deleteUser(user : User) {
    // Checks if the user exists in the table before deleting it
    val userExists = retrieveUser(user.username)
    if (userExists != null) {
        transaction(TransactionManager.defaultDatabase) {
            user.delete()
        }
    } else {
        // Throws an exception as it shouldn't be possible for a website user to have this error
        throw Exception("User doesn't exist")
    }

}

/**
 * Creates and adds a new Question to QuestionTable. Increments given Quiz num_of_questions by 1.
 */
fun addQuestion(quiz: Quiz, question: String, key_stage: String) : Question {
    for (parameter in listOf(question, key_stage)) {
        require(parameter.isNotEmpty())
    }
    val newQuestion = transaction(TransactionManager.defaultDatabase) {
        // Don't need to check if Question already exists
        val question = Question.new {
            this.question = question
            this.key_stage = key_stage
        }
        // Creates Quinstance so we know which Quiz is associated with what Question
        Quinstance.new {
            this.quiz = quiz
            this.question = question
        }

        quiz.num_of_questions += 1

        return@transaction question
    }
    return newQuestion
}

/**
 * Deletes given question from QuestionTable. Returns true if the question is successfully deleted
 */
fun deleteQuestion(question: Question): Boolean {
    // Finds if the given question exists, then deletes it
    return transaction(TransactionManager.defaultDatabase) {
        val foundQuestion = Question.find { QuestionTable.id eq question.id }
        if (foundQuestion.firstOrNull() != null) {
            question.delete()
            return@transaction true
        }
        return@transaction false
    }
}

/**
 * Retrieves all Questions associated with the given Quiz. If there are no questions associated with the quiz,
 * a null value is returned.
 */
fun retrieveQuizQuestions(quiz: Quiz) : MutableList<Question> {
    // Find all question ID's associated with the given Quiz
    return transaction(TransactionManager.defaultDatabase) {
        // Check to see if quiz actually exists
        println("test ${quiz.id}")
        require( (Quiz.findById(quiz.id))?.id == quiz.id ) { "Quiz received does not exist" }

        val foundQuestionQuinstances = Quinstance.find { QuinstanceTable.quiz eq quiz.id }
        val questionList = mutableListOf<Question>()

        // Retrieves Question objects from the ID's found and appends them to a list, which is returned.
        // Can be empty but never null
        for (questionQuinstance in foundQuestionQuinstances) {
            val question = Question.findById(questionQuinstance.id)
            if (question != null) {
                questionList.add(question)
            }
        }
        return@transaction questionList
    }
}

/**
 * Creates and adds a new Quiz to QuizTable
 */
fun addQuiz(user: User, quiz_name: String, description: String, category: String, key_stage: String, num_of_questions: Int = 0) : Quiz {
    // Checks the User exists
    require(retrieveUser(user.username) != null) { "User must not be null" }
    require(num_of_questions >= 0) { "Number of questions must be greater than or equal to 0" }
    // Checks string parameters are no empty
    for (parameter in listOf(quiz_name, description, category, key_stage)) {
        require(parameter.isNotEmpty()) { "Parameter $parameter must not be empty" }
    }
    // Don't need to check if Quiz already exists
    return transaction(TransactionManager.defaultDatabase) {
        Quiz.new {
            this.user = user
            this.quiz_name = quiz_name
            this.description = description
            this.category = category
            this.key_stage = key_stage
            this.num_of_questions = num_of_questions
        }
    }
}

/**
 * Deletes given Quiz from QuizTable. Returns true if the quiz is successfully deleted
 */
fun deleteQuiz(quiz: Quiz) : Boolean {
    // Finds if given Quiz exists, then deletes it
    return transaction(TransactionManager.defaultDatabase) {
        val foundQuiz = Quiz.find { QuizTable.id eq quiz.id }
        if (foundQuiz.firstOrNull() != null) {
            quiz.delete()
            return@transaction true
        }
        return@transaction false
    }
}

/**
 * Retrieve all Quizzes that a User has created. Will return null if the User passed in does not exist. An empty list
 * can be returned if the User has no quizzes.
 */
fun retrieveUserQuizzes(username: String) : MutableList<Quiz>? {
    // Returns null if the user doesn't exist
    val currentUser = retrieveUser(username) ?: return null
    val userID = currentUser.id

    return transaction(TransactionManager.defaultDatabase) {
        val quizList = mutableListOf<Quiz>()

        // Finds all quizzes associated with the specified User
        val foundQuizzes = Quiz.find { QuizTable.user eq userID }
        for (quiz in foundQuizzes) {
            quizList.add(quiz)
        }
        return@transaction quizList
    }
}

/**
 * Creates and adds an Answer (to a Question) to AnswerTable
 */
fun addAnswer(question: Question, option1: String, option2: String, option3: String? = null, option4: String? = null, correctIndex: Int) : Answer? {
    // Will need to discuss with team if we want the user to be able to have 2, 3 or 4 answers
    //Slightly changed this to accept 4 answers then flag the correct one, helped make the frontend a bit cleaner
    transaction(TransactionManager.defaultDatabase) {
        require( (Question.findById(question.id))?.id == question.id ) { "Question given does not exist" }
    }
    require(option1.isNotEmpty()) { "option 1 must not be empty" }
    require(option2.isNotEmpty()) { "option 2 must not be empty" }

    // Check to see if an Answer already exists (Question and Answer have 1:1 relationship), return null if Answer
    // already exists
    return transaction(TransactionManager.defaultDatabase) {
        val check = Answer.find {AnswerTable.question eq question.id }
        if (check.firstOrNull() != null) {
            return@transaction null
        }

        val allOptions = listOfNotNull(option1, option2, option3, option4)
        val newAnswer = Answer.new {
            this.question = question
            this.correct_answer = allOptions[correctIndex]
            this.option2 = option2 
            this.option3 = option3
            this.option4 = option4
        }
        return@transaction newAnswer
    }
}

/**
 * Retrieves Answer for given Question. Will return null if there is no answer.
 * NOTE: I was going to create a function to return ALL answers from a quiz, however I believe this may get confusing
 * unless I was to map each answer to a questionID. Am happy to do this instead but will want to discuss with the team
 */
fun retrieveQuestionAnswer(question: Question) : Answer? {
    return transaction(TransactionManager.defaultDatabase) {
        // Check to see if quiz given actually exists
        require( (Question.findById(question.id))?.id == question.id ) { "Question given does not exist" }

        val foundAnswer = Answer.find(AnswerTable.question eq question.id)
        // Return null if there is no Answer to this Question
        val answer = foundAnswer.firstOrNull() ?: return@transaction null
        return@transaction answer
    }
}