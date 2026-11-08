# ADR 0004 · Anmeldung über den offiziellen Minecraft Launcher (Brückenmodus)

**Status:** angenommen · **Datum:** 2026-11-01 · **Ersetzt:** ursprüngliche Fassung „eigene Azure-App, Brückenmodus als Fallback“

## Kontext
Ursprünglich war eine eigene Azure-App-Registrierung mit Microsoft-Login direkt im FROSTHAIN-Launcher geplant. Der Projektinhaber hat am 01.11.2026 mitgeteilt, dass die Azure-App nicht funktioniert. Ohne eine von Mojang freigegebene App-ID kann ein Launcher keine Minecraft-Tokens erhalten. Es gibt keinen sicheren und erlaubten Weg darum herum.

## Entscheidung
1. **Hauptweg – Brückenmodus über den offiziellen Minecraft Launcher.**
   Der FROSTHAIN-Launcher übernimmt alles außer der Anmeldung: Installation, Updates, Mods, Instanz, Sicherungen, Diagnose. Er registriert eine Installation „FROSTHAIN“ im offiziellen Minecraft Launcher. Anmeldung, Besitzprüfung und Spielstart laufen dort – mit Microsofts eigener, offizieller Anmeldung. Der FROSTHAIN-Launcher öffnet den offiziellen Launcher auf Knopfdruck.
2. **Zweitweg – Modpack-Export (`.mrpack`).**
   Für Spieler, die lieber einen anderen etablierten Launcher nutzen (z. B. Prism Launcher oder Modrinth App), exportiert FROSTHAIN ein Modpack im offenen Modrinth-Format. Die Anmeldung erfolgt dann in jenem Launcher mit dessen eigener, freigegebener Registrierung.
3. **Option für später – eigene Anmeldung.**
   Der Anmeldebaustein bleibt als austauschbare Schnittstelle im Code. Gelingt später eine freigegebene Registrierung, kann er ohne Umbau ergänzt werden.

## Ausdrücklich ausgeschlossen
- Client-IDs anderer Launcher übernehmen.
- Passwörter oder Tokens im FROSTHAIN-Launcher abfragen, speichern oder aus anderen Launchern auslesen.
- Offline-Konten ohne Kauf.

## Technische Umsetzung des Brückenmodus
- Instanzdateien (Mods, Konfiguration, Welten, Logs) liegen in `%LOCALAPPDATA%\Frosthain\instances\<instanz>`. Der offizielle Launcher startet das Spiel mit diesem Spielverzeichnis (`gameDir`).
- Die Fabric-Versionsbeschreibung wird additiv in `%APPDATA%\.minecraft\versions\` abgelegt (so arbeitet auch der offizielle Fabric-Installer). Vorhandene Dateien werden nicht überschrieben.
- Das Profil wird in `launcher_profiles.json` eingetragen, bei der Microsoft-Store-Variante des Launchers zusätzlich in `launcher_profiles_microsoft_store.json`. Vor jeder Änderung wird eine Sicherungskopie angelegt. Nur der eigene Eintrag wird geändert. Fremde Profile, Welten und Einstellungen werden nicht angefasst.
- Arbeitsspeicher und Java-Argumente setzt FROSTHAIN im Profil (`javaArgs`). Die Java-Laufzeit stellt der offizielle Launcher passend zur Minecraft-Version bereit.
- Offline-Spiel: so, wie es der offizielle Launcher nach einer früheren Anmeldung erlaubt.

## Folgen
- Kein Login-Code mit Tokens im FROSTHAIN-Launcher → deutlich weniger Sicherheitsrisiko.
- Ein zusätzlicher Klick („Spielen“ im offiziellen Launcher). Ob der offizielle Launcher die zuletzt aktualisierte Installation automatisch vorauswählt, wird in M1.2 auf Windows 11 geprüft und hier nachgetragen.
- Abhängigkeit vom Profilformat des offiziellen Launchers. Gegenmaßnahme: Formatprüfung beim Lesen; bei unbekanntem Format wird nichts geschrieben, sondern eine Anleitung zum manuellen Anlegen angezeigt.
