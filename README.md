# FROSTHAIN – Vom Abenteurer zur Legende

Ein eigenständiges Fantasy-RPG-Erlebnis auf Basis von Minecraft Java Edition: eigener Launcher, eigene Fabric-Mod, umfangreiche Charaktererstellung, verzweigte Talentbäume, Multiclassing, Runenmagie und eine bewohnte Welt mit verborgenem Tal und geheimem Hafen. Einzelspieler zuerst, freies Story-Survival, keine Pflichtaufgaben, keine Quest-Timer.

## Aktueller Stand (01.11.2026)

| Bereich | Stand |
|---|---|
| Projektplanung (Machbarkeit, Architektur, Konzepte, Roadmap, Risiken) | ✅ [docs/](docs/) |
| RPG-Kern ohne Minecraft: Datenmodell, Lader, Validator, Charaktererstellung, Talente, Stufen, Multiclassing, Quests, Dialoge, Speicherformat | ✅ **74 Tests grün** |
| Basisinhalte: 12 Klassen, 14 Unterklassen, 9 Spezies, 6 Hintergründe, 3 Talentbäume (inkl. Runenpfad), NPC Eldrin, 3 Quests, 1 Dialog, Deutsch + Englisch | ✅ Validator: 0 Fehler |
| UI nach Referenzbildern B · D · F: Implementierungsplan, Präsentationslogik (`ui`), interaktiver Desktop-Prototyp | ✅ **30 Tests grün**, [Screenshots](docs/prototyp/) |
| Fabric-Mod für Minecraft 26.3 | 🔶 baut im CI (Loader 0.20.1, Fabric API 0.165.0+26.3); **Dedicated Server startet mit geladenen Inhalten (CI)**; Client noch nicht geprüft |
| CI auf GitHub (Ubuntu + Windows) | ✅ grün |
| Launcher (Tauri 2, Anmeldung über den offiziellen Minecraft Launcher) | ⏳ M1.1 |
| Im Spiel spielbar | ❌ noch nicht |

Lizenz: FROSTHAIN-Eigenentwicklungen sind proprietär, alle Rechte vorbehalten ([LICENSE.md](LICENSE.md)); Drittanbieter siehe [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Dokumentation

1. [Technische Machbarkeitsanalyse](docs/01-machbarkeitsanalyse.md)
2. [Architekturentwurf](docs/02-architektur.md)
3. [Repository- und Ordnerstruktur](docs/03-repository-struktur.md)
4. [Konzept Launcher](docs/04-launcher-konzept.md)
5. [Konzept Charaktereditor (inkl. UI-Leitlinien)](docs/05-charaktereditor-konzept.md)
6. [Datenmodell für Klassen, Spezies und Talente](docs/06-datenmodell.md)
7. [Priorisierte Roadmap](docs/07-roadmap.md)
8. [Risikoanalyse](docs/08-risikoanalyse.md)
9. [Erster Implementierungsschritt](docs/09-erster-implementierungsschritt.md)
10. [UI-Implementierungsplan B · D · F](docs/10-ui-implementierungsplan.md)
11. [Windows-Installation, Testplan und Übergabe](docs/11-windows-test-und-uebergabe.md)

Außerdem: [UI-Prototyp](docs/prototyp/) · [Referenzen und Vorgaben](docs/referenzen/) · [Inhaltskatalog und Lizenzen](docs/inhaltskatalog-und-lizenzen.md) · [Architekturentscheidungen (ADRs)](docs/adr/) · [Hinweise und Namensnennung](NOTICE.md)

## Für Entwickler

Voraussetzung: JDK 25.

```bash
./gradlew check
```

Führt alle Tests aus und prüft alle Inhaltspakete. Unter Windows `gradlew.bat check`.

```bash
./gradlew :core:validateContent
```

Prüft nur die Inhalte und listet Fehler, Warnungen und Hinweise.

```bash
./gradlew :ui-prototype:run
```

Startet den interaktiven UI-Prototyp (HUD, Talentbaum, Dialog).

```bash
./gradlew :mod:build
```

Baut die Fabric-Mod (`mod/build/libs/`). Braucht Internetzugang zu `maven.fabricmc.net` und den Mojang-Servern; mit `-Pfrosthain.withMod=false` wird die Mod übersprungen.

Neue Inhalte (Klassen, Spezies, Talente) sind reine JSON-Dateien – siehe [Datenmodell §6.14](docs/06-datenmodell.md#614-neue-klasse-hinzufügen-beispiel-für-erweiterbarkeit).

## Für Spieler

Zum Testen: [Windows-Einrichtung mit einem PowerShell-Befehl](docs/11-windows-test-und-uebergabe.md). Für Endnutzer noch nicht verfügbar. Ziel: ein Download, ein PowerShell-Befehl, danach alles über den FROSTHAIN-Launcher – ohne Gradle oder Java-Builds.

---

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT. Enthält Material aus dem SRD 5.2.1 von Wizards of the Coast LLC unter CC-BY-4.0 – siehe [NOTICE.md](NOTICE.md).
