plugins {
    `java-library`
}

java {
    // Minecraft 26.x verlangt Java 25. Der Kern wird mit derselben Version gebaut,
    // damit es zwischen Kern und Mod keine Bytecode-Überraschungen gibt.
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

dependencies {
    // `api`, weil Gson-Typen (JsonElement) Teil der öffentlichen Lade-Schnittstelle sind.
    // In der Mod wird Gson NICHT mitgebündelt, sondern von Minecraft bereitgestellt.
    api(libs.gson)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
    // Der Integrationstest validiert das echte Inhaltspaket im Repository.
    systemProperty("frosthain.contentRoot", rootProject.layout.projectDirectory.dir("content").asFile.absolutePath)
    inputs.dir(rootProject.layout.projectDirectory.dir("content"))
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Kommandozeilen-Validierung aller Inhaltspakete: ./gradlew :core:validateContent
tasks.register<JavaExec>("validateContent") {
    group = "verification"
    description = "Prüft alle FROSTHAIN-Inhaltspakete (Referenzen, Lizenzen, Talentbäume, Übersetzungen)."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "net.frosthain.core.tools.ValidateContentCli"
    args(rootProject.layout.projectDirectory.dir("content").asFile.absolutePath)
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }
    // Umlaute in der Ausgabe auch auf Konsolen ohne UTF-8-Standard (z. B. ältere Windows-Konsolen).
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
}

tasks.named("check") {
    dependsOn("validateContent")
}
