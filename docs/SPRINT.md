# 48-Stunden-Sprint – Übergabe

Start: 2026-11-02 · Branch: `claude/ui-prototyp` (kein Merge nach `main` ohne Freigabe) · Inhalte: Branch `content/chatgpt-sprint`, Paket `content/frosthain_story/`, Vertrag [CONTENT_CONTRACT.md](CONTENT_CONTRACT.md)

## Prioritäten

| | Aufgabe | Status |
|---|---|---|
| MUSS 1 | Spielbares Startdorf mit NPCs | 🔶 Dorf (Platz, Brunnen, 4 Häuser, Wege), Eldrin auf dem Platz, Plätze für 4 NPCs aus `npcs/village/*`; NPC-Inhalte von ChatGPT ausstehend |
| MUSS 2 | Nebenquest „Verlorene Vorräte“ mit Kisten | 🔶 Anschlagbrett startet, 3 Kisten im Wald, Wegweiser, +80 EP; im Client-GameTest |
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
- 2026-11-02: `StarterVillage` – Dorf wird in neuen Welten beim ersten Betreten am Spawn gebaut (`/frosthain dorf` für bestehende Welten), Nebenquest mit Kisten. Kompiliert; Client-GameTest läuft im CI.

## Nächster Schritt

CI-Ergebnis des Dorf-Commits prüfen, dann Dungeon mit Boss (MUSS 4): Ruine nach „Die erste Rune“, Bossgegner mit 2 Phasen, Hauptquest mit `defeat`-Ziel.
