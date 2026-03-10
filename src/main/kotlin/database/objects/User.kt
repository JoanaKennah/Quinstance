package com.example.database.objects

import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import com.example.database.tables.UserTable

/**
 * User class connected to UserTable through IntEntityClass
 * @property username unique identifier for User (not to be confused with id)
 * @property password account password, will be hashed before storing
 * @property name should contain firstname and lastname
 * @property email_address account email address, format checked in .peb file
 * @property type type of account (e.g. Student, Teacher, Admin)
 */
class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(UserTable)

    var username by UserTable.username
    var password by UserTable.password
    var name by UserTable.name
    var email_address by UserTable.email_address
    var type by UserTable.type

    override fun toString() = username
}