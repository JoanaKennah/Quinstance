package com.example.database.objects

import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import com.example.database.tables.InUseTable
import com.example.database.tables.GameTable

class InUseCode(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<InUseCode>(InUseTable)

    var room_code by InUseTable.room_code
    var game by Game referencedOn InUseTable.game

    override fun toString() = room_code
}