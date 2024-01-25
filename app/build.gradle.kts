import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.util.*

plugins {
    id("common-conventions")
    id("kotlin-conventions")
    id("xyz.jpenilla.gremlin-gradle")
    application
}

fun DependencyHandler.runtimeDownloadApi(dependencyNotation: Any) {
    api(dependencyNotation)
    runtimeDownload(dependencyNotation)
}

fun DependencyHandler.runtimeDownloadOnlyApi(dependencyNotation: Any) {
    compileOnlyApi(dependencyNotation)
    runtimeDownload(dependencyNotation)
}

val operatingSystem: OperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-cli-jvm:0.3.6")
    runtimeDownloadOnlyApi("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    runtimeDownloadOnlyApi("io.javalin:javalin:6.0.0-SNAPSHOT")
    runtimeDownloadOnlyApi("com.github.ben-manes.caffeine:caffeine:3.1.8")
    runtimeDownloadOnlyApi("org.spongepowered:configurate-jackson:4.2.0-SNAPSHOT")
    runtimeDownloadOnlyApi("org.spongepowered:configurate-hocon:4.2.0-SNAPSHOT")
    implementation(libs.gremlin.runtime)
    runtimeDownloadOnlyApi("org.spongepowered:configurate-extra-kotlin:4.2.0-SNAPSHOT")
    runtimeDownloadOnlyApi("com.aayushatharva.brotli4j:brotli4j:1.13.0")
    runtimeDownloadOnlyApi("com.aayushatharva.brotli4j:native-windows-x86_64:1.16.0")
    runtimeDownloadOnlyApi("com.aayushatharva.brotli4j:native-osx-aarch64:1.13.0")
    runtimeDownloadOnlyApi("com.aayushatharva.brotli4j:native-osx-x86_64:1.13.0")
    runtimeDownloadOnlyApi("com.aayushatharva.brotli4j:native-linux-armv7:1.13.0")
    runtimeDownloadOnlyApi("com.aayushatharva.brotli4j:native-linux-aarch64:1.13.0")
    runtimeDownloadOnlyApi("com.aayushatharva.brotli4j:native-linux-x86_64:1.13.0")
}

applyJarMetadata("imagery-app")

application {
    mainClass.set("dev.mizule.imagery.app.launcher.Launcher")
}

tasks {
    clean {
        delete("run")
    }

    runShadow {
        workingDir = file("run").also(File::mkdirs)
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

tasks.writeDependencies {
    outputFileName.set("imagery-dependencies.txt")
    repos.add("https://repo.papermc.io/repository/maven-public/")
    repos.add("https://repo.maven.apache.org/maven2/")
    repos.add("https://maven.mizule.dev/")
    repos.add("https://maven.reposilite.com/snapshots/")
    repos.add("https://maven.reposilite.com/releases/")
}

gremlin {
    defaultJarRelocatorDependencies.set(true)
    defaultGremlinRuntimeDependency.set(false)
}

configurations.runtimeDownload {
    exclude("org.checkerframework", "checker-qual")
    exclude("org.jetbrains", "annotations")
}
