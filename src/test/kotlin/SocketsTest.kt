package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SocketsTest :
    StringSpec({

        // Testing GameSession default values
        "GameSession current question index defaults to zero" {
            val session = GameSession(gameId = 1, hostUsername = "testHost", questions = emptyList())
            session.currentQuestionIndex shouldBe 0
        }

        "GameSession isAdvanced flag defaults to false" {
            val session = GameSession(gameId = 1, hostUsername = "testHost", questions = emptyList())
            session.isAdvanced shouldBe false
        }

        "GameSession isStarted flag defaults to false" {
            val session = GameSession(gameId = 1, hostUsername = "testHost", questions = emptyList())
            session.isStarted shouldBe false
        }

        "GameSession isFinished flag defaults to false" {
            val session = GameSession(gameId = 1, hostUsername = "testHost", questions = emptyList())
            session.isFinished shouldBe false
        }

        "GameSession connections set is empty when created" {
            val session = GameSession(gameId = 1, hostUsername = "testHost", questions = emptyList())
            session.connections shouldBe emptySet()
        }

        // testing activeSessions values and behaviour
        "activeSessions returns null for non-existent gameId" {
            activeSessions.clear()
            activeSessions[999] shouldBe null
        }

        "activeSessions session accessible at correct index after being added, and can hold multiple sessions" {
            activeSessions.clear()
            val session1 = GameSession(gameId = 1, hostUsername = "testHost", questions = emptyList())
            val session2 = GameSession(gameId = 2, hostUsername = "testHost", questions = emptyList())
            activeSessions[1] = session1
            activeSessions[2] = session2
            activeSessions[1] shouldBe session1
            activeSessions[2] shouldBe session2
            activeSessions.size shouldBe 2
        }

        "activeSessions session is null after being removed" {
            activeSessions.clear()
            val session = GameSession(gameId = 1, hostUsername = "testHost", questions = emptyList())
            activeSessions[1] = session
            activeSessions.remove(1)
            activeSessions[1] shouldBe null
        }
    })
