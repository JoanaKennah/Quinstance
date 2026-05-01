package com.example

import com.example.database.ProjectDatabase
import com.example.database.objects.Answer
import com.example.database.objects.Game
import com.example.database.objects.InUseCode
import com.example.database.objects.Question
import com.example.database.objects.Quinstance
import com.example.database.objects.Quiz
import com.example.database.objects.User
import com.example.database.objects.UserScore
import com.example.database.tables.AnswerTable
import com.example.database.tables.GameTable
import com.example.database.tables.InUseTable
import com.example.database.tables.QuestionTable
import com.example.database.tables.QuinstanceTable
import com.example.database.tables.QuizTable
import com.example.database.tables.UserScoreTable
import com.example.database.tables.UserTable
import com.password4j.Password
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.text.isNotEmpty

//  All code here was written by a human :>

/*  Websites used for debugging/learning
    - https://kotlinlang.org
    - https://www.jetbrains.com/help/exposed
    - https://stackoverflow.com
 */

/**
 * Data class for a User's attempt at playing a Quiz.
 * @property quizName name of the quiz
 * @property score current user score for a Quiz
 */
data class UserQuizAttempt(
    val quizName: String,
    val score: Int,
)

/**
 * Information about a Player's score.
 * @property userID unique identifier for a User in the database
 * @property username unique custom name for a User
 * @property score current user score for a Quiz
 * @property isGhost True/False if a User is a ghost or not
 */
data class PlayerScoreInfo(
    val userID: Int,
    val username: String,
    val score: Int,
    val isGhost: Boolean,
)

/**
 * Creates all required tables if they do not already exist
 */
fun configureDatabase() {
    // Establishes connection from ProjectDatabase object
    val dbConnection = ProjectDatabase.database

    transaction(dbConnection) {
        SchemaUtils.create(
            AnswerTable,
            InUseTable,
            QuestionTable,
            QuinstanceTable,
            QuizTable,
            GameTable,
            UserScoreTable,
            UserTable,
        )
    }
}

/**
 * Retrieves user from UserTable using their username, returns null if no user found
 * @param username Unique identifier for a User
 * @return User object if User exists, null if they don't
 * @throws IllegalArgumentException if the username is empty.
 */
fun retrieveUser(username: String): User? {
    require(username.isNotEmpty()) { "User can't be empty" }
    return transaction(TransactionManager.defaultDatabase) {
        User.find { UserTable.username eq username }.firstOrNull()
    }
}

/**
 * Adds a new User to UserTable. Returns null if user already exists
 * Retrieves user from UserTable using their ID, returns null if no user found
 * @param userId Unique identifier for a User in the Database
 * @return User object if User exists, null if they don't
 * @throws IllegalArgumentException if userID is less than or equal to 0
 */
fun retrieveUserById(userId: Int): User? {
    require(userId >= 0) { "User ID must be positive" }

    return transaction(TransactionManager.defaultDatabase) {
        User.findById(userId)
    }
}

/**
 * Adds a new User to UserTable.
 * @param username Unique identifier for a User
 * @param password Plaintext password for a User to be hashed before being added to the Database
 * @param name First name and last name for a User, combined for Database purposes
 * @param keyStage Key stage for the User, e.g. Key Stage 1 or Further Education
 * @param role Role self-assigned, can either be Student or Teacher
 * @return User object if user was added to Database, null if user already exists
 * @throws IllegalArgumentException If any params are empty
 * @throws IllegalArgumentException If username or email is greater than 50 characters
 */
fun addUser(
    username: String,
    password: String,
    name: String,
    email: String,
    keyStage: String,
    role: String,
): User? {
    // Check to ensure all parameters are not empty
    for (parameter in listOf(username, password, name, email, keyStage, role)) {
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
                this.key_stage = keyStage
                this.role = role
            }
        }
    }
    return null
}

/**
 * Deletes given user from UserTable (possibly used for Admin purposes).
 * @param user User object
 **/
fun deleteUser(user: User) {
    // Checks if the user exists in the table before deleting it
    require(retrieveUser(user.username) != null) { "User must exist" }
    transaction(TransactionManager.defaultDatabase) {
        user.delete()
    }
}

/**
 * Creates and adds a new Question to QuestionTable. Increments given Quiz num_of_questions by 1
 * @param quiz Quiz object that will be associated with the new question
 * @param question question text
 * @param keyStage Key stage for the Question, e.g. Key Stage 1 or Further Education
 * @return new Question object
 * @throws IllegalArgumentException if any param is empty
 */
fun addQuestion(
    quiz: Quiz,
    question: String,
    keyStage: String,
): Question {
    for (parameter in listOf(question, keyStage)) {
        require(parameter.isNotEmpty())
    }
    val newQuestion =
        transaction(TransactionManager.defaultDatabase) {
            // Don't need to check if Question already exists
            val question =
                Question.new {
                    this.question = question
                    this.key_stage = keyStage
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
 * Takes each answer option the user inputted for their question and sets the correctAnswer to the answer indicated by
 * correctIndex and returns the remaining three options. Need for this found during testing of reading Csv files.
 * @param option1 First possible answer for a question, cannot be empty
 * @param option2 Second possible answer for a question, can be empty
 * @param option3 Third possible answer for a question, can be empty
 * @param option4 Fourth possible answer for a question, can be empty
 * @return listOf(correctOption, option2, option3?, option4?)
 * @throws IllegalArgumentException if option1 or option2 is empty, or if correctIndex isn't within 0 to 3.
 * @return listOf(correctOption, finalOption2, finalOption3, finalOption4)
 */
fun setAnswerOptions(
    option1: String,
    option2: String,
    option3: String? = null,
    option4: String? = null,
    correctIndex: Int,
): List<String?> {
    require(option1 != "") { "option1 cannot be empty" }
    require(option2 != "") { "option2 cannot be empty" }
    require(correctIndex in 0..3) { "correctIndex must be between 0 and 3" }

    val allOptions = listOfNotNull(option1, option2, option3, option4)
    val correctOption = allOptions[correctIndex]
    var finalOption2 = ""
    var finalOption3: String? = null
    var finalOption4: String? = null

    // Not happy about this implementation but avoids issues found in developing csv reading. Ensures there are no duplicate answers.
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
    return listOf(correctOption, finalOption2, finalOption3, finalOption4)
}

/**
 * Edits given Question and Answer in the Database.
 * @param question Question object to be edited
 * @param questionData map containing new/edited data for Question. Includes two keys, "question" and "keyStage"
 * @param answer Answer object to be edited
 * @param answerData map containing new/edited data for Answer. Includes five keys, "option1", "option2", "option3",
 * "option4", "correctIndex"
 * @throws IllegalArgumentException if questionData or answerData is empty.
 * @throws IllegalArgumentException if question given doesn't exist
 * @throws IllegalArgumentException if answer given doesn't exist or the answer is not associated with the question.
 */
fun editQuestion(
    question: Question,
    questionData: Map<String, String>,
    answer: Answer,
    answerData: Map<String, String?>,
) {
    require(questionData.isNotEmpty() && answerData.isNotEmpty()) { "Nothing to edit" }
    transaction(TransactionManager.defaultDatabase) {
        require(Question.findById(question.id) != null) { "Question must exist " }
        require(Answer.findById(answer.id) != null) { "Answer must exist" }
        require(answer.question.id == question.id) { "Answer must be for the given Question " }
    }

    for (item in questionData) {
        require(item.key in listOf("question", "keyStage")) { "questionData key must be an existing attribute" }
    }
    for (item in answerData) {
        require(
            item.key in listOf("option1", "option2", "option3", "option4", "correctIndex"),
        ) { "answerData key must be an existing attribute" }
        if (item.key == "correctIndex") {
            require(item.value?.toIntOrNull() != null) { "correctIndex must be an integer" }
            require(item.value?.toInt() in 0..3) { "correctIndex must in range 0..3" }
        }
    }

    transaction(TransactionManager.defaultDatabase) {
        if (questionData.isNotEmpty()) {
            questionData.forEach {
                when (it.key) {
                    "question" -> question.question = it.value
                    "keyStage" -> question.key_stage = it.value
                }
            }
        }

        if (answerData.isNotEmpty()) {
            val answerOptions =
                setAnswerOptions(
                    answerData["option1"]!!,
                    answerData["option2"]!!,
                    answerData["option3"],
                    answerData["option4"],
                    answerData["correctIndex"]!!.toInt(),
                )

            answerData.forEach {
                when (it.key) {
                    "correctIndex" -> answer.correct_answer = answerOptions[0]!!
                    "option2" -> answer.option2 = answerOptions[1]!!
                    "option3" -> answer.option3 = answerOptions[2]
                    "option4" -> answer.option4 = answerOptions[3]
                }
            }
        }
    }
}

/**
 * Deletes given question from QuestionTable. Returns true if the question is successfully deleted
 * Created by Minghao in Routing.kt, refactored and edited in Database.kt for testing purposes.
 * @param question Question object to be deleted
 * @return True/False if the question has been successfully deleted or not
 */
fun deleteQuestion(question: Question): Boolean {
    return transaction(TransactionManager.defaultDatabase) {
        val foundQuestion = Question.findById(question.id.value) ?: return@transaction false

        val quinstance = Quinstance.find { QuinstanceTable.question eq foundQuestion.id }.firstOrNull()
        val quiz = quinstance?.quiz

        Answer.find { AnswerTable.question eq foundQuestion.id }.firstOrNull()?.delete()
        quinstance?.delete()
        foundQuestion.delete()

        if (quiz != null && quiz.num_of_questions > 0) {
            quiz.num_of_questions -= 1
        }

        true
    }
}

/**
 * Retrieves all Questions associated with the given Quiz. If there are no questions associated with the quiz,
 * a null value is returned.
 * @param quiz Quiz object to retrieve Questions from
 * @return List of Question objects
 * @throws IllegalArgumentException If Quiz does not exist
 */
fun retrieveQuizQuestions(quiz: Quiz): MutableList<Question> {
    // Find all question ID's associated with the given Quiz
    return transaction(TransactionManager.defaultDatabase) {
        // Check to see if quiz actually exists
        require((Quiz.findById(quiz.id))?.id == quiz.id) { "Quiz received does not exist" }

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
 * @param user User object who owns the Quiz-to-be
 * @param quizName Name of the Quiz (ADD LENGTH LIMIT)
 * @param description Quiz description (ADD LENGTH LIMIT)
 * @param category Quiz category selected from a given list
 * @param keyStage Key stage for the Question, e.g. Key Stage 1 or Further Education
 * @param numOfQuestions number of questions in the Quiz, initialised to 0
 * @throws IllegalArgumentException If User doesn't exist
 * @throws IllegalArgumentException If User doesn't exist
 */
fun addQuiz(
    user: User,
    quizName: String,
    description: String,
    category: String,
    keyStage: String,
    numOfQuestions: Int = 0,
): Quiz {
    // Checks the User exists
    require(retrieveUser(user.username) != null) { "User must not be null" }
    require(numOfQuestions >= 0) { "Number of questions must be greater than or equal to 0" }
    // Checks string parameters are no empty
    for (parameter in listOf(quizName, description, category, keyStage)) {
        require(parameter.isNotEmpty()) { "Parameter $parameter must not be empty" }
    }
    // Don't need to check if Quiz already exists
    return transaction(TransactionManager.defaultDatabase) {
        Quiz.new {
            this.user = user
            this.quiz_name = quizName
            this.description = description
            this.category = category
            this.key_stage = keyStage
            this.num_of_questions = numOfQuestions
        }
    }
}

/**
 * Deletes given Quiz from QuizTable.
 * @param quiz Quiz object to be deleted
 * @return True if Quiz is successfully deleted, False if it doesn't exist
 */
fun deleteQuiz(quiz: Quiz): Boolean {
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
 * @param quizID Unique identifier for Quiz object in Database to be found
 * @return Quiz if found, null otherwise
 * @throws IllegalArgumentException If quizId is less than or equal to 0
 */
fun retrieveQuiz(quizID: Int): Quiz? {
    require(quizID > 0) { "QuizID must be greater than or equal to 0" }
    return transaction(TransactionManager.defaultDatabase) {
        Quiz.findById(quizID)
    }
}

/**
 * Retrieves Quiz from QuizTable using a gameID, returns null if no Quiz found.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 * @param gameID Unique identifier for Game object in Database
 * @return Retrieved Quiz if found, null otherwise
 * @throws IllegalArgumentException If gameID is less than or equal to 0
 */
fun retrieveQuizFromGameID(gameID: Int): Quiz? {
    // Check to see if given game actually exists
    require(gameID > 0) { "GameID must be non-negative" }
    return transaction(TransactionManager.defaultDatabase) {
        val game = retrieveGame(gameID)
        return@transaction game?.quiz
    }
}

/**
 * Retrieve all Quizzes that a User has created. Will return null if the User passed in does not exist. An empty list
 * can be returned if the User has no quizzes.
 * @param username Unique custom identifier for a User
 * @return List of Quiz objects associated with the User or null if the User doesn't exist
 */
fun retrieveUserQuizzes(username: String): MutableList<Quiz>? {
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
 * @param question Question object for the Answer to be associated with
 * @param option1 First possible answer for a question, cannot be empty
 * @param option2 Second possible answer for a question, can be empty
 * @param option3 Third possible answer for a question, can be empty
 * @param option4 Fourth possible answer for a question, can be empty
 * @param correctIndex Integer from 0 to 3 indicating which option is correct
 * @return Answer object if successfully added, null if the Question already has an existing Answer object associated with it
 * @throws IllegalArgumentException If given Question doesn't exist
 * @throws IllegalArgumentException If option1 or option2 is empty and correctIndex isn't between 0 and 3
 * @throws IllegalArgumentException If the correctIndex indicates an empty answer is the correct option
 */
fun addAnswer(
    question: Question,
    option1: String,
    option2: String,
    option3: String? = null,
    option4: String? = null,
    correctIndex: Int,
): Answer? {
    // Slightly changed this to accept 4 answers then flag the correct one, helped make the frontend a bit cleaner
    transaction(TransactionManager.defaultDatabase) {
        require((Question.findById(question.id))?.id == question.id) { "Question given does not exist" }
    }
    require(option1.isNotEmpty()) { "Option 1 must not be empty" }
    require(option2.isNotEmpty()) { "Option 2 must not be empty" }
    require(correctIndex in 0..3) { "CorrectIndex must be between 0 and 3" }
    require(listOf(option1, option2, option3, option4)[correctIndex] != null) { "Correct answer cannot be null" }

    // Check to see if an Answer already exists (Question and Answer have 1:1 relationship), return null if Answer already exists
    return transaction(TransactionManager.defaultDatabase) {
        val check = Answer.find { AnswerTable.question eq question.id }
        if (check.firstOrNull() != null) return@transaction null

        val answers = setAnswerOptions(option1, option2, option3, option4, correctIndex)

        val newAnswer =
            Answer.new {
                this.question = question
                this.correct_answer = answers[0]!!
                this.option2 = answers[1]!!
                this.option3 = answers[2]
                this.option4 = answers[3]
            }
        return@transaction newAnswer
    }
}

/**
 * Retrieves Answer for given Question
 * @param question Question object to receive Answer from
 * @return Retrieved Answer object if it exists, null otherwise
 * @throws IllegalArgumentException If Question given doesn't exist
 */
fun retrieveQuestionAnswer(question: Question): Answer? {
    return transaction(TransactionManager.defaultDatabase) {
        // Check to see if quiz given actually exists
        require((Question.findById(question.id))?.id == question.id) { "Question given does not exist" }

        val foundAnswer = Answer.find(AnswerTable.question eq question.id)
        // Return null if there is no Answer to this Question
        val answer = foundAnswer.firstOrNull() ?: return@transaction null
        return@transaction answer
    }
}

/**
 * Retrieves Game from GameTable using its gameID, returns null if no Game found.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 * @param gameID Unique identifier for the Game object in Database to be received
 * @return Retrieved Game object if it exists, null otherwise
 * @throws IllegalArgumentException If gameID is less than or equal to 0
 */
fun retrieveGame(gameID: Int): Game? {
    // Check given gameID is valid
    require(gameID > 0) { "GameID must be non-negative" }
    return transaction(TransactionManager.defaultDatabase) {
        Game.findById(gameID)
    }
}

/**
 * Creates and adds a new Game to GameTable.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 * @param quiz Quiz object to be associated with new Game object
 * @return Created Game object
 * @throws IllegalArgumentException if Quiz does not exist
 */
fun addGame(quiz: Quiz): Game {
    // Check to see if given quiz actually exists
    require(retrieveQuiz(quiz.id.value) != null) { "Quiz must exist" }
    return transaction(TransactionManager.defaultDatabase) {
        Game.new {
            this.quiz = quiz
            this.timeStamp =
                java.time.LocalDateTime
                    .now()
                    .toString()
        }
    }
}

/**
 * Retrieve all players from an existing Game and return them inside a List.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 * @param game Game object to receive players from
 * @return List of User usernames
 * @throws IllegalArgumentException If Game given does not exist
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
 * @param roomCode Unique code associated with an ongoing Game
 * @return True if the room code already exists, False otherwise
 * @throws IllegalArgumentException If the given room code is empty or does not have length 8
 * @throws IllegalArgumentException If the given room code contains anything other than letters or numbers
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
 * @param roomCode Unique code associated with an ongoing Game
 * @param game Game object to be associated with the given room code
 * @return InUseCode object
 * @throws IllegalArgumentException If the given Game does not exist
 * @throws IllegalArgumentException If the given room code is empty or contains characters that aren't letters or numbers
 * @throws IllegalArgumentException If the given room code doesn't have a length of 8 or already exists
 */
fun addInUseCode(
    roomCode: String,
    game: Game,
): InUseCode {
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
 * @param game Game object to retrieve room code for
 * @return Room code associated with the given Game or null if it doesn't exist
 * @throws IllegalArgumentException If the given Game doesn't exist
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
 * @param roomCode Unique identifier for an ongoing Game
 * @return InUseCode object associated with given room code or null if one doesn't exist
 * @throws IllegalArgumentException If the given room code is empty or contains characters that aren't letters or numbers
 * @throws IllegalArgumentException If the given room code doesn't have a length of 8
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
 * @param inUseCode InUseCode object associated with the wanted Game
 * @return Game object if it exists, null otherwise
 * @throws IllegalArgumentException if inUseCode doesn't exist
 */
fun retrieveInUseCodeGame(inUseCode: InUseCode): Game? {
    require(retrieveInUseCode(inUseCode.room_code) != null) { "InUseCode must exist" }
    return transaction(TransactionManager.defaultDatabase) {
        Game.find { GameTable.id eq inUseCode.game.id }.firstOrNull()
    }
}

/**
 * Delete InUseCode record from InUseCodeTable using given game object.
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 * @param game Game object associated with the InUseCode to be deleted
 * @throws IllegalArgumentException If Game given does not exist
 */
fun deleteInUseCode(game: Game) {
    require(retrieveGame(game.id.value) != null) { "Game must exist" }
    transaction(TransactionManager.defaultDatabase) {
        InUseCode.find { InUseTable.game eq game.id }.firstOrNull()?.delete()
    }
}

/**
 * Creates and adds a new UserScore record to UserScoreTable
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 * @param game Game object
 * @param user User object
 * @return New UserScore object, with score attribute being set to 0
 * @throws IllegalArgumentException If given Game does not exist
 * @throws IllegalArgumentException If given User does not exist
 *
 */
fun addUserScore(
    game: Game,
    user: User,
): UserScore {
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
 * @param game Game object associated with the wanted UserScore
 * @param user User object associated with the wanted UserScore
 * @return UserScore object if found, null otherwise
 * @throws IllegalArgumentException If given Game does not exist
 * @throws IllegalArgumentException If given User does not exist
 */
fun retrieveUserScore(
    game: Game,
    user: User,
): UserScore? {
    require(retrieveGame(game.id.value) != null) { "Game must exist" }
    require(retrieveUser(user.username) != null) { "User must exist" }

    return transaction(TransactionManager.defaultDatabase) {
        UserScore
            .find {
                UserScoreTable.game eq game.id and (UserScoreTable.user eq user.id)
            }.firstOrNull()
    }
}

/**
 * Retrieves all UserScores from a given Game
 * Created by Emily in Routing.kt, refactored to Database.kt for testing purposes.
 * @param game Game object needed to retrieve all associated UserScore objects
 * @return List of PlayerScoreInfo objects (can be empty)
 * @throws IllegalArgumentException If given Game does not exist
 */
fun retrieveAllUserScoresDescending(game: Game): List<PlayerScoreInfo> {
    require(retrieveGame(game.id.value) != null) { "Game must exist" }
    return transaction(TransactionManager.defaultDatabase) {
        UserScore
            .find { UserScoreTable.game eq game.id }
            .sortedByDescending { it.score }
            .map { userScore ->
                PlayerScoreInfo(userScore.user.id.value, userScore.user.username, userScore.score, userScore.isGhost)
            }
    }
}

/**
 * Deletes UserScore object for a given User.
 * Created by Emily in Routing.kt, refactored and edited in Database.kt for testing purposes.
 * @param game Game object associated with the UserScore to be deleted
 * @param user User object associated with the UserScore to be deleted
 * @return True if delete is successful, false if UserScore doesn't exist
 * @throws IllegalArgumentException If the Game or User doesn't exist
 */
fun deleteUserScoreForGivenUser(
    game: Game,
    user: User,
): Boolean {
    require(retrieveGame(game.id.value) != null) { "Game must exist" }
    require(retrieveUser(user.username) != null) { "User must exist" }
    val userScore =
        transaction(TransactionManager.defaultDatabase) {
            UserScore
                .find {
                    UserScoreTable.game eq game.id and (UserScoreTable.user eq user.id)
                }.firstOrNull()
        } ?: return false
    transaction(TransactionManager.defaultDatabase) { userScore.delete() }
    return true
}

/**
 * Deletes all UserScores for a given Game.
 * Created by Emily in Routing.kt, refactored and edited in Database.kt for testing purposes.
 * @param game Game object associated with UserScore objects to be deleted
 * @return True if delete is successful, false if there are no UserScores to delete
 * @throws IllegalArgumentException If the given Game does not exist
 */
fun deleteGameUserScores(game: Game): Boolean {
    require(retrieveGame(game.id.value) != null) { "Game must exist" }
    val userScores =
        transaction(TransactionManager.defaultDatabase) {
            // delete all user scores from this game
            UserScore.find { UserScoreTable.game eq game.id }.toList()
        }
    if (userScores.isEmpty()) return false
    transaction(TransactionManager.defaultDatabase) { userScores.forEach { it.delete() } }
    return true
}

/**
 * Retrieve all quizzes a user has played (or attempted to play) and return the quiz name and the user's score.
 * @param user User object
 * @return List of UserQuizAttempt objects (can be empty)
 * @throws IllegalArgumentException If given User does not exist
 */
fun retrieveUserQuizAttempts(user: User): List<UserQuizAttempt> {
    require(retrieveUser(user.username) != null) { "User must exist" }
    return transaction(TransactionManager.defaultDatabase) {
        UserScore
            .find { UserScoreTable.user eq user.id }
            .map {
                UserQuizAttempt(
                    quizName = it.game.quiz.quiz_name,
                    score = it.score,
                )
            }
    }
}
