rootProject.name = "frosthain"

// Minecraft-unabhängiger RPG-Kern (Regeln, Datenmodell, Validierung, Persistenzformat).
include(":core")

// Präsentationslogik der Oberflächen (HUD, Talentbaum, Dialoge) ohne Minecraft.
include(":ui")

// Interaktiver Desktop-Prototyp der Oberflächen B, D, F (Java2D), nutzt :ui.
include(":ui-prototype")
project(":ui-prototype").projectDir = file("tools/ui-prototype")

// Die Fabric-Mod (`:mod`) wird erst eingebunden, sobald der Build Zugriff auf
// maven.fabricmc.net und die Mojang-Server hat (siehe docs/07-roadmap.md, Meilenstein M0.3).
// Bis dahin bleibt der Kern ohne Minecraft-Abhängigkeit vollständig baubar und testbar.
