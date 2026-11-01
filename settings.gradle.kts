pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        gradlePluginPortal()
    }
    plugins {
        // Version kommt aus gradle.properties (fabric.loomVersion); wird nur gebraucht, wenn :mod eingebunden ist.
        id("net.fabricmc.fabric-loom") version (providers.gradleProperty("fabric.loomVersion").getOrElse("0"))
    }
}

rootProject.name = "frosthain"

// Minecraft-unabhängiger RPG-Kern (Regeln, Datenmodell, Validierung, Persistenzformat).
include(":core")

// Präsentationslogik der Oberflächen (HUD, Talentbaum, Dialoge) ohne Minecraft.
include(":ui")

// Interaktiver Desktop-Prototyp der Oberflächen B, D, F (Java2D), nutzt :ui.
include(":ui-prototype")
project(":ui-prototype").projectDir = file("tools/ui-prototype")

// Fabric-Mod für Minecraft 26.3. Braucht Zugriff auf maven.fabricmc.net und die Mojang-Server.
// Eingebunden, sobald die Fabric-Versionen gesetzt sind (gradle.properties oder -P) und nicht
// ausdrücklich abgeschaltet (frosthain.withMod=false, z. B. in Umgebungen ohne diese Server).
val withMod = providers.gradleProperty("frosthain.withMod").map { it.toBoolean() }.getOrElse(true)
val fabricConfigured = providers.gradleProperty("fabric.loomVersion").isPresent
if (withMod && fabricConfigured) {
    include(":mod")
}
