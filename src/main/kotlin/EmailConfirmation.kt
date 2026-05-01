package com.example

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.Session
import jakarta.mail.Transport
import java.util.Properties

fun sendConfirmationEmail(
    toEmail: String,
    firstName: String,
    lastName: String,
    username: String,
    password: String,
) {
    val host = "smtp.gmail.com"
    val from = "joanakennah@gmail.com"
    val passwordEmail = "lykavmaocerplzam"

    val props =
        Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", host)
            put("mail.smtp.port", "587")
        }

    val session =
        Session.getInstance(
            props,
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication = PasswordAuthentication(from, passwordEmail)
            },
        )

    try {
        val message =
            MimeMessage(session).apply {
                setFrom(InternetAddress(from))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "Welcome to Game Night Quizzes!"

                setText(
                    """
                    Hi $firstName $lastName,

                    Welcome to Game Night Quizzes!
                    Your account has been created successfully!

                    Username: $username

                    Have fun and thanks for signing up!
                    """.trimIndent(),
                )
            }

        Transport.send(message)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
