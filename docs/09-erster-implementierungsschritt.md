# 9 · Erster konkreter Implementierungsschritt

## 9.1 Warum dieser Schritt zuerst

Der Auftrag sieht den Launcher als Phase 1 vor. Dessen kritischster Teil (eigene Anmeldung) wartet aber auf eine externe Freigabe, und die Mod ließ sich in dieser Umgebung nicht bauen (Fabric- und Mojang-Server gesperrt). Der Schritt mit dem größten Nutzen, der hier **vollständig gebaut und getestet** werden konnte, ist das Fundament, auf dem Mod, Editor, Talentbaum und Launcher-Charakteransicht aufsetzen: der Minecraft-unabhängige RPG-Kern mit Datenmodell und Basisinhalten.

## 9.2 Was umgesetzt wurde

| Baustein | Dateien | Nachweis |
|---|---|---|
| Build: Gradle 9.4 Wrapper, JDK 25, Versionskatalog, Compiler mit `-Xlint:all -Werror` | `settings.gradle.kts`, `build.gradle.kts`, `gradle/`, `core/build.gradle.kts` | `./gradlew check` grün |
| IDs kompatibel zu Minecraft | `ContentId` | `ContentIdTest` |
| Strikter JSON-Leser (Pflichtfelder, Bereiche, unbekannte Felder, JSON-Pfade in Meldungen) | `json/JsonObjectReader` | `ContentLoaderTest` |
| Inhaltstypen: Quellen, Fertigkeiten, Ressourcen, Fähigkeiten, Spezies, Klassen, Unterklassen, Hintergründe, Talentbäume, Regeln | `content/*`, `rules/*` | `BaseContentPackTest` |
| Lader mit Datapack-Layout und Override-Semantik, sammelt alle Fehler statt beim ersten abzubrechen | `registry/*` | `ContentLoaderTest` |
| Validator: Lizenz-Sperre, Referenzen, Zyklen, Erreichbarkeit (inkl. Exklusivgruppen), Positionen, Übersetzungen, Status der Engine-Funktionen | `validation/ContentValidator` | `ContentValidatorTest` |
| Charaktererstellung mit Standardreihe, Punktekauf, Hintergrundbonus, Namens- und Aussehensprüfung | `character/*` | `CharacterFactoryTest` |
| Talentregeln: Vergabe, Sperrgründe, Ränge, Exklusivität, Rücknahme, Reset, Bereinigung nach Inhaltsänderung | `progression/TalentAllocator` | `TalentAllocatorTest` |
| Stufenaufstieg, Multiclassing, Unterklassenwahl, gemeinsame Zauberstufe | `progression/LevelingRules` | `LevelingRulesTest` |
| Speicherformat v1 mit Migrationskette und Schutz vor neueren Formaten | `persistence/*` | `CharacterSheetCodecTest` |
| Kommandozeilen-Prüfung der Inhalte | `tools/ValidateContentCli`, Task `:core:validateContent` | Teil von `check` |
| Basisinhalte: 12 Klassen, 14 Unterklassen, 9 Spezies (mit Abstammungslinien), 6 Hintergründe, 18 Fertigkeiten, 5 Ressourcen, 7 Fähigkeiten, 2 Talentbäume (17 + 10 Knoten), Regeln, 245 Übersetzungen je Sprache (Deutsch, Englisch) | `content/frosthain_base/` | Validator: 0 Fehler |

## 9.3 Ergebnis der Prüfung (in dieser Sitzung tatsächlich ausgeführt)

```
./gradlew check
  JUnit: 58 Tests, 0 Fehlschläge, 0 Fehler, 0 übersprungen
  Inhaltsprüfung: 0 Fehler, 11 Warnungen
BUILD SUCCESSFUL
```

Die 11 Warnungen sind beabsichtigt und ehrlich: 11 von 12 Klassen haben noch keinen Talentbaum (nur der Krieger). Zusätzlich meldet der Validator als Hinweis, dass 7 Engine-Funktionen (z. B. Dunkelsicht) zwar spezifiziert, in der Mod aber noch nicht umgesetzt sind.

Ein Test schlug im ersten Lauf fehl – nicht wegen eines Fehlers im Code, sondern weil der Test falsch gerechnet hatte (15/15/15/8/8/8 kostet genau die erlaubten 27 Punkte). Der Test wurde korrigiert und prüft jetzt einen tatsächlich überzogenen Punktekauf (29 Punkte).

## 9.4 Selbst nachprüfen

Voraussetzung: JDK 25 (z. B. Eclipse Temurin 25 oder Microsoft Build of OpenJDK 25).

```bash
./gradlew check
```

```bash
./gradlew :core:validateContent
```

Unter Windows `gradlew.bat` statt `./gradlew`. Testbericht: `core/build/reports/tests/test/index.html`.

## 9.5 Bewusst noch nicht umgesetzt

- Keine Minecraft-Integration (Mod-Modul fehlt, siehe M0.3) – im Spiel passiert noch nichts.
- Fähigkeiten haben nur Metadaten, keine Effekte.
- Aussehens-Teile (Frisuren, Hörner …) sind nur IDs; Texturen und Compositing folgen in M2.4.
- Talentbäume nur für die allgemeine Entwicklung und den Krieger.
- Die SRD-Inhaltsliste stammt aus Sekundärquellen und wird in M3.1 gegen das Original-PDF geprüft.
- CI-Datei existiert, wurde aber noch nie ausgeführt (kein Remote-Repository).

## 9.6 Nächster Schritt: M0.3 – Fabric-Mod-Skelett

Sobald Netzwerkzugriff besteht (oder auf dem Windows-Rechner bzw. im CI):

1. Aktuelle stabile Versionen für Minecraft 26.3 ermitteln: Fabric Loader, Fabric API, Loom (über `meta.fabricmc.net` und die Fabric-Entwicklerseite) und in `gradle/libs.versions.toml` pinnen.
2. Modul `:mod` mit Plugin `net.fabricmc.fabric-loom`, getrennten Source-Sets `main`/`client`, Abhängigkeit auf `:core` (eingebettet), `content/frosthain_base` als Ressourcen.
3. `FrosthainMod`: Datapack-Reload-Listener → `ContentLoader` → `ContentValidator`; Diagnosen ins Log; Registry für das Spiel bereitstellen.
4. Spieleranhang für den Charakterbogen (Fabric Data Attachment API) – noch ohne Oberfläche.
5. Ein GameTest: Server startet, Inhalte geladen, 12 Klassen vorhanden.
6. Abnahme: Dev-Client und Dedicated Server starten fehlerfrei; GameTest grün im CI.

## 9.7 Was der Projektinhaber jetzt tun sollte

1. **GitHub-Repository anlegen** und in dieser Umgebung freigeben, damit gepusht werden kann. *Bis dahin liegt der Stand nur in diesem flüchtigen Cloud-Container.*
2. **Netzwerkfreigaben** in den Umgebungseinstellungen ergänzen: `piston-meta.mojang.com`, `piston-data.mojang.com`, `launchermeta.mojang.com`, `libraries.minecraft.net`, `resources.download.minecraft.net`, `meta.fabricmc.net`, `maven.fabricmc.net`, `fabricmc.net`, `docs.fabricmc.net` (Anleitung: https://code.claude.com/docs/en/cloud-environments#network-access).
3. **Azure-App registrieren und Mojang-Freigabe beantragen** (Microsoft Entra ID → App-Registrierung, öffentlicher Client, Weiterleitung `http://localhost`; danach das Mojang-Antragsformular für die Minecraft-API). Das hat die längste Wartezeit.
4. **Referenzbilder** für die Richtungen B, D und F bereitstellen (für M2.1).
5. **Lizenz** für FROSTHAIN-Code und -Inhalte festlegen (offen, quelloffen oder proprietär – siehe ADR 0005).
