# ADR 0004 · Offizielle Microsoft-Anmeldung mit eigener Azure-App, Brückenmodus als Fallback

**Status:** angenommen · **Datum:** 2026-11-01

## Kontext
Der Launcher soll offiziell anmelden, ohne unsichere Workarounds. Mojang schaltet neue Azure-App-IDs für die Minecraft-API nur nach manueller Prüfung frei.

## Entscheidung
- Eigene Azure-App-Registrierung (öffentlicher Client, OAuth 2.0 Authorization Code mit PKCE im Standardbrowser, Gerätecode als Alternative), nur die nötigen Berechtigungen.
- Tokenkette Microsoft → Xbox Live → XSTS → Minecraft Services, Besitzprüfung vor jedem Start.
- Refresh Token nur in der Windows-Anmeldeinformationsverwaltung; Access Tokens nur im Speicher; Tokens werden in Logs und Diagnosepaketen entfernt.
- Bis zur Freigabe: **Brückenmodus** – Profil im offiziellen Minecraft Launcher, Anmeldung und Start dort.
- Ausgeschlossen: fremde Client-IDs, Passworteingabe im Launcher, Offline-Konten ohne Kauf.

## Folgen
Der eigene Login ist ein externer Blocker (R1). Alle anderen Launcher-Funktionen sind davon unabhängig.
