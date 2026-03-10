package com.example.database.tables

import com.example.database.tables.SessionTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object InUseTable : IntIdTable() {
    val room_code = varchar("room_code", 8)
    val sessionID = reference("sessionID", SessionTable.id, ReferenceOption.CASCADE)
}