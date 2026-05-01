package com.example.database.objects

import com.example.database.tables.InUseTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class InUseCode(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<InUseCode>(InUseTable)

    var room_code by InUseTable.room_code
    var game by Game referencedOn InUseTable.game

    override fun toString() = room_code
}
