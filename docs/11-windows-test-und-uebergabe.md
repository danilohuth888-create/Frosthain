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

### Lokaler Test auf dem MSI-Laptop (Windows 11 Home, 01.11.2026)

Umgebung: Microsoft Build of OpenJDK 25.0.4 (nur für die Build-Prozesse als `JAVA_HOME` gesetzt; `java` im PATH bleibt JDK 17), Gradle 9.8.2 über den Wrapper, Intel UHD + NVIDIA GeForce RTX 3050 Laptop GPU, Minecraft Launcher aus dem Microsoft Store 2.6.2.0.

| Test | Ergebnis |
|---|---|
| `gradlew.bat check :mod:build` | ✅ 105 JUnit-Tests (Kern 75, UI 30), Inhaltsprüfung 0 Fehler, 12 Warnungen (Klassen ohne Talentbaum) |
| Server-GameTest (`:mod:runGameTest`, Teil von `check`) | ✅ `FROSTHAIN geladen … 0 Fehler`, `All 1 required tests passed` |
| Reproduzierbarkeit | ✅ lokal gebaute `frosthain.jar` ist bitgenau gleich der CI-Datei aus `frosthain-dev` (SHA-256 `2be39326…`) |
| Installer-Test (`Test-Installer.ps1`, künstliches `.minecraft`) | ✅ 19/19 mit Windows PowerShell 5.1 (PowerShell 7 nicht installiert) |
| **Minecraft-Client** (`:mod:runClientGameTest`) mit echter Grafik | ✅ startet, lädt FROSTHAIN, erstellt Einzelspielerwelt, Screenshots; OpenGL-Backend |
| UI-Prototyp-Screenshots unter Windows | ✅ 9 Bilder (`tools/ui-prototype/build/screenshots/`) |

Gefunden und behoben:
- **Schriften wurden im Spiel verworfen:** Minecraft akzeptiert in Ressourcenpfaden nur `[a-z0-9/._-]`; `Cinzel.ttf`, `EBGaramond*.ttf` und die OFL-Texte wurden mit `Invalid path in mod resource-pack` ignoriert. Dateien heißen jetzt `cinzel.ttf`, `eb_garamond.ttf`, `eb_garamond_italic.ttf`, `cinzel_ofl.txt`, `eb_garamond_ofl.txt`; neuer Test `BaseContentPackTest.everyPackFileHasAPathMinecraftAccepts` verhindert den Rückfall.
- **`dev-test-setup.ps1` lud unnötig ein JDK:** Ein installiertes JDK 25 wurde nur erkannt, wenn es im PATH stand. Jetzt werden auch `JAVA_HOME` und die üblichen Installationsordner (Microsoft, Eclipse Adoptium, Java, Zulu, BellSoft) geprüft.

**Noch nicht getestet:** interaktives Prototyp-Fenster unter Windows.

### Erste Oberflächen im Spiel (02.11.2026)

Beim Start von Minecraft erscheint jetzt das FROSTHAIN-Hauptmenü, beim ersten Betreten einer Welt die Charaktererstellung. Bilder: [docs/spiel/](spiel/).

| Ansicht (Design 2.0) | Im Spiel | Nachweis |
|---|---|---|
| Hauptmenü „FROSTHAIN – Das Erwachen“ (Einzelspieler, Mehrspieler, Charakter, Optionen, Beenden) | ✅ | Produktionsstart auf dem MSI (Minecraft-Klassenpfad wie im Launcher), Bild `01_hauptmenue.png` |
| Charaktererstellung: Spezies · Klasse · Hintergrund · Fertig (Name, Attribute), Spielermodell | ✅ | Client-GameTest: Erstellung als Elf/Runenmagier/Gelehrter, Server speichert und prüft (`CharacterFactory`) |
| Charakter als Spieleranhang (persistent, bleibt nach dem Tod) | ✅ | GameTest liest den gespeicherten Bogen auf dem Server |
| HUD-Abzeichen (Name, Klasse, Stufe), Taste **I** öffnet „Dein Held“ | ✅ | Screenshot `05_hud.png` |
| Vorschau der Charaktererstellung aus dem Hauptmenü (nur ansehen) | ✅ | GameTest |
| **Talentbaum (K)**: Pfade (Legende, Runenpfad, Klassenbaum als Vorschau), Zweig-Reiter, Sechseck-Knoten mit Verbindungen, Detailtafel, Freischalten/Zurücknehmen/Alle zurücksetzen, Ziehen und Zoomen | ✅ | Client-GameTest: 2 Talente freigeschaltet, Server speichert und prüft (`TalentAllocator`); Bilder `06_`/`07_talentbaum*.png` |
| Attribut-Talente wirken (Schaden, Rüstung, Leben, Tempo, Reichweite … – 28 Effekte) | ✅ | GameTest prüft den Modifikator „Kampfhaltung“ auf dem Angriffsschaden; wird nach Respawn neu angewendet |
| Testbefehl `/frosthain ep <Menge>` (nur mit Cheats/OP) bis es echte EP-Quellen gibt (M3.2) | ✅ | 2000 EP → Stufe 4 |
| Questtracker, Questjournal, NPC-Dialog, Fähigkeitenleiste | ❌ nächste Schritte | Logik in `core`/`ui` vorhanden |

Grenzen: Aussehen-Schritt (Haare, Hautfarbe) fehlt noch, Attribute werden automatisch nach Klasse verteilt (Standardreihe), lange Namen in Kacheln werden gekürzt. Talent-Symbole sind vorerst Vanilla-Gegenstände; Fähigkeits-, Merkmals- und Resistenz-Talente werden gespeichert, wirken aber erst mit dem Fähigkeitensystem (M4.1). Auf Stufe 1 gibt es 1 Talentpunkt; EP-Quellen im Spiel folgen (M3.2).

So probierst du es aus: Welt mit erlaubten Cheats öffnen, Charakter erstellen, `/frosthain ep 2000` eingeben (Stufe 4), dann **K** drücken.

### Installation auf dem MSI über den echten Launcher

`install-frosthain.ps1` hat Profil und Fabric-Version korrekt eingetragen; andere Profile blieben unverändert. Minecraft startete über den Launcher, lud aber **keine Mods**: siehe O9. Nach dem Kopieren in den echten Ordner lädt der Produktionsstart 52 Mods inklusive FROSTHAIN, 0 Fehler.

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
| O8 | Auf Laptops mit zwei Grafikchips (MSI: Intel UHD + RTX 3050) meldet Minecraft „Preferring discrete GPU: NVIDIA“, nutzt aber tatsächlich die Intel UHD. Abhilfe beim Spieler: Windows-Einstellungen → System → Anzeige → Grafik → `javaw.exe` der Minecraft-Laufzeit auf „Hohe Leistung“ (Systemeinstellung, nicht automatisch durch FROSTHAIN). Beim echten Start über den Launcher lief Minecraft am 01.11. bereits auf der RTX 3050. |
| O9 | Wird der Installer aus einer Store-App (MSIX) heraus gestartet – z. B. aus der Claude-Desktop-App –, leitet Windows **neu angelegte Ordner unter `%LOCALAPPDATA%`** in den privaten Speicher dieser App um (`%LOCALAPPDATA%\Packages\<App>\LocalCache\Local\Frosthain`). Minecraft sieht dann einen leeren `mods`-Ordner. In einer normalen PowerShell tritt das nicht auf. Abhilfe in der Entwicklung: Dateien über den Paketkontext des Launchers kopieren (`Invoke-CommandInDesktopPackage`, Launcher hat Schreibvirtualisierung abgeschaltet) oder die Instanz außerhalb von AppData anlegen (`-InstanceDir`). Die Profilsicherung vom 01.11. liegt deshalb unter `…\Packages\Claude_pzs8sxrjxfjjc\LocalCache\Local\Frosthain\backups`. |

## 11.6 Nächste Entwicklungsschritte (Reihenfolge nach Prioritätenliste)

1. **RPG-Daten im Spiel verankern:** Charakterbogen als Spieleranhang (Fabric Data Attachment), Inhalte über Datapack-Reload, Netzwerkpakete mit Serverprüfung; Client-GameTest um Assertions erweitern.
2. **Charaktererstellung im Spiel** (Design 2.0, großes Minecraft-Modell) beim ersten Betreten einer Welt, danach Talentbaum auf `K` mit serverseitiger Vergabe.
3. **Fähigkeiten wirksam machen** (Ressourcen, Abklingzeiten, Fähigkeitenleiste, Runenstoß als erste echte Runenfähigkeit), danach Questtracker, Questjournal und NPC-Dialog mit Eldrin in der Welt.
