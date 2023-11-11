package dev.mizule.imagery.app.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class Config(
    @Comment("The port to start the application on.")
    val port: Int = 8052,

    @Comment("The base URL that this will be on, without trailing slashes.")
    val baseUrl: String = "https://i.mizule.dev",

    @Comment("The path to the file upload index.")
    val indexPath: String = "./files.json",

    @Comment("The path to the uploaded file storage directory.")
    val storagePath: String = "./storage"
)
