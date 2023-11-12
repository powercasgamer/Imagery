package dev.mizule.imagery.app.exceptions

import io.javalin.http.HttpResponseException
import io.javalin.http.HttpStatus

class FileNotFoundResponse @JvmOverloads constructor(
    message: String = "This file does not exist",
    details: Map<String, String> = mapOf(),
) : HttpResponseException(HttpStatus.NOT_FOUND, message, details)
