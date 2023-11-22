import com.diffplug.gradle.spotless.FormatExtension
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import net.kyori.indra.licenser.spotless.HeaderFormat
import java.util.*

plugins {
    id("base-conventions")
    id("net.kyori.indra")
    id("net.kyori.indra.git")
    id("net.kyori.indra.licenser.spotless")
    id("io.github.goooler.shadow")
    id("java-library")
}

//val libs = extensions.getByType(org.gradle.accessors.dm.LibrariesForLibs::class)

//extensions.getByType(BasePluginExtension::class.java).archivesName.set(project.nameString())

indra {
    javaVersions {
        minimumToolchain(17)
        target(17)
    }
    mitLicense()

    publishSnapshotsTo("mizule", "https://repo.mizule.dev/snapshots")
    publishReleasesTo("mizule", "https://repo.mizule.dev/releases")
}

java {
    withSourcesJar()
    withJavadocJar()
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

tasks {
    assemble {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")

        mergeServiceFiles()

        transform(Log4j2PluginsCacheFileTransformer::class.java)
    }

    jar {
        archiveClassifier.set("unshaded")
        from(rootProject.projectDir.resolve("LICENSE")) {
            rename { "LICENSE_${providers.gradleProperty("projectName").getOrElse("template")}" }
        }
    }

    withType<JavaCompile>().configureEach {
        options.isFork = true
        options.isIncremental = true
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
        options.compilerArgs.add("-Xlint:-processing")
    }

    withType<ProcessResources>().configureEach {
        filteringCharset = "UTF-8"
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

     javadoc {
            val options = options as? StandardJavadocDocletOptions ?: return@javadoc
            options.isAuthor = true
            options.encoding = "UTF-8"
            options.charSet = "UTF-8"
            options.linkSource(true)
        }
}
