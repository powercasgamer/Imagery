@file:JvmName("Launcher")

package dev.mizule.imagery.app.launcher

import dev.mizule.imagery.app.App
import dev.mizule.imagery.app.config.Config
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapperFactory
import kotlin.io.path.Path
import kotlin.io.path.exists

fun main(args: Array<String>) {
    val parser = ArgParser("imagery")
    val path by parser.option(ArgType.String, shortName = "p", description = "The configuration file path, created if not present.")
        .default("./config.conf")

    parser.parse(args)

    val configPath = Path(path)
    val configLoader = HoconConfigurationLoader.builder()
        .path(configPath)
        .defaultOptions { options ->
            options.shouldCopyDefaults(true)
            options.serializers { builder ->
                builder.registerAnnotatedObjects(objectMapperFactory())
            }
        }
        .build()

    val configNode = configLoader.load()
    val config = requireNotNull(configNode.get<Config>()) {
        "Could not read configuration"
    }

    if (!configPath.exists()) {
        configNode.set(config) // update the backing node to add defaults
        configLoader.save(configNode)
    }

    val app = App(config)
    Runtime.getRuntime().addShutdownHook(Thread(app::stop))

    app.start()
}
