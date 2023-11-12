package dev.mizule.imagery.app.model

import io.javalin.security.RouteRole

enum class Roles : RouteRole {

    PUBLIC,
    PRIVATE
}