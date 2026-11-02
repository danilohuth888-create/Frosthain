plugins {
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // Der Prototyp zeichnet ausschließlich, was das UI-Modul berechnet (gleiche Logik wie später im Spiel).
    implementation(project(":ui"))
}

application {
    mainClass = "net.frosthain.prototype.PrototypeApp"
    applicationDefaultJvmArgs = listOf("-Dsun.java2d.uiScale=1", "-Dstdout.encoding=UTF-8")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

val contentRoot = rootProject.layout.projectDirectory.dir("content").asFile.absolutePath

// ./gradlew :ui-prototype:run  – interaktives Fenster
tasks.named<JavaExec>("run") {
    systemProperty("frosthain.contentRoot", contentRoot)
}

// ./gradlew :ui-prototype:screenshots  – PNGs ohne Bildschirm nach build/screenshots
tasks.register<JavaExec>("screenshots") {
    group = "verification"
    description = "Rendert die Prototyp-Ansichten B, D und F als PNG (headless)."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "net.frosthain.prototype.PrototypeApp"
    systemProperty("frosthain.contentRoot", contentRoot)
    systemProperty("java.awt.headless", "true")
    val out = layout.buildDirectory.dir("screenshots")
    args("--screenshots", out.get().asFile.absolutePath)
    outputs.dir(out)
    inputs.dir(rootProject.layout.projectDirectory.dir("content"))
}
