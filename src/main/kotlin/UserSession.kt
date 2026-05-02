package com.example

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val username: String,
    val role: String,
    val theme: String = "default",
)
