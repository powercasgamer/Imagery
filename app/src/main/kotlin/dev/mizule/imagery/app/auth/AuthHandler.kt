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
package dev.mizule.imagery.app.auth

import dev.mizule.imagery.app.config.UserConfig
import org.spongepowered.configurate.jackson.JacksonConfigurationLoader
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapperFactory
import java.security.SecureRandom
import java.util.Base64
import kotlin.io.path.Path
import kotlin.io.path.exists

class AuthHandler(userConfigPath: String) {

    private val usersMap: MutableMap<String, User> = mutableMapOf()

    val usersPath = Path(userConfigPath)
    val usersLoader = JacksonConfigurationLoader.builder()
        .path(usersPath)
        .defaultOptions { options ->
            options.shouldCopyDefaults(true)
            options.serializers { builder ->
                builder.registerAnnotatedObjects(objectMapperFactory())
            }
        }
        .build()

    val usersNode = usersLoader.load()
    val usersConfig = requireNotNull(usersNode.get<UserConfig>()) {
        "Could not read user configuration"
    }

    init {
        if (!usersPath.exists()) {
            usersNode.set(usersConfig) // update the backing node to add defaults
            usersLoader.save(usersNode)
        }
        loadUsers()
    }

    private fun loadUsers() {
        usersConfig.users.forEach {
            this.usersMap[it.username] = User(it.username, it.token)
        }
    }

    fun createUser(name: String): User {
        val secret = ByteArray(48)
        SecureRandom().nextBytes(secret)
        return createUser(name, Base64.getEncoder().encodeToString(secret))
    }

    fun isAuthorized(token: String): Boolean {
        return usersMap.values.any { it.token == token }
    }

    fun getUserByToken(token: String): User? {
        return usersMap.values.find { it.token == token }
    }

    fun createUser(name: String, token: String): User {
        val user = User(name, token)
        this.usersMap[name] to user
        this.usersConfig.users.add(user)
        usersNode.set(usersConfig)
        usersLoader.save(usersNode)
        return user
    }
}
