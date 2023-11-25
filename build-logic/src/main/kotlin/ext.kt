import net.kyori.indra.git.IndraGitExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.text.SimpleDateFormat
import java.util.*

val Project.libs: LibrariesForLibs
    get() = the()

fun Project.versionString(): String = this.version as String

fun Project.nameString(useProjectName: Boolean = false): String =
    if (useProjectName) {
        val projName = providers.gradleProperty("projectName").getOrElse("template")
        if (this.name.contains('-')) {
            val splat = this.name.split("-").toMutableList()
            splat[0] = projName
            splat.joinToString("-") { it.uppercaseFirstChar() }
        } else {
            projName
        }
    } else if (this.name.contains('-')) {
        this.name.split("-").joinToString("-") { it.uppercaseFirstChar() }
    } else {
        this.name.uppercaseFirstChar()
    }

fun Project.applyJarMetadata(moduleName: String) {
    if ("jar" in tasks.names) {
        tasks.named<Jar>("jar") {
            manifest.attributes(
                "Multi-Release" to "true",
                "Built-By" to System.getProperty("user.name"),
                "Created-By" to System.getProperty("java.vendor.version"),
                "Build-Jdk" to System.getProperty("java.version"),
                "Build-Time" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm.sssZ").format(Date()),
                "Automatic-Module-Name" to moduleName,
                "Specification-Title" to moduleName,
                "Specification-Version" to project.versionString(),
                "Specification-Vendor" to providers.gradleProperty("projectAuthor").getOrElse("athena"),
            )
            val indraGit = project.extensions.findByType<IndraGitExtension>()
            indraGit?.apply {
                applyVcsInformationToManifest(manifest)

                if (isPresent) {
                }
            }
        }
    }
}

fun ExternalModuleDependency.excludeUseless() {
    exclude(group = "org.jetbrains", module = "annotations")
    exclude(group = "org.checkerframework", module = "checker-qual")
    exclude(group = "com.google.errorprone", module = "error_prone_core")
    exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    exclude(group = "com.google.errorprone", module = "error_prone_annotation")
    exclude(group = "com.google.code.findbugs", module = "jsr305")
    exclude(group = "com.google.j2objc", module = "j2objc-annotations")
}

fun Configuration.excludeUseless() {
    exclude(group = "org.jetbrains", module = "annotations")
    exclude(group = "org.checkerframework", module = "checker-qual")
    exclude(group = "com.google.errorprone", module = "error_prone_core")
    exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    exclude(group = "com.google.errorprone", module = "error_prone_annotation")
    exclude(group = "com.google.code.findbugs", module = "jsr305")
    exclude(group = "com.google.j2objc", module = "j2objc-annotations")
}

/**
 * Returns `true` if a substring of this char sequence starting at the specified offset [startIndex] starts with any of the specified prefixes.
 */
fun CharSequence.startsWithAny(vararg prefixes: CharSequence, startIndex: Int = 0, ignoreCase: Boolean = false): Boolean {
    return prefixes.any { this.startsWith(it, startIndex, ignoreCase) }
}

/**
 * Returns `true` if this char sequence ends with any of the specified suffixes.
 */
fun CharSequence.endsWithAny(vararg suffixes: CharSequence, ignoreCase: Boolean = false): Boolean {
    return suffixes.any { this.endsWith(it, ignoreCase) }
}