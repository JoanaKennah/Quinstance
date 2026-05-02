package com.example.database

import org.jetbrains.exposed.v1.jdbc.Database

object ProjectDatabase {
    const val URL = "jdbc:h2:./data"
    const val DRIVER = "org.h2.Driver"

    val database by lazy {
        Database.connect(URL, DRIVER)
    }
}
