package com.example

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.core.eq
import com.password4j.Password
import kotlin.text.isNotEmpty
import com.example.database.ProjectDatabase
import com.example.database.tables.*
import com.example.database.objects.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

//  All code here was written by a human :>
/*  Websites used for debugging/learning
    - https://kotlinlang.org
    - https://www.jetbrains.com/help/exposed
    - https://stackoverflow.com
 */

data class UserAttemptView(
    val quizName: String,
    val score: Int
)

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
 * Creates and adds a new Quiz to QuizTable
 */
fun addQuiz(user: User, quiz_name: String, description: String, category: String, key_stage: String, num_of_questions: Int = 0, minigamesEnabled: Boolean = false) : Quiz {
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
            this.minigames_enabled = minigamesEnabled
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
 * Retrieves Quiz from QuizTable using a quizID, returns null if no Quiz found.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 */
fun retrieveQuiz(quizID: Int): Quiz? {
    require(quizID >= 0) { "QuizID must be greater than or equal to 0" }
    return transaction(TransactionManager.defaultDatabase) {
        Quiz.findById(quizID)
    }
}

/**
 * Retrieves Quiz from QuizTable using a gameID, returns null if no Quiz found.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 */
fun retrieveQuizFromGameID(gameID: Int): Quiz? {
    // Check to see if given game actually exists
    require(gameID >= 0) { "GameID must be non-negative" }
    return transaction(TransactionManager.defaultDatabase) {
        val game = retrieveGame(gameID)
        return@transaction game?.quiz
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
 * Creates and adds an Answer (to a Question) to AnswerTable
 */
fun addAnswer(question: Question, option1: String, option2: String, option3: String? = null, option4: String? = null, correctIndex: Int) : Answer? {
    // Slightly changed this to accept 4 answers then flag the correct one, helped make the frontend a bit cleaner
    transaction(TransactionManager.defaultDatabase) {
        require( (Question.findById(question.id))?.id == question.id ) { "Question given does not exist" }
    }
    require(option1.isNotEmpty()) { "Option 1 must not be empty" }
    require(option2.isNotEmpty()) { "Option 2 must not be empty" }
    require(correctIndex in 0..3) { "CorrectIndex must be between 0 and 3" }
    require(listOf(option1, option2, option3, option4)[correctIndex] != null) { "Correct answer cannot be null" }

    // Check to see if an Answer already exists (Question and Answer have 1:1 relationship), return null if Answer
    // already exists
    return transaction(TransactionManager.defaultDatabase) {
        val check = Answer.find {AnswerTable.question eq question.id }
        if (check.firstOrNull() != null) return@transaction null

        val allOptions = listOfNotNull(option1, option2, option3, option4)
        val correctOption = allOptions[correctIndex]
        var finalOption2 = ""
        var finalOption3: String? = null
        var finalOption4: String? = null

        // Not happy about this implementation but avoids issues found in developing csv reading. Ensures there are
        // no duplicate answers.
        when (correctOption) {
            option1 -> {
                finalOption2 = option2
                if (option3 != null) finalOption3 = option3
                if (option4 != null) finalOption4 = option4
            }
            option2 -> {
                finalOption2 = option1
                if (option3 != null) finalOption3 = option3
                if (option4 != null) finalOption4 = option4
            }
            option3 -> {
                finalOption2 = option1
                finalOption3 = option2
                finalOption4 = option4
            }
            option4 -> {
                finalOption2 = option1
                finalOption3 = option2
                finalOption4 = option3
            }
        }

        val newAnswer = Answer.new {
            this.question = question
            this.correct_answer = allOptions[correctIndex]
            this.option2 = finalOption2
            this.option3 = finalOption3
            this.option4 = finalOption4
        }
        return@transaction newAnswer
    }
}

/**
 * Retrieves Answer for given Question. Will return null if there is no answer.
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

/**
 * Retrieves Game from GameTable using its gameID, returns null if no Game found.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 */
fun retrieveGame(gameID: Int): Game? {
    // Check given gameID is valid
    require(gameID >= 0) { "GameID must be non-negative" }
    return transaction(TransactionManager.defaultDatabase) {
       Game.findById(gameID)
    }
}

/**
 * Creates and adds a new Game to GameTable.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 */
fun addGame(quiz: Quiz): Game {
    // Check to see if given quiz actually exists
    require(retrieveQuiz(quiz.id.value) != null) { "Quiz must exist" }
    return transaction(TransactionManager.defaultDatabase) {
        Game.new {
            this.quiz = quiz
            this.timeStamp = java.time.LocalDateTime.now().toString()
        }
    }
}

/**
 * Retrieve all players from an existing Game and return them inside a List.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 */
fun retrieveGamePlayers(game: Game): List<String> {
    // Check to see if given game actually exists
    require(retrieveGame(game.id.value) != null) { "Game must exist" }
    return transaction(TransactionManager.defaultDatabase) {
        UserScore.find { UserScoreTable.game eq game.id }.map { it.user.username }
    }
}

/**
 * Checks InUseTable to see if there is an InUseCode object that already exists with the given room code.
 * @return true if the room code already exists, false otherwise
 */
fun roomCodeAlreadyExists(roomCode: String): Boolean {
    require(roomCode.isNotEmpty()) { "Room code must not be empty" }
    val acceptableChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    for (character in roomCode) {
        require(character in acceptableChars) { "Room code should only contain letters or numbers" }
    }
    require(roomCode.length == 8) { "Room code must have length 8" }

    val check =
        transaction(TransactionManager.defaultDatabase) {
            InUseCode.find { InUseTable.room_code eq roomCode }.firstOrNull()
        }
    return check != null
}

/**
 * Creates and adds a new InUseCode record to InUseCodeTable.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 */
fun addInUseCode(roomCode: String, game: Game): InUseCode {
    // Check to see if given game actually exists and that the room code is valid (length 8, not empty)
    require(retrieveGame(game.id.value) != null) { "Game must exist" }
    require(roomCode.isNotEmpty()) { "Room code must not be empty" }
    val acceptableChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    for (character in roomCode) {
        require(character in acceptableChars) { "Room code should only contain letters or numbers" }
    }
    require(roomCode.length == 8) { "Room code must have length 8" }
    require(!roomCodeAlreadyExists(roomCode)) { "Room code already exists" }

    return transaction(TransactionManager.defaultDatabase) {
        InUseCode.new {
            this.room_code = roomCode
            this.game = game
        }
    }
}

/**
 * Retrieves room code attribute for the given Game, stored in InUseCodeTable.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 */
fun retrieveRoomCode(game: Game): String? {
    // Check to see if given game actually exists
    require(retrieveGame(game.id.value) != null) { "Game must exist" }

    return transaction(TransactionManager.defaultDatabase) {
        InUseCode.find { InUseTable.game eq game.id }.firstOrNull()?.room_code
    }
}

/**
 * Retrieves InUseCode object from InUseCodeTable using its room code, returns null if it doesn't exist
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 */
fun retrieveInUseCode(roomCode: String): InUseCode? {
    // Checks to see if room code given is not empty and has length 8
    require(roomCode.isNotEmpty()) { "Room code must not be empty" }
    require(roomCode.length == 8) { "Room code must have length 8" }
    val acceptableChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    for (character in roomCode) {
        require(character in acceptableChars) { "Room code should only contain letters or numbers" }
    }

    return transaction(TransactionManager.defaultDatabase) {
        InUseCode.find { InUseTable.room_code eq roomCode }.firstOrNull()
    }
}

/**
 * Retrieves Game associated with given InUseCode record. Required in some parts of Routing.kt due to init errors.
 * Exposed DAO is shite.
 */
fun retrieveInUseCodeGame(inUseCode: InUseCode): Game? {
    require(retrieveInUseCode(inUseCode.room_code) != null ) { "InUseCode must exist" }
    return transaction(TransactionManager.defaultDatabase) {
        Game.find { GameTable.id eq inUseCode.game.id }.firstOrNull()
    }
}

/**
 * Delete InUseCode record from InUseCodeTable using given game object.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 */
fun deleteInUseCode(game: Game) {
    require(retrieveGame(game.id.value) != null) { "Game must exist" }
    transaction(TransactionManager.defaultDatabase) {                                                                   //delete room code
        InUseCode.find { InUseTable.game eq game.id }.firstOrNull()?.delete()
    }
}

/**
 * Creates and adds a new UserScore record to UserScoreTable
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 */
fun addUserScore(game: Game, user: User): UserScore {
    // Checks to see if the given game and user exists
    require(retrieveGame(game.id.value) != null) { "Game must exist" }
    require(retrieveUser(user.username) != null) { "User must exist" }

    return transaction(TransactionManager.defaultDatabase) {
        UserScore.new {
            this.game = game
            this.user = user
            this.score = 0
        }
    }
}

/**
 * Retrieves UserScore from UserScoreTable, returns null if UserScore doesn't exist.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 */
fun retrieveUserScore(game: Game, user: User): UserScore? {
    require(retrieveGame(game.id.value) != null) { "Game must exist" }
    require(retrieveUser(user.username) != null) { "User must exist" }

    return transaction(TransactionManager.defaultDatabase) {
        UserScore.find {
            UserScoreTable.game eq game.id and (UserScoreTable.user eq user.id)
        }.firstOrNull()
    }
}

/**
 * Retrieves all UserScores from a given Game
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 */
fun retrieveAllUserScoresDescending(game: Game): List<Triple<Int, String, Int>> {
    require(retrieveGame(game.id.value) != null) { "Game must exist" }
    return transaction(TransactionManager.defaultDatabase) {
        UserScore.find { UserScoreTable.game eq game.id }
            .sortedByDescending { it.score }
            .map { userScore ->
                Triple(userScore.user.id.value, userScore.user.username, userScore.score)
        }
    }
}

/**
 * Retrieve all quizzes a user has played (or attempted to play) and return the quiz name and the user's score.
 * Returns a list of UserAttemptView objects, can be empty.
 */
fun retrieveUserAttempts(user: User): List<UserAttemptView> {
    require(retrieveUser(user.username) != null) { "User must exist" }
    return transaction(TransactionManager.defaultDatabase) {
        UserScore.find { UserScoreTable.user eq user.id }
            .map {
                UserAttemptView(
                    quizName = it.game.quiz.quiz_name,
                    score = it.score
                )
            }
    }
}