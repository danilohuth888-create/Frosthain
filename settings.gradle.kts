rootProject.name = "frosthain"

// Minecraft-unabhängiger RPG-Kern (Regeln, Datenmodell, Validierung, Persistenzformat).
include(":core")

// Die Fabric-Mod (`:mod`) wird erst eingebunden, sobald der Build Zugriff auf
// maven.fabricmc.net und die Mojang-Server hat (siehe docs/07-roadmap.md, Meilenstein M0.3).
// Bis dahin bleibt der Kern ohne Minecraft-Abhängigkeit vollständig baubar und testbar.
