# 8 · Risikoanalyse

Wahrscheinlichkeit (W) und Auswirkung (A): niedrig · mittel · hoch.

| # | Risiko | W | A | Gegenmaßnahme | Frühwarnzeichen |
|---|---|---|---|---|---|
| R1 | **Mojang gibt die Azure-App-ID nicht oder erst spät frei** – kein eigener Login | mittel | hoch | Antrag sofort stellen; Brückenmodus über den offiziellen Launcher als vollwertiger Fallback; Login-Code bleibt austauschbar | keine Antwort nach 4 Wochen |
| R2 | **Minecraft-Drops ändern APIs tiefgreifend** (26.1 GUI, 26.2 Vulkan) | hoch | hoch | eine MC-Version je Release pinnen; Regeln im `core`; dünne Adapter; Rendering nur über Vanilla-Abstraktionen; Update-Fenster einplanen | Fabric-Ankündigung „largest changes“; Snapshot-Changelogs |
| R3 | **Lizenzverstoß** durch übernommene geschützte Inhalte (D&D außerhalb SRD, BG3) | mittel | hoch | Lizenz-Sperre im Validator; Pflicht-Quelle je Definition; eigene Texte; Inhaltskatalog mit Ausschlussliste; keine Marken im Produktnamen | Inhalte ohne nachvollziehbare Quelle; Namen bekannter Nicht-SRD-Optionen |
| R4 | **Umfang übersteigt Kapazität** (mehrjähriges Projekt) | hoch | hoch | Vertical Slice vor Breite; Meilensteine mit Abnahmekriterien; kuratierte statt vollständige Fähigkeitenlisten | Meilensteine dauern doppelt so lange wie geschätzt |
| R5 | **Weltbau-Qualität** – Strukturen wirken generisch | mittel | mittel | Strukturvorlagen iterativ mit Screenshots; Option auf menschliche Bauer; Bau- und Technikaufgaben trennen | Spieltest-Feedback „leer“, „langweilig“ |
| R6 | **Spielstandverlust** durch Updates oder Migrationen | niedrig | hoch | Schemaversionen; Migration auf Kopie; Backup vor Speichern; neuere Formate werden nicht geladen; automatische Weltsicherung im Launcher | Fehlermeldungen beim Laden; Testkorpus schlägt fehl |
| R7 | **Sicherheitsfehler im Launcher** (Token-Leck, manipulierte Downloads) | niedrig | hoch | Anmeldeinformationsverwaltung; Log-Filter mit Test; Prüfsummen für jede Datei; signierte Manifeste und Updates; Sicherheitsreview je Release | Token-Muster im Diagnosepaket |
| R8 | **SmartScreen/Antivirus-Warnungen** beim Installer | hoch | mittel | Code-Signatur (Trusted Signing oder OV-Zertifikat); reproduzierbare Builds; Prüfsummen veröffentlichen | Nutzerberichte über Warnungen |
| R9 | **Performance** durch Effekte, Dialogkameras, Compositing | mittel | mittel | Performance-Budget; Textur-Cache; Effekt-Qualitätsstufen; Messungen im CI | Tick-Zeit > Budget |
| R10 | **Mod-Inkompatibilitäten** (Mixin-Konflikte, Rendering-Mods) | mittel | mittel | wenige Mixins; kuratierte Kompatibilitätsliste mit CI-Start-Test; klare Kennzeichnung „nicht unterstützt“ | Absturzberichte mit Fremd-Mods |
| R11 | **Abhängigkeiten ohne 26.3-Unterstützung** (z. B. GeckoLib) | mittel | mittel | keine Pflichtabhängigkeit außer Fabric API; Animationen mit Vanilla-Mitteln | fehlende Builds für die Zielversion |
| R12 | **Balancing** von Multiclass-Kombinationen | hoch | mittel | gemeinsame Zauberstufe; Klassenstufen-Bedingungen; Ausschlusslisten; Spieltests | dominante Builds im Spieltest |
| R13 | **Einsteigerfreundlichkeit** – D&D-Regeln überfordern | mittel | hoch | Schnellstart-Helden; Erklärungen je Schritt; schrittweise Freischaltung; Sperrgründe in Klartext | Abbrüche im Editor-Spieltest |
| R14 | **Ausfall der Cloud-Arbeitsumgebung** – Arbeit geht verloren | mittel | mittel | Remote-Repository und regelmäßige Pushes; bis dahin liegt alles nur im flüchtigen Container | kein Remote konfiguriert (aktuell der Fall) |
| R15 | **Kein Zugriff auf Zielumgebung** (Windows, Minecraft-Server) für Tests | hoch | mittel | Windows-CI-Runner; Checklisten für manuelle Tests durch den Inhaber; Ergebnisse nur nach tatsächlichem Test als bestanden markieren | ungeprüfte Annahmen in Doku |
