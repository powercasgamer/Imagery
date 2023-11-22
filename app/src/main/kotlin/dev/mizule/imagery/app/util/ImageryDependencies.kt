package dev.mizule.imagery.app.util

import org.slf4j.LoggerFactory
import xyz.jpenilla.gremlin.runtime.DependencyCache
import xyz.jpenilla.gremlin.runtime.DependencyResolver
import xyz.jpenilla.gremlin.runtime.DependencySet
import java.nio.file.Path

class ImageryDependencies {

    fun resolve(cacheDir: Path): Set<Path> {
        val deps = DependencySet.readFromClasspathResource(
            ImageryDependencies::class.java.classLoader, "imagery-dependencies.txt"
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