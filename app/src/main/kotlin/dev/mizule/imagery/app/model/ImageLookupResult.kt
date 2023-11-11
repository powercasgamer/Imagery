package dev.mizule.imagery.app.model

data class ImageLookupResult(
    val url: String,
    val delete: String = "$url/del",
)
