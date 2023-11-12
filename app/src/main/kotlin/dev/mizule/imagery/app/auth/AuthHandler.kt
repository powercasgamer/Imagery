package dev.mizule.imagery.app.auth

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
