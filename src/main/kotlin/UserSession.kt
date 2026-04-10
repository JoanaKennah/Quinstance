package com.example

import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable

@Serializable
data class UserSession(val username: String, val role: String)