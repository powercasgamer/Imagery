@file:JvmName("Launcher")
package dev.mizule.imagery.app.launcher

import dev.mizule.imagery.app.App

fun main() {
    val app = App()

    app.load()
    app.enable()
}
