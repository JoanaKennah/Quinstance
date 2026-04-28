package com.example.database.objects

import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import com.example.database.tables.UserTable

/**
 * User class connected to UserTable through IntEntityClass
 * @property username Unique identifier for User (not to be confused with id)
 * @property password Account password, will be hashed before storing
 * @property name Should contain firstname and lastname
 * @property email Account email address, format checked in .peb file
 * @property key_stage Age group of the user
 * @property role Account role (e.g. Student, Teacher, Admin)
 */
class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UserTable)

    var username by UserTable.username
    var password by UserTable.password
    var name by UserTable.name
    var email by UserTable.email
    var key_stage by UserTable.key_stage
    var role by UserTable.role

    override fun toString() = username
}