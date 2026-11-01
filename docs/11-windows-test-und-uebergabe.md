# 11 · Installation, Spielanleitung, Testergebnisse und Übergabe

Stand: 01.11.2026 · Branch `claude/ui-prototyp` · Pull Request #1 · CI-Läufe 41353429251 (push) und 41353434818 (PR), alle Jobs grün · Vorabversion `frosthain-dev`

> **Ehrlicher Stand:** Die installierbare Version ist ein **Entwicklungsstand**. Minecraft startet mit FROSTHAIN und lädt alle Inhalte. Sichtbare RPG-Funktionen im Spiel (Charaktererstellung, Talentbaum, Quests, Dialoge, Runenmagie) sind **noch nicht** eingebaut. Sie existieren als getestete Logik und im Desktop-Prototyp. FROSTHAIN 1.0 ist nicht fertig.

## 11.1 Installation (Windows 10/11, ohne winget, ohne Java, ohne Git)

Voraussetzung: Der offizielle Minecraft Launcher ist installiert, wurde einmal gestartet, und du bist angemeldet. Der Launcher muss **geschlossen** sein, während das Skript läuft.

```powershell
powershell -ExecutionPolicy Bypass -Command "irm https://github.com/danilohuth888-create/Frosthain/releases/download/frosthain-dev/install-frosthain.ps1 -OutFile $env:TEMP\install-frosthain.ps1; powershell -ExecutionPolicy Bypass -File $env:TEMP\install-frosthain.ps1"
```

Das Skript:
- lädt `frosthain.jar`, `fabric-api.jar` und die Fabric-Versionsbeschreibung aus der Vorabversion und prüft jede Datei per SHA-256 (`SHA256SUMS.txt`); bei Abweichung bricht es ab, ohne etwas zu verändern;
- sichert `launcher_profiles.json` nach `%LOCALAPPDATA%\Frosthain\backups\<Zeitstempel>` und stellt sie bei Fehlern wieder her;
- legt die Fabric-Version additiv in `.minecraft\versions` ab und einen **eigenen Spielordner** `%LOCALAPPDATA%\Frosthain\instance` an (eigene Welten, Mods, Einstellungen);
- trägt die Installation **„FROSTHAIN“** im offiziellen Launcher ein. Fremde Profile und Welten bleiben unberührt.

Entfernen (Welten bleiben erhalten): dasselbe Skript mit `-Uninstall`. Weitere Optionen: `-MemoryGB 6`, `-InstanceDir <Ordner>`.

Java braucht der Spieler nicht: Der offizielle Launcher bringt die passende Java-Laufzeit für Minecraft 26.3 mit. Für Entwickler gibt es `installer/dev-test-setup.ps1` (portables JDK 25 von Adoptium mit Prüfsumme, Quellcode als ZIP, Build, optional `-Prototype`).

## 11.2 So spielst du (aktueller Stand)

1. Offiziellen Minecraft Launcher öffnen, Installation **FROSTHAIN** wählen, **Spielen**.
2. Einzelspielerwelt erstellen und normal spielen.
3. Nachweis, dass FROSTHAIN aktiv ist: `%LOCALAPPDATA%\Frosthain\instance\logs\latest.log` enthält `FROSTHAIN geladen: 13 Klassen, 9 Spezies, 3 Talentbäume, 3 Quests, 1 Dialoge – 0 Fehler`.

Die Oberflächen (HUD, Talentbaum, Dialog) kannst du im **Desktop-Prototyp** ausprobieren: `installer\dev-test-setup.ps1 -Prototype` (siehe `docs/prototyp/README.md`).

## 11.3 Testergebnisse (GitHub Actions, 01.11.2026)

| Test | Umgebung | Ergebnis |
|---|---|---|
| 104 JUnit-Tests (Kern 74, UI 30) + Inhaltsprüfung (0 Fehler) | Ubuntu und Windows | ✅ |
| Mod-Build gegen Minecraft 26.3 (Loader 0.20.1, Fabric API 0.165.0+26.3) | Ubuntu und Windows | ✅ |
| **Dedicated Server** startet, lädt FROSTHAIN | Ubuntu | ✅ `Done`, 0 Fehler |
| **Minecraft-Client** startet (Fabric Client-GameTest), lädt FROSTHAIN, erstellt und speichert eine Einzelspielerwelt | Ubuntu, xvfb, Software-Rendering | ✅ `FROSTHAIN geladen: 13 Klassen … 0 Fehler`; Grafik: OpenGL ohne GPU nicht verfügbar → Minecraft wechselt automatisch auf **Vulkan** (llvmpipe) |
| Ingame-Screenshots (Titelbildschirm, Welt) | Artefakt `client-screenshots` im CI-Lauf | erzeugt; aus der Cloud-Sitzung nicht abrufbar (Download auf GitHub möglich) |
| **Installer**: Installation, Update, manipulierte Datei, fehlender Launcher, Deinstallation, fremde Profile/Welten unberührt, Datei ohne BOM | Windows PowerShell 5.1 **und** PowerShell 7 | ✅ 2 × 19 Prüfungen |
| Portables JDK 25 ohne winget (Adoptium, SHA-256) | Windows | ✅ |
| UI-Prototyp-Screenshots | Ubuntu headless | ✅ |

**Noch nicht getestet:** Installation auf deinem MSI-Laptop (echter Launcher, echte Anmeldung); Spielen mit echter Grafikkarte; Prototyp-Fenster unter Windows.

## 11.4 Funktionsübersicht

| Bereich | Umgesetzt und getestet | Im Spiel sichtbar |
|---|---|---|
| Klassen (12 SRD + Runenmagier), Unterklassen, 9 Spezies, Hintergründe, Attribute | Daten + Regeln | ❌ |
| Charaktererstellung (Regeln, Aussehen-Validierung, Speicherformat) | ✅ | ❌ |
| Talentbäume (allgemein mit 4 Zweigen, Runenpfad, Krieger), Vergabe/Rücknahme/Reset, Multiclassing | ✅ | ❌ |
| Quests (Haupt-/Neben-, aufeinanderfolgende Ziele, Folgequests), NPC Eldrin, Dialog „Das Erwachen“ | ✅ | ❌ |
| Fähigkeiten (7 + Runenstoß) | nur Metadaten | ❌ |
| Engine-Funktionen (Dunkelsicht usw.) | spezifiziert (`PLANNED`) | ❌ |
| UI-Logik HUD/Talentbaum/Dialog + Desktop-Prototyp | ✅ | Prototyp |
| Fabric-Mod: lädt und prüft Inhalte beim Start | ✅ Server + Client | nur im Log |
| Installer (Bridge-Modus über offiziellen Launcher) | ✅ automatisiert | – |
| Eigener Launcher (Tauri) | Konzept | ❌ |

## 11.5 Bekannte Einschränkungen und offene Punkte

| # | Punkt |
|---|---|
| O1 | In der Cloud-Arbeitsumgebung sind Fabric-/Mojang-Server gesperrt; Mod-Entwicklung läuft über CI-Durchläufe (langsam). Abhilfe: Netzwerkfreigabe in einer neuen Sitzung prüfen. |
| O2 | Viele Fabric-Methoden heißen in 26.x anders (Mojang-Namen). Die echte API von `TestSingleplayerContext` wurde ins CI-Log geschrieben: `getServer`, `getConnection`, `getWorldSave`, `close`. |
| O3 | Die Vorabversion `frosthain-dev` wird bei jedem Push auf den Branch neu gebaut; sie ist ein Entwicklungsstand ohne Code-Signatur. |
| O4 | Gradle-Wrapper-Prüfsumme nicht gepinnt. |
| O5 | Schriften ohne Glyphen ▸ ✓ ✗ ↑ ↓ – im Spiel wie im Prototyp als Grafik zeichnen. |
| O6 | SRD-Inhaltsliste noch nicht mit dem Original-PDF abgeglichen; Halbelf/Wolfsmensch aus den Referenzen noch nicht angelegt. |
| O7 | Der Prototyp nutzt noch die ältere, stärker verzierte Optik; Farben sind auf Design 2.0 umgestellt, Rahmen noch nicht. |

## 11.6 Nächste Entwicklungsschritte (Reihenfolge nach Prioritätenliste)

1. **RPG-Daten im Spiel verankern:** Charakterbogen als Spieleranhang (Fabric Data Attachment), Inhalte über Datapack-Reload, Netzwerkpakete mit Serverprüfung; Client-GameTest um Assertions erweitern.
2. **Charaktererstellung im Spiel** (Design 2.0, großes Minecraft-Modell) beim ersten Betreten einer Welt, danach Talentbaum auf `K` mit serverseitiger Vergabe.
3. **Fähigkeiten wirksam machen** (Ressourcen, Abklingzeiten, Fähigkeitenleiste, Runenstoß als erste echte Runenfähigkeit), danach Questtracker, Questjournal und NPC-Dialog mit Eldrin in der Welt.
