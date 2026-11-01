# 11 · Windows-Installation, Testplan und Übergabe

Stand: 01.11.2026 · Branch `claude/ui-prototyp` · CI-Lauf 41338708077 (alle Jobs grün)

## 11.1 Einrichtung mit einem PowerShell-Befehl

Voraussetzung: Windows 11 mit `winget` (App-Installer). Das Skript installiert bei Bedarf Git und JDK 25, holt das Repository nach `%USERPROFILE%\Frosthain`, führt alle Tests aus, baut die Mod und legt eine **getrennte** Test-Instanz unter `%LOCALAPPDATA%\Frosthain\test-instance` an. `%APPDATA%\.minecraft` und bestehende Welten bleiben unberührt.

```powershell
powershell -ExecutionPolicy Bypass -Command "irm https://raw.githubusercontent.com/danilohuth888-create/Frosthain/claude/ui-prototyp/installer/dev-test-setup.ps1 -OutFile $env:TEMP\frosthain-setup.ps1; & $env:TEMP\frosthain-setup.ps1 -Prototype"
```

Nach dem Merge in `main` in der URL `claude/ui-prototyp` durch `main` ersetzen und `-Branch main` anhängen. Wer das Skript vorher lesen möchte: URL im Browser öffnen. **Das Skript wurde auf Windows noch nicht ausgeführt** (nur auf Linux-Seite geschrieben); Rückmeldungen bitte mit der Konsolenausgabe.

## 11.2 Testplan

| # | Test | Erwartung | Status |
|---|---|---|---|
| T1 | Skript aus 11.1 ausführen | endet mit „Fertig“, `mods` enthält `frosthain-0.1.0-SNAPSHOT.jar` und `fabric-api-0.165.0+26.3.jar` | ⏳ manuell |
| T2 | UI-Prototyp (öffnet sich durch `-Prototype`) | HUD → `K` Talentbaum → „Kampfhaltung“ freischalten; Rechtsklick → Eldrin → „Ich bin bereit.“ → Tracker zeigt „Finde den alten Schrein …“ | ⏳ manuell |
| T3 | Fabric-Installer: Client, Minecraft 26.3, Loader 0.20.1 | Installation „fabric-loader-0.20.1-26.3“ im offiziellen Launcher | ⏳ manuell |
| T4 | Installation bearbeiten → Spielverzeichnis `%LOCALAPPDATA%\Frosthain\test-instance` → Spielen | Hauptmenü erscheint, Mod-Liste zeigt FROSTHAIN | ⏳ manuell |
| T5 | Einzelspielerwelt erstellen, `test-instance\logs\latest.log` öffnen | Zeile `FROSTHAIN geladen: 12 Klassen, 9 Spezies, 3 Talentbäume, 3 Quests, 1 Dialoge – 0 Fehler` | ⏳ manuell |
| A1 | `check` auf Ubuntu und Windows (CI) | 104 Tests grün, Inhaltsprüfung 0 Fehler | ✅ |
| A2 | Mod-Build gegen 26.3 (CI, Ubuntu und Windows) | Jar mit `core`/`ui` als Jar-in-Jar | ✅ |
| A3 | Fabric-Server-Start (CI, privater Testserver, EULA laut Freigabe nur dort) | `Done (3.149s)` und FROSTHAIN-Ladezeile mit 0 Fehlern | ✅ |
| A4 | Prototyp-Screenshots headless (CI) | 9 PNGs als Artefakt | ✅ |

## 11.3 Was funktioniert – und was noch nicht getestet ist

**Funktioniert und ist geprüft:** Regelkern (Charaktererstellung, Talente, Stufen, Multiclassing, Quests, Dialoge, Speicherformat), Inhaltsvalidierung mit Lizenz-Sperre, UI-Präsentationslogik, Desktop-Prototyp B/D/F, Mod-Build gegen 26.3, Start des Fabric-Servers mit geladenen Inhalten.

**Noch nicht getestet:** Mod im Minecraft-**Client**; PowerShell-Skript auf Windows; Prototyp-Fenster auf Windows (nur headless gerendert); Barrierefreiheit/Controller.

**Noch nicht umgesetzt:** Oberflächen in Minecraft (Talentbaum, Questtracker, Dialog, Charaktereditor); Anwenden der Modifikatoren im Spiel; Engine-Funktionen (alle `PLANNED`); Datapack-Reload; Spielerdaten-Speicherung; NPC-Entity; Launcher; Klasse „Runenmagier“ (beschlossen, nicht angelegt); Talentbäume für 11 Klassen.

## 11.4 Bekannte offene Punkte

| # | Punkt | Auswirkung |
|---|---|---|
| O1 | In der Cloud-Sitzung sind Fabric-/Mojang-Server weiterhin gesperrt (HTTP 403) – Mod wird nur im CI gebaut | Entwicklungsumweg über CI |
| O2 | Gradle-Wrapper-Prüfsumme nicht gepinnt | Supply-Chain-Härtung offen |
| O3 | GitHub-Actions-Warnungen: Node 20 veraltet (`checkout@v4`, `upload-artifact@v4`, `setup-gradle@v4`) | Aktionen aktualisieren |
| O4 | Schriften haben keine Glyphen für ▸ ✓ ✗ ↑ ↓; Prototyp zeichnet sie als Vektoren – im Spiel genauso lösen | sonst Kästchen statt Zeichen |
| O5 | Textumbruch kann eine Zeile mit „…“ beginnen | kosmetisch |
| O6 | SRD-Inhaltsliste nur aus Sekundärquellen, Abgleich mit dem Original-PDF offen | Lizenz-Sorgfalt (M3.1) |
| O7 | Der Server-Starttest meldet einmalig `NoSuchFileException: server.properties` | normal beim ersten Start, harmlos |

## 11.5 Nächste drei Entwicklungsschritte

1. **Client-Start und Datapack-Reload (M0.3 abschließen):** Inhalte über den Ressourcen-Manager laden (`/reload`), Client-Start im CI (Fabric Client-GameTest, headless) und T1–T5 auf Windows.
2. **Talentbaum im Spiel (U5, Teil 1):** `TalentTreeScreen` als Zeichenschicht über `TalentTreeView`, Spieleranhang für den Charakterbogen, Paket „Talent freischalten“ mit Serverprüfung, Taste `K`.
3. **Questtracker und Dialog im Spiel (U5, Teil 2):** HUD-Element über die Fabric-HUD-API, `DialogueScreen` mit HUD-Ausblendung, Rechtsklick auf NPC (E optional, ohne Inventar zu blockieren).
