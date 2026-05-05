package com.example

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

fun sendResetEmail(toEmail: String) {
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
                subject = "Password Reset for Quinstance"

                setText(
                    """
                    Hello,

                    We have received a request that you would like to reset your password with Quinstance.
                    
                    Unfortunately, the way to do this requires sending you a link to reset.
                    
                    As this game is being ran on a local server, we need a localhost:8080 link. If we were to have an actual website and implement this in real life, we would be able to carry this out.

                    Thank you!
                    """.trimIndent(),
                )
            }

        Transport.send(message)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
