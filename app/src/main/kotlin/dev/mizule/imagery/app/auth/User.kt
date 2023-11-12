package dev.mizule.imagery.app.auth

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class User(

    val username: String,

    val token: String,
)
