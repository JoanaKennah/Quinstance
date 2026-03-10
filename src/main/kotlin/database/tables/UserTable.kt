package com.example.database.tables

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

/**
 * Table to store Users.
 * @property username unique identifier for User (not to be confused with id)
 * @property password account password, will be hashed before storing
 * @property name should contain firstname and lastname
 * @property email account email address, format checked in .peb file
 * @property key_stage age group of the user
 * @property role account role (e.g. Student, Teacher, Admin)
 */
object UserTable : IntIdTable() {
    val username = varchar("username", 20).uniqueIndex() // random length for now, unique
    val password = varchar("password", 255) // will be hashed
    val name = varchar("name", 20)
    val email = varchar("email", 20)
    val key_stage = varchar("key_stage", 20)
    val role = varchar("role", 7)
}