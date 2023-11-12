package dev.mizule.imagery.app.auth

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class UserConfig(

    val users: MutableList<User> = mutableListOf()
)