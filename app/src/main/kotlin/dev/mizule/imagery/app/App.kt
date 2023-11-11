package dev.mizule.imagery.app

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.gson.Gson
import com.google.gson.JsonObject
import dev.mizule.imagery.app.model.UploadedFile
import io.javalin.Javalin
import io.javalin.http.ContentType
import io.javalin.http.HttpStatus
import io.javalin.json.JavalinGson
import org.eclipse.jetty.http.MimeTypes
import org.spongepowered.configurate.BasicConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.gson.GsonConfigurationLoader
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.Path
import kotlin.io.path.exists

class App {
    private val gson = Gson()
    private lateinit var config: BasicConfigurationNode
    private lateinit var loader: GsonConfigurationLoader
    private val javalin: Javalin by lazy {
        Javalin.create {
            it.jsonMapper(JavalinGson(gson))
            it.contextResolver.ip = { ctx ->
                ctx.header("CF-Connecting-IP")?: ctx.req().remoteAddr
            }
            it.router.ignoreTrailingSlashes = true
        }
    }
    private val path = Path(".")
    val storage: Path = path.resolve("storage")
    val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(15))
        .expireAfterAccess(Duration.ofMinutes(10))
        .build<String, Pair<UploadedFile, Path>>()

    fun load() {
        initializeStorage()

        this.loader = GsonConfigurationLoader.builder()
            .path(path.resolve("files.json"))
            .build()

        try {
            this.config = loader.load()
            loader.save(this.config)
        } catch (e: ConfigurateException) {
            throw RuntimeException(e)
        }

        configureEndpoints()
    }

    private fun initializeStorage() {
        if (!storage.exists()) {
            Files.createDirectories(storage)
        }
    }

    private fun configureEndpoints() {
        javalin.beforeMatched { ctx ->
            println(String.format("Received %s request from: %s:%s for %s", ctx.method(), ctx.ip(), ctx.port(), ctx.fullUrl()));
        }
        javalin.post("/upload") { ctx ->
            handleFileUpload(ctx)
        }

        javalin.get("{id}") { ctx ->
            serveUploadedFile(ctx)
        }
    }

    private fun handleFileUpload(ctx: io.javalin.http.Context) {
        ctx.uploadedFiles("file").first().also { file ->
            val fileName = getRandomString(8) + file.extension()
            val filePath = storage.resolve(fileName)
            Files.createFile(filePath)
            FileOutputStream(filePath.toFile()).use {
                file.content().copyTo(it)
            }
            val uploadedFile = UploadedFile(
                fileName,
                "user",
                System.currentTimeMillis(),
                file.filename(),
                file.extension(),
                MimeTypes.getDefaultMimeByExtension(file.extension())
            )
            config.node(uploadedFile.id).set(uploadedFile)
            try {
                loader.save(this.config)
            } catch (e: ConfigurateException) {
                throw RuntimeException(e)
            }
            cache.put(fileName, Pair(uploadedFile, filePath))
            ctx.json(JsonObject().apply {
                add("data", JsonObject().apply {
                    addProperty("url", "https://i.mizule.dev/$fileName")
                    addProperty("delete", "https://i.mizule.dev/$fileName/del")
                })
            })
        }
    }

    private fun serveUploadedFile(ctx: io.javalin.http.Context) {
        val path = cache.getIfPresent(ctx.pathParam("id"))

        if (path == null) {
            ctx.status(HttpStatus.NOT_FOUND)
        } else {
            val file = path.second.toFile()
            ctx.result(file.inputStream())
                .contentType(ContentType.getContentTypeByExtension(path.first.extension) ?: ContentType.IMAGE_PNG)
        }
    }

    fun enable() {
        javalin.start(5462)
    }

    fun disable() {
        // Add any necessary cleanup logic here
    }

    private fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
