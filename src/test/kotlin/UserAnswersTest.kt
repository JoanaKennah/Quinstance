package com.example

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class UserAnswersTest :
    StringSpec({

        // clears answers so tests don't interfere with each other
        beforeTest {
            UserAnswers.clearAnswers()
        }

        "addAnswer stores answer - valid answer posted and retrieved" {
            val answer = SentAnswerData(username = "linguin0", chosenAnswer = "testanswer", timeRemaining = 30)
            UserAnswers.addAnswer(answer)
            UserAnswers.allUserAnswers() shouldContain answer
            UserAnswers.allUserAnswers().size shouldBe 1
        }

        "addAnswer throws IllegalArgumentException for duplicate answer" {
            val answer = SentAnswerData(username = "linguin0", chosenAnswer = "testanswer2", timeRemaining = 30)
            UserAnswers.addAnswer(answer)
            shouldThrow<IllegalArgumentException> {
                UserAnswers.addAnswer(answer)
            }
        }

        "clearAnswers clears listof answers - empties list" {
            val answer = SentAnswerData(username = "linguin0", chosenAnswer = "testanswer3", timeRemaining = 30)
            UserAnswers.addAnswer(answer)
            UserAnswers.clearAnswers()
            UserAnswers.allUserAnswers().size shouldBe 0
        }

        "allUserAnswers returns all answers -correct list size" {
            val answerOne = SentAnswerData(username = "linguin0", chosenAnswer = "testanswer4", timeRemaining = 30)
            val answerTwo = SentAnswerData(username = "JoanaKennah", chosenAnswer = "testanswer5", timeRemaining = 30)
            UserAnswers.addAnswer(answerOne)
            UserAnswers.addAnswer(answerTwo)
            UserAnswers.allUserAnswers().size shouldBe 2
            UserAnswers.allUserAnswers() shouldContain answerOne
            UserAnswers.allUserAnswers() shouldContain answerTwo
        }

        "allUserAnswers returns empty when no answers added" {
            UserAnswers.allUserAnswers().size shouldBe 0
        }
    })
