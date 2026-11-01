plugins {
    id("net.fabricmc.fabric-loom")
}

val minecraftVersion = providers.gradleProperty("fabric.minecraftVersion").get()
val loaderVersion = providers.gradleProperty("fabric.loaderVersion").get()
val fabricApiVersion = providers.gradleProperty("fabric.apiVersion").get()

base {
    archivesName = "frosthain"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // Minecraft 26.x ist nicht obfuskiert: kein Mappings-Eintrag, kein Remapping (Plugin net.fabricmc.fabric-loom).
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // Regeln und Präsentationslogik werden als Jar-in-Jar mitgeliefert; Gson stellt Minecraft bereit.
    implementation(project(":ui"))
    include(project(":core"))
    include(project(":ui"))
}

// Fabric GameTests: eigener Source-Set "gametest"; der Client-Test startet das echte Spiel (CI: xvfb + Mesa).
// eula = true gilt nur für diese automatisierten Testläufe (Freigabe des Projektinhabers vom 01.11.2026).
fabricApi {
    configureTests {
        createSourceSet = true
        modId = "frosthain-gametest"
        eula = true
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
}

tasks.processResources {
    val properties = mapOf(
        "version" to project.version.toString(),
        "minecraft_version" to minecraftVersion,
        "loader_version" to loaderVersion,
    )
    inputs.properties(properties)
    filesMatching("fabric.mod.json") {
        expand(properties)
    }
    // Basis-Inhaltspaket (Datapack-Layout, Sprachdateien, Schriften) als Mod-Ressourcen.
    from(rootProject.layout.projectDirectory.dir("content/frosthain_base"))
    from(rootProject.layout.projectDirectory.file("THIRD_PARTY_NOTICES.md"))
    from(rootProject.layout.projectDirectory.file("NOTICE.md"))
}
