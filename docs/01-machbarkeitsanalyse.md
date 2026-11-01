# 1 · Technische Machbarkeitsanalyse

Stand: 01.11.2026 · Grundlage: Auftrag „FROSTHAIN – Complete Rebuild“, Recherche in dieser Sitzung, tatsächlich gebauter und getesteter RPG-Kern (siehe [09](09-erster-implementierungsschritt.md)).

## 1.1 Ergebnis auf einen Blick

| Anforderung | Bewertung | Kernaussage |
|---|---|---|
| Eigener Windows-Launcher | 🟢 machbar | Tauri 2 (Rust + Web-UI), ca. 10–20 MB statt ~150 MB bei Electron. |
| Offizielle Microsoft-Anmeldung im eigenen Launcher | 🔴 zurückgestellt (Stand 01.11.2026) | Die eigene Azure-App funktioniert nicht. Angemeldet wird im offiziellen Minecraft Launcher (Brückenmodus, ADR 0004). |
| Offline-Spiel | 🟡 eingeschränkt | Nur Einzelspieler und nur nach einer früheren, gültigen Anmeldung auf diesem Gerät. Keine „Cracked“-Konten. |
| Automatische Installation Minecraft/Fabric/Mods | 🟢 machbar | Offizielle Manifeste von Mojang und Fabric, SHA-1-Prüfung jeder Datei. |
| Modmanager mit Abhängigkeitsprüfung | 🟢 machbar | `fabric.mod.json` (depends/breaks) + Modrinth-Metadaten. Garantierte Kompatibilität nur für eine kuratierte Liste. |
| Charaktereditor mit drehbarer 3D-Vorschau | 🟢 machbar | Eigener Bildschirm im Spiel, der das echte Spielermodell rendert. |
| Aussehen in „Baldur's-Gate-3-Tiefe“ | 🟡 angepasst | Minecraft-Figuren bestehen aus Quadern mit 64×64-Textur. Machbar: Farben, Gesichts-/Haar-/Bartvarianten, Narben, Tattoos, Ohren, Hörner, Schweife, Größe. **Nicht** machbar: stufenlose Gesichtsformung, freie Körperproportionen. |
| Alle offiziellen D&D-Klassen, Unterklassen, Spezies | 🔴 nicht wie gefordert | Rechtlich nutzbar ist das SRD 5.2.1 (CC-BY-4.0): 12 Klassen mit je **einer** Unterklasse, 9 Spezies, 4 Hintergründe. Weitere offizielle Inhalte (z. B. Kampfmeister, Arkaner Betrüger, Aasimar, Artificer) sind geschützt. Lücken füllen eigene FROSTHAIN-Inhalte. |
| Datengetriebene Registry | 🟢 umgesetzt | Im Kern implementiert und getestet; neue Klassen sind reine JSON-Dateien. |
| Verzweigter Talentbaum, Multiclassing, eigene EP | 🟢 Regeln umgesetzt | Regelwerk im Kern fertig und getestet; Oberfläche folgt in Phase 3. |
| Echtzeitkampf-Erweiterung, Zauber, Runenmagie | 🟢 machbar, großer Umfang | Eine kuratierte Auswahl von Fähigkeiten statt aller ~300 SRD-Zauber. |
| Gegner-Verhalten und Bosskämpfe | 🟡 aufwendig | Eigene Entities mit eigener KI; jeder Boss ist ein eigenes kleines Projekt. |
| Startregion: Tal, Berge, Meerzugang, geheimer Hafen | 🟡 technisch machbar | Strukturvorlagen + Worldgen-Datapacks. Der Engpass ist der **Bau** (künstlerische Arbeit), nicht die Technik. |
| Quests, Wegpunkte, NPC-Dialoge | 🟢 machbar | Eigenes Quest- und Dialogsystem; eigene Wegpunkt-Anzeige, Minimap-Integration optional. |
| Deutsch primär, weitere Sprachen vorbereitet | 🟢 umgesetzt | Übersetzungsschlüssel aus IDs; Prüfung auf fehlende Texte im Validator. |
| Installation: 1 Download + 1 PowerShell-Befehl | 🟢 machbar | Ohne Code-Signaturzertifikat zeigt Windows SmartScreen Warnungen (§1.3.9). |
| Zielversion Minecraft Java 26.3 + Fabric | 🟡 plausibel, hier nicht verifizierbar | 26.3 erschien laut mehreren Sekundärquellen am 15.09.2026. Mojang- und Fabric-Server sind aus dieser Cloud-Umgebung gesperrt; die Fabric-Toolchain wird in M0.3 verifiziert. |

## 1.2 Geprüfte Fakten und ihr Verifikationsstand

| Fakt | Quelle | Status |
|---|---|---|
| Minecraft Java 26.3 („Wilderness Bound“) erschien am 15.09.2026 | Sekundärquellen (z. B. jeu.video, minecraft.how, bisecthosting.com) | Nicht gegen minecraft.net geprüft (Host gesperrt). |
| Seit 26.1 ist Minecraft **nicht mehr obfuskiert**; Fabric nutzt keine Yarn-Mappings mehr, sondern direkt Mojangs Namen | Fabric-Dokumentation „Migrating Mappings“, Fabric-Blog 14.03.2026 | Aus Suchergebnissen; Seiten selbst nicht abrufbar. |
| Minecraft 26.x verlangt **Java 25**; empfohlen: Loom 1.15+, Gradle 9.4, Plugin `net.fabricmc.fabric-loom` (ohne Remapping) | Fabric-Blog zu 26.1 | Wie oben. Gradle 9.4 + JDK 25 hier **praktisch getestet** (Kern baut). |
| 26.1 hat das GUI-Rendering umgebaut (`GuiGraphics` → `GuiGraphicsExtractor`, `render` → `extractRenderState`); 26.2 bringt ein Vulkan-Backend, direktes OpenGL bricht | Fabric-Doku „Basic Rendering Concepts“, NeoForge-Primer 26.2, Mod-Changelogs | Aus Suchergebnissen. Konsequenz: nur über Vanilla-Abstraktionen rendern. |
| Fabric API hatte Builds für 26.3-Snapshots (z. B. `0.158.0+26.3`) | Modrinth-Spiegelseiten | Finale 26.3-Version nicht verifiziert. |
| GeckoLib unterstützt laut Aggregatoren bis 26.2; 26.3 unklar | Modrinth-/CurseForge-Spiegel | Nicht verifiziert → GeckoLib keine Pflichtabhängigkeit. |
| Neue Azure-App-IDs für die Minecraft-API werden von Mojang manuell auf eine Allow-List gesetzt; nicht freigegebene IDs erhalten „Invalid app registration“ | Mojang-Hilfeartikel „Java Edition Game Service API Review or Application Process“, Entwicklerdokumentationen | Aus Suchergebnissen; Formular nicht selbst eingesehen. |
| SRD 5.2.1 steht unter CC-BY-4.0; enthält 12 Klassen mit je einer Unterklasse, 9 Spezies; Hintergründe Akolyth, Krimineller, Gelehrter, Soldat | D&D Beyond (dndbeyond.com/srd), Sekundärquellen | Liste aus Sekundärquellen; vor Release gegen das Original-PDF prüfen (Aufgabe in M3.1). |

## 1.3 Bewertung der einzelnen Bereiche

### 1.3.1 Launcher
Ein eigenständiger Launcher ist Standardtechnik (Prism Launcher, ATLauncher und andere zeigen, dass es geht). Die Bausteine sind öffentlich dokumentiert: Versionsmanifest, Bibliotheken mit Betriebssystem-Regeln, Asset-Index, Java-Laufzeit, Fabric-Loader-Profil. **Rechtlich wichtig:** Spieldateien dürfen nicht weiterverteilt, sondern müssen von Mojang geladen werden – das ist im Konzept so vorgesehen.

### 1.3.2 Anmeldung (kritischer Pfad)
Der offizielle Weg ist: Microsoft OAuth 2.0 → Xbox Live → XSTS → Minecraft Services → Besitzprüfung → Profil. Technisch einfach, **organisatorisch blockiert**, bis Mojang die eigene Azure-App-ID freigibt. Die Wartezeit ist unbekannt.

Was ausdrücklich **nicht** gemacht wird: fremde Client-IDs anderer Launcher wiederverwenden, Anmeldedaten im Launcher abfragen, „Offline-Konten“ ohne Kauf. Das wären genau die unsicheren Workarounds, die der Auftrag verbietet.

**Lösung in zwei Stufen:**
1. *Sofort nutzbar:* **Brückenmodus** – der FROSTHAIN-Launcher installiert und verwaltet alles (Instanz, Mods, Backups, Diagnose) und legt im offiziellen Minecraft Launcher ein Profil an (mit Sicherung von dessen Profildatei). Angemeldet und gestartet wird über den offiziellen Launcher. Genau so arbeitet auch der offizielle Fabric-Installer.
2. *Nach Freigabe:* eigener Login und Start direkt aus dem FROSTHAIN-Launcher.

Die Freigabe muss der Projektinhaber beantragen (Azure-Konto, App-Registrierung, Formular). Das sollte **sofort** passieren, weil es die längste externe Wartezeit im Projekt ist.

> **Update 01.11.2026:** Die Azure-App funktioniert nicht. Stufe 2 ist zurückgestellt; der Brückenmodus ist jetzt der Hauptweg, ergänzt um einen `.mrpack`-Export für andere Launcher ([ADR 0004](adr/0004-authentifizierung.md)).

### 1.3.3 Charaktereditor
- **3D-Vorschau:** Minecraft kann Entities in GUI-Bildschirmen rendern (wie im Inventar). Drehen per Maus/Stick und Zoomen sind damit machbar, ohne Fremdbibliothek.
- **Aussehen:** Hautfarbe, Augen, Mund, Haare, Bart, Narben, Tattoos und magische Markierungen werden zur Laufzeit zu einer 64×64-Spielertextur zusammengesetzt (Ebenen-Compositing). Ohren, Hörner, Schweife, Hauer, Schuppen sind zusätzliche Modellteile, die am Spielermodell hängen und mit animiert werden.
- **Proportionen:** Größe ist über das Attribut `minecraft:scale` möglich (wirkt auf Hitbox – gewollt für Halblinge, Gnome, Goliaths) plus ±5 % rein optische Feinanpassung. Freie Proportionen (lange Beine, breite Schultern) würden Rüstungs- und Animationsdarstellung brechen und werden nicht angeboten.
- **Kleidung/Rüstung:** Kosmetische Outfits und Farbanpassungen sind machbar; die Darstellung echter Rüstungen bleibt Vanilla-kompatibel.

### 1.3.4 Klassen, Spezies, Lizenzen
Nutzbar und bereits als Daten angelegt: alle 12 SRD-Klassen, 12 SRD-Unterklassen, 9 SRD-Spezies mit Abstammungslinien, 4 SRD-Hintergründe, 18 Fertigkeiten, Attributsregeln (Standardreihe, Punktekauf). Ergänzt durch 2 eigene Unterklassen und 2 eigene Hintergründe als Beispiel. Eine **Lizenz-Sperre im Validator** verhindert technisch, dass Inhalte aus ungeprüften oder abgelehnten Quellen ausgeliefert werden. Details: [Inhaltskatalog](inhaltskatalog-und-lizenzen.md).

### 1.3.5 Talentbaum, Erfahrung, Multiclassing
Vollständig im Minecraft-unabhängigen Kern umgesetzt: Talentpunkte pro Stufe, Voraussetzungen (alle/eine von), Exklusivgruppen für Schlüsseltalente, Klassen- und Charakterstufenbedingungen, Rücknahme nur ohne abhängige Talente, Komplett-Reset, automatische Bereinigung nach Inhaltsupdates. Multiclassing mit Attributsvoraussetzungen, Klassenlimit, Mindeststufe, Ausschlusslisten und gemeinsamer Zauberstufe. Eigene Erfahrungspunkte, unabhängig von Vanilla-XP.

### 1.3.6 Kampf und Magie
Minecraft-Kampf bleibt die Basis. Ergänzt werden aktive Fähigkeiten mit Ressourcen und Abklingzeiten, Ausweichen, Statuseffekte und Runenmagie. Machbar, aber umfangreich. **Empfehlung:** Für Version 1.0 eine kuratierte Auswahl von etwa 40–60 Fähigkeiten über alle Klassen, statt SRD-Zauberlisten vollständig nachzubauen. Partikeleffekte bewusst sparsam, mit Qualitätsstufe in den Einstellungen.

### 1.3.7 Welt und Story
Technisch: eigenes Biom und eigene Geländeform für das Tal (Worldgen-Datapack), Strukturvorlagen (NBT) für Dorf, Hafen, Ruinen und Dungeons, platziert über das Jigsaw-/Strukturen-System bei der Weltgenerierung. Die Welt bleibt unendlich und frei bebaubar. **Der eigentliche Engpass ist das Bauen selbst.** Claude Code kann Strukturen programmatisch erzeugen und iterieren, aber hochwertige Architektur braucht Iterationen mit Screenshots oder menschliche Bauer.

### 1.3.8 Quests, NPCs, Dialoge, Minimap
Eigenes, datengetriebenes Questsystem, ausgelöst durch Spielereignisse (Ort betreten, Gegenstand erhalten, NPC ansprechen, Gegner besiegen). Dialoge als Graphen mit Bedingungen und Konsequenzen (Ruf, Questfortschritt, Fertigkeitsproben). Wegpunkte erscheinen als eigene dezente Markierung am Bildschirmrand bzw. auf einem Kompass; die Anbindung an eine verbreitete Minimap-Mod wird geprüft (nur über deren offizielle API und nur bei passender Lizenz).

### 1.3.9 Installation
„Ein Download + ein PowerShell-Befehl“ ist machbar: ein signiertes Installationsprogramm (pro Benutzer, ohne Administratorrechte) plus ein PowerShell-Skript, das Download und Prüfsumme übernimmt. **Hinweis:** Ohne Code-Signaturzertifikat warnt Windows SmartScreen. Günstigste saubere Lösung: Microsoft Trusted Signing oder ein OV-Zertifikat (laufende Kosten, Entscheidung des Projektinhabers).

### 1.3.10 Multiplayer (später)
Wird nicht jetzt gebaut, aber jetzt ermöglicht: Alle RPG-Logik läuft auf dem (auch im Einzelspieler vorhandenen, integrierten) Server, der Client zeigt nur an und sendet Wünsche. Jede Aktion wird serverseitig geprüft (bereits im Kern so gebaut: z. B. lehnt `CharacterFactory` ungültige Charaktere ab).

## 1.4 Konstruktiv hinterfragte Anforderungen

1. **„Alle offiziellen D&D-Klassen und Unterklassen“** → nur SRD + eigene + nachweislich freigegebene Community-Inhalte. Die Registry macht späteres Ergänzen trivial, sobald Rechte vorliegen.
2. **„Charaktereditor wie Baldur's Gate 3“** → Tiefe in Optionen und Bedienung ja, Gesichtsmodellierung nein. Ein ehrlicher Minecraft-Stil wirkt besser als ein halber Realismus-Versuch.
3. **„Offizielle Anmeldung im eigenen Launcher“** → sofort beantragen; bis zur Freigabe Brückenmodus.
4. **„Modmanager zum Hinzufügen beliebiger Mods“** → technisch ja, aber Kompatibilität wird nur für eine kuratierte Liste zugesagt; alles andere läuft als „nicht unterstützt“ gekennzeichnet.
5. **„Minecraft 26.3“** → ja, aber Mojang veröffentlicht etwa vierteljährlich „Drops“ mit tiefen API-Änderungen (26.1: GUI-Rendering, 26.2: Vulkan). Strategie: eine Minecraft-Version pro FROSTHAIN-Release festpinnen, nicht jedem Drop hinterherlaufen; Minecraft-Code in dünnen Adaptern halten.
6. **Reihenfolge der Phasen** → Phase 1 (Launcher) wartet in ihrem kritischsten Teil auf externe Freigabe. Daher wurde der Minecraft-unabhängige RPG-Kern vorgezogen; der Launcher startet parallel mit der Installationslogik.
7. **Umfang** → Das Gesamtziel ist ein mehrjähriges Projekt. Damit früh etwas Spielbares entsteht, ist ein **Vertical Slice** eingeplant: Startdorf, 6 Klassen mit Talentbäumen, 1 Dungeon, 1 Boss, ~10 Quests, kompletter Editor (siehe [Roadmap](07-roadmap.md)).

## 1.5 Grenzen dieser Arbeitsumgebung

Diese Sitzung läuft in einem Linux-Cloud-Container, nicht auf dem Windows-11-Rechner des Projektinhabers.

- **Gesperrte Hosts:** `piston-meta.mojang.com`, `libraries.minecraft.net`, `resources.download.minecraft.net`, `meta.fabricmc.net`, `maven.fabricmc.net`, `fabricmc.net`, `api.adoptium.net`, `downloads.gradle.org`. Folge: Die Fabric-Mod kann hier nicht gebaut werden.
- **Verfügbar:** JDK 25 (aus Ubuntu-Paketen), Gradle 9.4 (über GitHub), Maven Central (zeitweise gedrosselt), Node 22, Rust 1.97, crates.io, npm.
- **Nicht testbar hier:** Windows-Builds des Launchers, das Spiel selbst. Dafür ist CI auf Windows-Runnern vorgesehen.
