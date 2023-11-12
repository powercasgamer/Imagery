package dev.mizule.imagery.app.auth

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class UserConfig(

    val users: MutableList<User> = mutableListOf(),
)
