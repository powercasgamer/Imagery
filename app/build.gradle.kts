import com.diffplug.gradle.spotless.FormatExtension
import net.kyori.indra.licenser.spotless.HeaderFormat
import java.util.*

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("net.kyori.indra") version "3.1.3"
    id("net.kyori.indra.licenser.spotless") version "3.1.3"
    id("net.kyori.indra.git") version "3.1.3"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.7"
    `java-library`
    application
}

repositories {
    mavenCentral()
    sonatype.s01Snapshots()
    sonatype.ossSnapshots()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-cli-jvm:0.3.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("io.javalin:javalin:6.0.0-beta.2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.spongepowered:configurate-jackson:4.2.0-SNAPSHOT")
    implementation("org.spongepowered:configurate-hocon:4.2.0-SNAPSHOT")
    implementation("org.spongepowered:configurate-extra-kotlin:4.2.0-SNAPSHOT")
}

indra {
    javaVersions {
        target(17)
        minimumToolchain(17)
    }
    mitLicense()
}

spotless {
    fun FormatExtension.applyCommon() {
        trimTrailingWhitespace()
        endWithNewline()
        encoding("UTF-8")
        toggleOffOn()
    }
    java {
        importOrderFile(rootProject.file(".spotless/mizule.importorder"))
        removeUnusedImports()
        formatAnnotations()
        applyCommon()
        target("*/src/*/java/**/*.java")
    }
    kotlinGradle {
        applyCommon()
        ktlint("0.50.0")
    }
    kotlin {
        applyCommon()
        ktlint("0.50.0")
    }
}

indraSpotlessLicenser {
    headerFormat(HeaderFormat.starSlash())
    licenseHeaderFile(rootProject.projectDir.resolve("HEADER"))

    val currentYear = Calendar.getInstance().apply {
        time = Date()
    }.get(Calendar.YEAR)
    val createdYear = providers.gradleProperty("createdYear").map { it.toInt() }.getOrElse(currentYear)
    val year = if (createdYear == currentYear) createdYear.toString() else "$createdYear-$currentYear"

    property("name", providers.gradleProperty("projectName").getOrElse("template"))
    property("year", year)
    property("description", project.description ?: "A template project")
    property("author", providers.gradleProperty("projectAuthor").getOrElse("template"))
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
