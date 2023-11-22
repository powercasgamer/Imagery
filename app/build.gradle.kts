import org.gradle.nativeplatform.platform.internal.Architectures
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.util.*

plugins {
    id("common-conventions")
    id("kotlin-conventions")
    id("xyz.jpenilla.gremlin-gradle")
    application
}

val operatingSystem: OperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()

dependencies {
    implementation(kotlin("stdlib"))
    runtimeDownload(kotlin("reflect"))
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-cli-jvm:0.3.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("io.javalin:javalin:6.0.0-SNAPSHOT")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.spongepowered:configurate-jackson:4.2.0-SNAPSHOT")
    implementation("org.spongepowered:configurate-hocon:4.2.0-SNAPSHOT")
    implementation(libs.gremlin.runtime)
    implementation("org.spongepowered:configurate-extra-kotlin:4.2.0-SNAPSHOT")
    runtimeDownload("com.aayushatharva.brotli4j:brotli4j:1.13.0")
    runtimeDownload(
        "com.aayushatharva.brotli4j:native-" +
            if (operatingSystem.isWindows) {
                "windows-x86_64"
            } else if (operatingSystem.isMacOsX) {
                if (DefaultNativePlatform.getCurrentArchitecture().isArm()) {
                    "osx-aarch64"
                } else {
                    "osx-x86_64"
                }
            } else if (operatingSystem.isLinux) {
                if (Architectures.ARM_V7.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) {
                    "linux-armv7"
                } else if (Architectures.AARCH64.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) {
                    "linux-aarch64"
                } else if (Architectures.X86_64.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) {
                    "linux-x86_64"
                } else {
                    throw IllegalStateException("Unsupported architecture: ${DefaultNativePlatform.getCurrentArchitecture().name}")
                }
            } else {
                throw IllegalStateException("Unsupported operating system: $operatingSystem")
            } + ":1.13.0",
    )
}

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
}

gremlin {
    defaultJarRelocatorDependencies.set(true)
    defaultGremlinRuntimeDependency.set(false)
}
