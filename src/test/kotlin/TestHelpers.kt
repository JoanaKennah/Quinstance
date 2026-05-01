package com.example

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters

//checks for a 200 OK and correct landing
fun checkForHtml(response: HttpResponse) {
    response.status shouldBe HttpStatusCode.OK
    response.headers["Content-Type"] shouldContain "text/html"
}

//Checks for a 302 found and correct landing
fun checkForRedirect(response: HttpResponse, expectedLocation: String) {
    response.status shouldBe HttpStatusCode.Found
    response.headers["Location"] shouldContain expectedLocation
}

//handles login request for some tests (inc auth. get routes)
suspend fun preLogin(client: HttpClient, username: String, password: String) {
    client.post("/login") {
        setBody(FormDataContent(Parameters.build {
            append("username", username)
            append("password", password)
        }))
    }
}