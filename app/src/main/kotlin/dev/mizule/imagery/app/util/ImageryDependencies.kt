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
package dev.mizule.imagery.app.util

import org.slf4j.LoggerFactory
import xyz.jpenilla.gremlin.runtime.DependencyCache
import xyz.jpenilla.gremlin.runtime.DependencyResolver
import xyz.jpenilla.gremlin.runtime.DependencySet
import java.nio.file.Path

class ImageryDependencies {
    companion object {
        fun resolve(cacheDir: Path): Set<Path> {
            val deps = DependencySet.readFromClasspathResource(
                ImageryDependencies::class.java.classLoader,
                "imagery-dependencies.txt",
            )
            val cache = DependencyCache(cacheDir)
            val logger = LoggerFactory.getLogger(ImageryDependencies::class.java.simpleName)
            val files: Set<Path>
            DependencyResolver(logger).use { downloader ->
                files = downloader.resolve(deps, cache).jarFiles()
            }
            cache.cleanup()
            return files
        }
    }
}
