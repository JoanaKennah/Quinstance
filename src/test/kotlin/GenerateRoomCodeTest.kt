package com.example

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class GenerateRoomCodeTest :
    StringSpec({
        "generateRoomCode returns strlen 8 - correct length room code generated" {
            val code = generateRoomCode()
            code.length shouldBe 8
        }

        "generateRoomCode contains 0-9, A-Z only - correct chars in room code" {
            val code = generateRoomCode()
            val charList = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            code.forEach { char -> charList shouldContain char.toString() }
        }

        "generateRoomCode returns diff codes per call - randomness working on room code" {
            val codeOne = generateRoomCode()
            val codeTwo = generateRoomCode()
            codeOne shouldNotBe codeTwo
        }
    })
