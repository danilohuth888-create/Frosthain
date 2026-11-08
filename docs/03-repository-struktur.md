# 3 · Repository- und Ordnerstruktur

Ein Monorepo, damit Launcher, Mod, Kern und Inhalte immer zueinander passende Versionen haben ([ADR 0001](adr/0001-monorepo-und-minecraft-unabhaengiger-kern.md)).

```
frosthain/
├── README.md                      Überblick, Status, Schnellstart
├── NOTICE.md                      SRD-Namensnennung, Marken- und Minecraft-Hinweise
├── settings.gradle.kts            Gradle-Module (:core, später :mod)
├── build.gradle.kts               gemeinsame Gradle-Konfiguration
├── gradle.properties              Version, Build-Optionen
├── gradle/
│   ├── libs.versions.toml         zentrale Versionsliste aller JVM-Abhängigkeiten
│   └── wrapper/                   Gradle 9.4 Wrapper
├── gradlew, gradlew.bat
│
├── core/                          ✅ RPG-Kern ohne Minecraft (Java 25)
│   └── src/main/java/net/frosthain/core/
│       ├── ContentId.java         Namespaced IDs (kompatibel zu Minecraft-Identifiern)
│       ├── json/                  strikter JSON-Leser mit Pfadangaben
│       ├── content/               Definitionen: Klassen, Spezies, Talentbäume, …
│       ├── registry/              Lader, Registry, Inhaltstypen, Verzeichnisquelle
│       ├── validation/            ContentValidator, Diagnosen
│       ├── rules/                 Fortschritts- und Attributsregeln
│       ├── character/             Charakterbogen, Aussehen, Charaktererstellung
│       ├── progression/           Talentvergabe, Stufenaufstieg, Multiclassing
│       ├── persistence/           Speicherformat und Migrationen
│       └── tools/                 Kommandozeilen-Prüfung der Inhalte
│
├── content/                       ✅ Inhaltspakete (Datapack-Layout)
│   └── frosthain_base/
│       ├── data/frosthain/frosthain/
│       │   ├── sources/           Lizenzkatalog (Quelle je Inhalt)
│       │   ├── rules/             progression.json, ability_scores.json
│       │   ├── skills/ resources/ powers/
│       │   ├── species/ classes/ subclasses/ backgrounds/
│       │   └── talent_trees/
│       └── assets/frosthain/lang/ de_de.json, en_us.json
│
├── ui/                            ✅ Präsentationslogik der Oberflächen (ohne Minecraft)
├── tools/ui-prototype/            ✅ Desktop-Prototyp B · D · F (Java2D), Screenshots headless
├── mod/                           🔶 Fabric-Mod: Skelett baut gegen 26.3 (CI); Start im Spiel offen
│   ├── build.gradle.kts           Plugin net.fabricmc.fabric-loom, bindet :core ein
│   └── src/
│       ├── main/java/net/frosthain/mod/        Server/gemeinsam
│       ├── client/java/net/frosthain/mod/client/  nur Client (Loom split source sets)
│       ├── main/resources/fabric.mod.json
│       └── gametest/                            Fabric GameTests
│
├── launcher/                      ⏳ Windows-Launcher (ab M1.1)
│   ├── src-tauri/                 Rust: Befehle, Fensterverwaltung, Updater
│   │   └── crates/launcher-core/  Rust-Bibliothek: Manifeste, Installation, Auth, Mods, Backups
│   ├── src/                       TypeScript/React-Oberfläche
│   └── tests/                     Playwright-Tests der Oberfläche
│
├── installer/                     ⏳ install.ps1 + Dokumentation (M1.5)
├── schemas/                       ⏳ JSON-Schemas für Editor-Unterstützung (M3)
├── tools/                         ⏳ Entwicklungswerkzeuge (z. B. Strukturexport, Dialog-Compiler)
├── docs/                          ✅ Planung, Konzepte, ADRs
│   ├── 01 … 09                    Planungsdokumente (diese Reihe)
│   ├── inhaltskatalog-und-lizenzen.md
│   └── adr/                       Architekturentscheidungen
└── .github/workflows/ci.yml       ✅ CI-Definition (noch nicht ausgeführt – kein Remote-Repository)
```

✅ vorhanden · ⏳ geplant (Verzeichnis entsteht mit dem jeweiligen Meilenstein, keine leeren Platzhalter)

## Konventionen

| Thema | Regel |
|---|---|
| Java-Paket | `net.frosthain.*` (unabhängig von Domainbesitz; vor dem ersten öffentlichen Release endgültig festlegen) |
| Mod-ID / Namespace | `frosthain` |
| IDs | `snake_case`, Englisch; Unterordner im Pfad erlaubt (`fighter/champion`) |
| Commits | Deutsch oder Englisch, Präsens, ein Thema pro Commit |
| Branches | `main` stabil; Arbeitsbranches `feature/<thema>` |
| Versionen | SemVer `MAJOR.MINOR.PATCH`; Minecraft-Version im Release-Namen (`0.3.0+mc26.3`) |
| Zeilenenden | LF, Windows-Skripte CRLF (`.gitattributes`) |
