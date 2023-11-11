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
