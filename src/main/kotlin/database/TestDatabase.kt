package com.example.database

import com.example.database.objects.Quiz
import com.example.database.tables.UserTable
import com.example.database.tables.QuizTable
import com.example.database.tables.QuestionTable
import com.example.database.tables.QuinstanceTable
import com.example.database.tables.AnswerTable
import com.password4j.Password
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Demo database used for Testing. It is currently set up to test our first Sprint. Contains 3 Users, all with hashed
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
            SchemaUtils.drop(UserTable, QuizTable, QuestionTable, AnswerTable, QuinstanceTable)
            SchemaUtils.create(UserTable, QuizTable, QuestionTable, AnswerTable, QuinstanceTable)

            // Doesn't use functions I developed to potentially avoid bugs
            val hashedPasswordOne = Password.hash("password123").addRandomSalt(8).withScrypt()
            val userOne = UserTable.insertAndGetId {
                it[username] = "linguin0"
                it[password] = hashedPasswordOne.result
                it[name] = "Ethan Harris"
                it[email] = "ethan@gmail.com"
                it[key_stage] = "furthereducation"
                it[role] = "student"
            }

            val hashedPasswordTwo = Password.hash("password456").addRandomSalt(8).withScrypt()
            val userTwo = UserTable.insertAndGetId {
                it[username] = "JoanaKennah"
                it[password] = hashedPasswordTwo.result
                it[name] = "Joana Kennah"
                it[email] = "joanakennah@gmail.com"
                it[key_stage] = "furthereducation"
                it[role] = "teacher"
            }

            val hashedPasswordThree = Password.hash("password789").addRandomSalt(8).withScrypt()
            val userThree = UserTable.insertAndGetId {
                it[username] = "MinghaoLe123"
                it[password] = hashedPasswordThree.result
                it[name] = "Minghao Le"
                it[email] = "tgqj4326@leeds.ac.uk"
                it[key_stage] = "furthereducation"
                it[role] = "teacher"
            }

            // Quiz #1 for linguin0 (2 questions with answers)
            val userOneQuiz = QuizTable.insertAndGetId {
                it[user] = userOne
                it[quiz_name] = "COMP2860 Quiz"
                it[num_of_questions] = 0
                it[description] = "Quiz to test my limited knowledge on COMP2860"
                it[category] = "computerscience"
            }

            val userOneQuizQ1 = QuestionTable.insertAndGetId {
                it[question] = "What is a process?"
                it[key_stage] = "furthereducation"
            }

            Quiz.findByIdAndUpdate(userOneQuiz.value) {
                it.num_of_questions += 1
            }

            val userOneQuizQ2 = QuestionTable.insertAndGetId {
                it[question] = "What is a thread?"
                it[key_stage] = "furthereducation"
            }

            Quiz.findByIdAndUpdate(userOneQuiz.value) {
                it.num_of_questions += 1
            }

            AnswerTable.insert {
                it[question] = userOneQuizQ1
                it[correct_answer] = "A program in execution"
                it[option2] = "Come to understand or accept something over a period of time"
                it[option3] = "A summons or writ requiring a person to appear in court"
                it[option4] = "A program waiting to be executed"
            }

            AnswerTable.insert {
                it[question] = userOneQuizQ2
                it[correct_answer] = "A small sequence of programmed instructions"
                it[option2] = "A piece of yarn or twine"
                it[option3] = null
                it[option4] = null
            }

            QuinstanceTable.insert {
                it[quiz] = userOneQuiz
                it[question] = userOneQuizQ1
            }

            QuinstanceTable.insert {
                it[quiz] = userOneQuiz
                it[question] = userOneQuizQ2
            }

            // Quiz #2 for linguino (empty)
            QuizTable.insert {
                it[user] = userOne
                it[quiz_name] = "FNAF Quiz"
                it[num_of_questions] = 0
                it[description] = "Freddy Fazbear"
                it[category] = "other"
            }

            // Quiz #1 for user JoanaKennah (1 question with answers)
            val userTwoQuiz = QuizTable.insertAndGetId {
                it[user] = userTwo
                it[quiz_name] = "COMP2870 Quiz"
                it[num_of_questions] = 0
                it[description] = "University of Leeds COMP2870 Quiz for Revision :)"
                it[category] = "computerscience"
            }

            val userTwoQuizQ1 = QuestionTable.insertAndGetId {
                it[question] = "What is a finite state automation?"
                it[key_stage] = "furthereducation"
            }

            Quiz.findByIdAndUpdate(userTwoQuiz.value) {
                it.num_of_questions += 1
            }

            AnswerTable.insert {
                it[question] = userTwoQuizQ1
                it[correct_answer] = "A model of computation with an extremely limited amount of memory"
                it[option2] = "A machine that only runs a finite amount of times"
                it[option3] = "I have ran out of fake answers"
                it[option4] = null
            }

            QuinstanceTable.insert {
                it[quiz] = userTwoQuiz
                it[question] = userTwoQuizQ1
            }
        }
    }
}

// to use test database over the real one, type in the command box "./gradlew run -PapplicationModule=testModule