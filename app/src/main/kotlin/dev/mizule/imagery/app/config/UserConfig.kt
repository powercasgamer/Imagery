package dev.mizule.imagery.app.config

import dev.mizule.imagery.app.auth.User
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class UserConfig(

    val users: MutableList<User> = mutableListOf(),
)
