package com.example

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.pebble.Pebble
import io.ktor.server.routing.get
import io.pebbletemplates.pebble.loader.ClasspathLoader

fun Application.configureTemplating() {
    install(Pebble) {
        loader(
            ClasspathLoader().apply {
                prefix = "templates"
            },
        )
    }
}
