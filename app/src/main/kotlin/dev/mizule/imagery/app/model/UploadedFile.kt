package dev.mizule.imagery.app.model

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class UploadedFile(
    val id: String,
    val user: String,
    val uploadedDate: Long,
    val fileName: String,
    val extension: String,
    val mimeType: String,
)
