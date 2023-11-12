/*
 * This file is part of Imagery, licensed under the MIT License.
 *
 * Copyright (c) 2023 powercas_gamer
 * Copyright (c) 2023 contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.mizule.imagery.app

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dev.mizule.imagery.app.auth.AuthHandler
import dev.mizule.imagery.app.config.Config
import dev.mizule.imagery.app.exceptions.FileNotFoundResponse
import dev.mizule.imagery.app.model.ImageLookupResult
import dev.mizule.imagery.app.model.Roles
import dev.mizule.imagery.app.model.UploadedFile
import io.github.oshai.kotlinlogging.KotlinLogging
import io.javalin.Javalin
import io.javalin.http.BadRequestResponse
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse
import io.javalin.json.JavalinJackson
import io.javalin.util.ConcurrencyUtil
import org.eclipse.jetty.http.MimeTypes
import org.spongepowered.configurate.jackson.JacksonConfigurationLoader
import org.spongepowered.configurate.kotlin.objectMapperFactory
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

private val logger = KotlinLogging.logger {}

class App(private val config: Config, usersConfigOption: String) {
    private val scheduler = ConcurrencyUtil.executorService("Imagery Scheduler", true)
    private val storageDir = Path(config.storagePath)
    private val dataLoader = JacksonConfigurationLoader.builder()
        .path(Path(config.indexPath))
        .defaultOptions { options ->
            options.shouldCopyDefaults(true)
            options.serializers { builder ->
                builder.registerAnnotatedObjects(objectMapperFactory())
            }
        }
        .build()
    private val authHandler = AuthHandler(usersConfigOption)

    private val dataNode = dataLoader.load()
    private val cache: Cache<String, FileCacheEntry> = Caffeine.newBuilder() // This is really not needed, but, yes.
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build()

    private val javalin = Javalin.create {
        it.jsonMapper(JavalinJackson(MAPPER))
        it.showJavalinBanner = false
        it.router.ignoreTrailingSlashes = true
        it.useVirtualThreads = true
        it.contextResolver.ip = { ctx ->
            ctx.header("CF-Connecting-IP") ?: ctx.req().remoteAddr
        }
    }

    init {
        storageDir.createDirectories()

        javalin.beforeMatched { ctx ->
            logger.info { "Received ${ctx.method()} request from: ${ctx.ip()}:${ctx.port()} for ${ctx.fullUrl()}" }
        }
        javalin.get("/{id}", ::serveUploadedFile)
        if (authHandler.usersConfig.users.isEmpty()) {
            authHandler.createUser("user")
        }
        javalin.beforeMatched("/upload") { ctx ->
            if (ctx.routeRoles().contains(Roles.PRIVATE)) {
                // check auth header
                val token = ctx.header("Authorization") ?: throw ForbiddenResponse()
                if (!authHandler.isAuthorized(token)) {
                    throw ForbiddenResponse()
                }
            }
        }
        javalin.post("/upload", ::handleFileUpload, Roles.PRIVATE)
    }

    private fun handleFileUpload(ctx: Context) {
        val file = ctx.uploadedFiles("file").firstOrNull() ?: throw BadRequestResponse()

        val id = getRandomString()
        val fileName = id + file.extension()
        val filePath = storageDir.resolve(fileName)
        filePath.outputStream().use {
            file.content().copyTo(it)
        }
        val token = ctx.header("Authorization") ?: throw ForbiddenResponse()

        val uploadedFile = UploadedFile(
            id,
            authHandler.getUserByToken(token)?.username ?: "Unknown",
            System.currentTimeMillis(),
            fileName,
            file.filename(),
            file.extension(),
            MimeTypes.getDefaultMimeByExtension(file.extension()),
        )

        scheduler.execute {
            dataNode.node(id).set(uploadedFile)
            dataLoader.save(dataNode) // TODO: probably shouldn't be done during the http request
        }

        cache.put(fileName, FileCacheEntry(uploadedFile, filePath))
        ctx.json(mapOf("data" to ImageLookupResult("${config.baseUrl}/$fileName")))
    }

    private fun serveUploadedFile(ctx: Context) {
        val id = ctx.pathParam("id")
        cache.get(id) {
            val node = dataNode.node(id)
            if (node != null && !node.virtual()) {
                val uploadedNode = node.get(UploadedFile::class.java)!!
                FileCacheEntry(uploadedNode, storageDir.resolve(uploadedNode.fileName))
            } else {
                // cry
                throw FileNotFoundResponse()
            }
        }?.also { (uploadedFile, path) ->
            ctx.result(path.inputStream())
                .contentType(ContentType.getContentTypeByExtension(uploadedFile.extension) ?: ContentType.IMAGE_PNG)
        } ?: throw FileNotFoundResponse()
    }

    fun start() {
        logger.info { "Starting HTTP server at port ${config.port}..." }
        javalin.start(config.port)
    }

    fun stop() {
        logger.info { "Shutting down..." }
    }

    companion object {
        private val MAPPER = jacksonObjectMapper()
        private val ALLOWED_CHARS = ('A'..'Z') + ('a'..'z') + ('0'..'9')

        fun getRandomString(length: Int = 8): String =
            generateSequence(ALLOWED_CHARS::random).take(length).joinToString("")
    }

    data class FileCacheEntry(val file: UploadedFile, val path: Path)
}
