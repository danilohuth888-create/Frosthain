# FROSTHAIN – Vom Abenteurer zur Legende

Ein eigenständiges Fantasy-RPG-Erlebnis auf Basis von Minecraft Java Edition: eigener Launcher, eigene Fabric-Mod, umfangreiche Charaktererstellung, verzweigte Talentbäume, Multiclassing, Runenmagie und eine bewohnte Welt mit verborgenem Tal und geheimem Hafen. Einzelspieler zuerst, freies Story-Survival, keine Pflichtaufgaben, keine Quest-Timer.

## Aktueller Stand (01.11.2026)

| Bereich | Stand |
|---|---|
| Projektplanung (Machbarkeit, Architektur, Konzepte, Roadmap, Risiken) | ✅ [docs/](docs/) |
| RPG-Kern ohne Minecraft: Datenmodell, Lader, Validator, Charaktererstellung, Talente, Stufen, Multiclassing, Speicherformat | ✅ gebaut, **58 Tests grün** |
| Basisinhalte: 12 Klassen, 14 Unterklassen, 9 Spezies, 6 Hintergründe, 2 Talentbäume, Deutsch + Englisch | ✅ Validator: 0 Fehler |
| Fabric-Mod (Minecraft 26.3) | ⏳ M0.3 – blockiert: Fabric-/Mojang-Server in der Cloud-Umgebung gesperrt |
| Launcher (Tauri 2) | ⏳ M1.1 |
| Im Spiel spielbar | ❌ noch nicht |

Details: [Erster Implementierungsschritt](docs/09-erster-implementierungsschritt.md).

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

Außerdem: [Inhaltskatalog und Lizenzen](docs/inhaltskatalog-und-lizenzen.md) · [Architekturentscheidungen (ADRs)](docs/adr/) · [Hinweise und Namensnennung](NOTICE.md)

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

Neue Inhalte (Klassen, Spezies, Talente) sind reine JSON-Dateien – siehe [Datenmodell §6.14](docs/06-datenmodell.md#614-neue-klasse-hinzufügen-beispiel-für-erweiterbarkeit).

## Für Spieler

Noch nicht verfügbar. Ziel: ein Download, ein PowerShell-Befehl, danach alles über den FROSTHAIN-Launcher – ohne Gradle oder Java-Builds.

---

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT. Enthält Material aus dem SRD 5.2.1 von Wizards of the Coast LLC unter CC-BY-4.0 – siehe [NOTICE.md](NOTICE.md).
