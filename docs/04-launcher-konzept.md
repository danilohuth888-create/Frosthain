# 4 · Konzept: FROSTHAIN-Launcher

## 4.1 Technologieentscheidung

| Kriterium | Electron | **Tauri 2** | .NET 8 + Avalonia/WinUI | Java + JavaFX |
|---|---|---|---|---|
| Installationsgröße | ~150 MB | **~10–20 MB** (nutzt WebView2 von Windows 11) | ~60–80 MB (self-contained) | ~80 MB + JRE |
| Arbeitsspeicher im Leerlauf | hoch | **niedrig** | mittel | mittel |
| Moderne, frei gestaltbare UI | sehr gut | **sehr gut** (Web-Technik) | gut | eingeschränkt |
| Sicherheitsmodell | Node im Renderer muss aktiv abgesichert werden | **Allow-List pro Befehl, kein Node im UI** | gut | gut |
| Zugriff auf Windows-Anmeldeinformationsverwaltung | über Module | **`keyring`-Crate (DPAPI)** | nativ | über JNA |
| Signierte Selbst-Updates | electron-updater | **eingebauter Updater mit Signaturprüfung** | eigene Lösung | eigene Lösung |
| Testbarkeit der Kernlogik | Jest | **`cargo test`** (auch unter Linux im CI) | xUnit | JUnit |

**Entscheidung:** Tauri 2 mit Rust-Kern und TypeScript/React-Oberfläche ([ADR 0003](adr/0003-launcher-tauri.md)). Die gesamte Installations-, Auth- und Dateilogik liegt in der Rust-Bibliothek `launcher-core`, die ohne Fenster testbar ist. Die Oberfläche ruft nur freigegebene Befehle auf.

## 4.2 Funktionsumfang

| Bereich | Funktion | Meilenstein |
|---|---|---|
| **Installation** | Minecraft-Version laut Modpack-Manifest, Bibliotheken (inkl. Windows-Regeln), Assets, Fabric Loader, FROSTHAIN-Mod; jede Datei per SHA-1/SHA-256 geprüft; parallele Downloads mit Wiederaufnahme | M1.1 |
| **Java** | verwaltete Java-25-Laufzeit pro Instanz (Mojang-Laufzeitkomponente oder Temurin), alternativ eigener Pfad | M1.1 |
| **Instanzen** | eigene Spielverzeichnisse unter `%LOCALAPPDATA%\Frosthain\instances\`, getrennt von `.minecraft`; gemeinsamer Cache für Bibliotheken/Assets | M1.1 |
| **Anmeldung** | Microsoft → Xbox Live → XSTS → Minecraft Services → Besitzprüfung → Profil; mehrere Konten | M1.3 (nach Freigabe) |
| **Brückenmodus** | Profil im offiziellen Minecraft Launcher anlegen (mit Backup von dessen Profildatei) | M1.2 |
| **Start & Status** | Startknopf, Fortschritt, „Spiel läuft“, Absturzerkennung | M1.2 |
| **Einstellungen** | RAM (Empfehlung anhand des Systems), Java, JVM-Argumente (Expertenbereich), Auflösung/Vollbild, Grafikvoreinstellung | M1.2 |
| **Modmanager** | FROSTHAIN-Mods (fest), kuratierte optionale Mods, eigene Mods aus Modrinth; aktivieren/deaktivieren; Abhängigkeiten und Konflikte aus `fabric.mod.json` (`depends`, `breaks`, `conflicts`) und Modrinth-Metadaten | M1.4 |
| **Spielstände & Charaktere** | Welten je Instanz mit Charakterübersicht (Name, Spezies, Klassen, Stufe) aus der Charakter-Zusammenfassung der Mod | M2.4 |
| **Sicherung** | automatisch vor jedem Update und jeder Migration, manuell per Knopf; Aufbewahrung konfigurierbar; Wiederherstellung mit Bestätigung | M1.4 |
| **Diagnose** | Launcher- und Spiellog, Absturzbericht-Analyse mit verständlichen deutschen Erklärungen bekannter Fehler (falsche Java-Version, fehlende Abhängigkeit, zu wenig RAM, Mod-Konflikt), Export eines Diagnosepakets mit entfernten Geheimnissen | M1.4 |
| **Updates** | Launcher: signierter Tauri-Updater; Spielinhalte: signiertes Modpack-Manifest | M1.5 |
| **Offline** | Start im Einzelspieler mit zwischengespeichertem, zuvor verifiziertem Profil | M1.3 |

## 4.3 Anmeldung im Detail

```mermaid
sequenceDiagram
  participant U as Spieler
  participant L as FROSTHAIN-Launcher
  participant B as Standardbrowser
  participant MS as Microsoft-Konto
  participant X as Xbox Live / XSTS
  participant M as Minecraft Services
  U->>L: „Anmelden“
  L->>B: öffnet Microsoft-Anmeldeseite (OAuth 2.0 + PKCE, Loopback-Redirect)
  B->>MS: Anmeldung (Passwort/2FA nur bei Microsoft)
  MS-->>L: Autorisierungscode an http://127.0.0.1:<port>
  L->>MS: Code → Access + Refresh Token
  L->>X: Xbox-Live-Token, dann XSTS-Token
  L->>M: login_with_xbox → Minecraft-Token
  L->>M: Besitzprüfung + Profil (Name, UUID, Skin)
  L->>L: Refresh Token in Windows-Anmeldeinformationsverwaltung
```

**Sicherheitsregeln**
- Passwörter werden nie im Launcher eingegeben – nur im Browser bei Microsoft.
- Eigene Azure-App-Registrierung (öffentlicher Client, PKCE, nur `XboxLive.signin offline_access`). Keine fremden Client-IDs.
- Refresh Token ausschließlich in der Windows-Anmeldeinformationsverwaltung (DPAPI, an das Windows-Konto gebunden). Access Tokens nur im Arbeitsspeicher.
- Logs und Diagnosepakete filtern Tokens, auch in der Startzeile des Spiels.
- Das Minecraft-Token muss dem Spielprozess als Startargument übergeben werden (so verlangt es der Client, auch beim offiziellen Launcher); es ist dadurch für Prozesse desselben Windows-Benutzers sichtbar. Dieses Restrisiko wird dokumentiert, nicht verschleiert.
- Gerätecode-Fluss als Alternative für Systeme ohne Standardbrowser.

**Externe Voraussetzung:** Mojang muss die App-ID freischalten (siehe [Machbarkeitsanalyse §1.3.2](01-machbarkeitsanalyse.md#132-anmeldung-kritischer-pfad)). Bis dahin: Brückenmodus.

## 4.4 Dateilayout auf Windows

```
%LOCALAPPDATA%\Frosthain\
├── launcher\               Programm (vom Installer, pro Benutzer, ohne Adminrechte)
├── settings.json           Launcher-Einstellungen (keine Geheimnisse)
├── cache\
│   ├── libraries\          gemeinsame Bibliotheken (per Prüfsumme dedupliziert)
│   ├── assets\             gemeinsame Assets
│   └── runtimes\           Java-Laufzeiten
├── instances\
│   └── frosthain-main\     Spielverzeichnis (saves, mods, config, logs, options.txt)
├── backups\                Weltsicherungen mit Manifest
└── logs\                   Launcher-Logs (rotierend, ohne Tokens)
```

Der Ordner `%APPDATA%\.minecraft` anderer Installationen wird **nie** verändert. Einzige Ausnahme: der Brückenmodus ergänzt dort nach Rückfrage ein Profil in `launcher_profiles.json` und legt vorher eine Sicherungskopie an.

## 4.5 Modpack-Manifest (gemeinsames Format)

```json
{
  "schema_version": 1,
  "frosthain_version": "0.3.0",
  "minecraft": "26.3",
  "fabric_loader": "<aus M0.3>",
  "java_major": 25,
  "mods": [
    { "id": "frosthain", "version": "0.3.0", "url": "https://…", "sha256": "…", "required": true },
    { "id": "fabric-api", "version": "<aus M0.3>", "url": "https://…", "sha256": "…", "required": true }
  ],
  "signature": "<Ed25519-Signatur über den kanonischen Inhalt>"
}
```

Der Launcher installiert nur Manifeste mit gültiger Signatur des FROSTHAIN-Schlüssels.

## 4.6 Oberfläche

Designsprache wie die Mod: Dark Fantasy, dezente Goldakzente, ruhige Animationen ([UI-Leitlinien](05-charaktereditor-konzept.md#59-ui-leitlinien-für-alle-oberflächen)).

| Bildschirm | Inhalt |
|---|---|
| **Start** | großer „Spielen“-Knopf, Status (aktuell / Update verfügbar / installiert …), letzter Charakter, kurze Neuigkeiten |
| **Charaktere & Welten** | Karten je Welt mit Charakterzusammenfassung, Sichern/Wiederherstellen |
| **Mods** | Pflicht-Mods (gesperrt), empfohlene Mods, eigene Mods; Konflikt- und Abhängigkeitshinweise |
| **Einstellungen** | Allgemein · Spiel · Java & Speicher · Erweitert |
| **Diagnose** | Systemprüfung, letzte Logs, Absturzanalyse, „Diagnosepaket erstellen“ |
| **Konto** | angemeldetes Profil, Abmelden, Konto wechseln |

## 4.7 Installation für den Endnutzer (Ziel M1.5)

1. Download von `FrosthainSetup.exe` **oder** ein PowerShell-Befehl, der Installer und Prüfsumme lädt, prüft und startet.
2. Installation pro Benutzer, ohne Adminrechte, Startmenü-Eintrag.
3. Erster Start: Systemprüfung → Anmeldung (oder Brückenmodus) → Installation der Spieldateien → „Spielen“.

Kein Gradle, kein Java-Build, keine manuelle Mod-Installation für den Endnutzer.
