package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty

class MathsQuestionsTest :
    StringSpec({

        "MathsQuestions.questions is not empty - CSV loaded correctly" {
            MathsQuestions.questions.shouldNotBeEmpty()
        }

        "MathsQuestions.questions, each question has non-empty fields - CSV parsed correctly" {
            for (question in MathsQuestions.questions) {
                question.question.shouldNotBeEmpty()
                question.correctAnswer.shouldNotBeEmpty()
                question.option2.shouldNotBeEmpty()
                question.option3.shouldNotBeEmpty()
                question.option4.shouldNotBeEmpty()
            }
        }

        "MathsQuestions.questions each question text contains a maths operator - questions are actual maths questions" {
            val operators = listOf("+", "-", "×", "÷")
            for (question in MathsQuestions.questions) {
                operators.any { it in question.question } shouldBe true
            }
        }

        "MathsQuestions.questions each question has distinct answer options - no duplicate answers" {
            for (question in MathsQuestions.questions) {
                val options =
                    listOf(
                        question.correctAnswer,
                        question.option2,
                        question.option3,
                        question.option4,
                    )
                options.distinct().size shouldBe options.size
            }
        }

        "MathsQuestions.shuffleAnswers returns same number of questions as original - no questions lost on shuffle" {
            val shuffled = MathsQuestions.shuffleAnswers()
            shuffled.size shouldBe MathsQuestions.questions.size
        }

        "MathsQuestions.shuffleAnswers contains same questions as original - no questions added or removed on shuffle" {
            val shuffled = MathsQuestions.shuffleAnswers()
            shuffled shouldContainAll MathsQuestions.questions
        }
    })
