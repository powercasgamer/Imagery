package dev.mizule.imagery.app.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class Config(
    @Comment("The port to start the application on")
    val port: Int = 8052,

    @Comment("The domain that this will be on")
    val domain: String = "https://example.com"
)
