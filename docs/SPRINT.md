# 48-Stunden-Sprint – Übergabe

Start: 2026-11-02 · Branch: `claude/ui-prototyp` (kein Merge nach `main` ohne Freigabe) · Inhalte: Branch `content/chatgpt-sprint`, Paket `content/frosthain_story/`, Vertrag [CONTENT_CONTRACT.md](CONTENT_CONTRACT.md)

## Prioritäten

| | Aufgabe | Status |
|---|---|---|
| MUSS 1 | Spielbares Startdorf mit NPCs | ⏳ in Arbeit |
| MUSS 2 | Nebenquest „Verlorene Vorräte“ mit Kisten | ⏳ |
| MUSS 3 | Stufenaufstieg | 🔶 automatisch mit Meldung vorhanden, Dialog/Effekt offen |
| MUSS 4 | Dungeon mit Boss | ⏳ |
| MUSS 5 | ChatGPT-Inhalte integriert und validiert | ⏳ wartet auf `content/chatgpt-sprint` |
| MUSS 6 | Vorabversion über GitHub Releases | 🔶 `frosthain-dev` wird bei grünem CI automatisch erneuert |
| SOLL 1 | Talentbäume für 2–3 weitere Klassen | ⏳ |
| SOLL 2 | Runenmagie (Grundlage) | ⏳ |
| SOLL 3 | Skin auf der Spielfigur | ⏳ |
| KANN 1 | Eigenständiger Launcher | ⏳ |

## Regeln

Nur getestet = fertig · Client-Test lokal nur am Phasenende, sonst GitHub Actions · nicht bauen, solange Minecraft läuft · Gradle sparsam (`--max-workers=2`, Daemon nach Build beenden) · Codex nur bei kritischen Problemen oder Meilenstein-Reviews.

## Protokoll

- 2026-11-02: Content-Vertrag erstellt, Beispiele mit Validator geprüft (0 Fehler).

## Nächster Schritt

Startdorf: Gebäude am Spawn, NPCs aus `npcs/village/*` platzieren (generisch statt nur Eldrin), Dialoge per Rechtsklick.
