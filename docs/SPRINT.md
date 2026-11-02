# 48-Stunden-Sprint – Übergabe

Start: 2026-11-02 · Branch: `claude/ui-prototyp` (kein Merge nach `main` ohne Freigabe) · Inhalte: Branch `content/chatgpt-sprint`, Paket `content/frosthain_story/`, Vertrag [CONTENT_CONTRACT.md](CONTENT_CONTRACT.md)

## Prioritäten

| | Aufgabe | Status |
|---|---|---|
| MUSS 1 | Spielbares Startdorf mit NPCs | 🔶 Dorf (Platz, Brunnen, 4 Häuser, Wege), Eldrin auf dem Platz, Plätze für 4 NPCs aus `npcs/village/*`; NPC-Inhalte von ChatGPT ausstehend |
| MUSS 2 | Nebenquest „Verlorene Vorräte“ mit Kisten | 🔶 Anschlagbrett startet, 3 Kisten im Wald, Wegweiser, +80 EP; im Client-GameTest |
| MUSS 3 | Stufenaufstieg | 🔶 automatisch bei genug EP: Titel „Stufe N“ (Cinzel, Gold), Untertitel mit Talentbaum-Hinweis, Fanfare, Partikel, volle Gesundheit, Chatzeile; Screenshot im Client-GameTest |
| MUSS 4 | Dungeon mit Boss | 🔶 „Die gefrorene Gruft“ (Folgequest von „Die erste Rune“): Ruine, Treppe, Gang, 2 Wachräume (Strays, Zombies), Grabkammer; Boss Hrímgar mit Bossleiste, Frostnova mit Vorwarnung, Phase 2 bei 50 % (Bogen, 2 Diener); Client-GameTest im CI |
| MUSS 5 | ChatGPT-Inhalte integriert und validiert | ⏳ wartet auf `content/chatgpt-sprint` |
| MUSS 6 | Vorabversion über GitHub Releases | 🔶 `frosthain-dev` wird bei grünem CI automatisch erneuert |
| SOLL 1 | Talentbäume für 2–3 weitere Klassen | ⏳ |
| SOLL 2 | Runenmagie (Grundlage) | 🔶 Inhaltstyp `runes` (5 Runen), Runenaltar am Schrein nach „Die erste Rune“, Herstellen serverseitig geprüft, Runensiegel mit Wirkung (Feuer, Eis, Schutz, Heilung, Bewegung); Kern-Tests + Client-GameTest; Siegel/Rituale-Reiter noch nicht |
| SOLL 3 | Skin auf der Spielfigur | ⏳ |
| KANN 1 | Eigenständiger Launcher | ⏳ |

## Regeln

Nur getestet = fertig · Client-Test lokal nur am Phasenende, sonst GitHub Actions · nicht bauen, solange Minecraft läuft · Gradle sparsam (`--max-workers=2`, Daemon nach Build beenden) · Codex nur bei kritischen Problemen oder Meilenstein-Reviews.

## Protokoll

- 2026-11-02: Content-Vertrag erstellt, Beispiele mit Validator geprüft (0 Fehler).
- 2026-11-02: `StarterVillage` – Dorf wird in neuen Welten beim ersten Betreten am Spawn gebaut (`/frosthain dorf` für bestehende Welten), Nebenquest mit Kisten. Kompiliert; Client-GameTest läuft im CI.
- 2026-11-02: Dorf-Commit im CI grün (Client-GameTest mit Dorf, Anschlagbrett, Kisten).
- 2026-11-02: `FrozenCrypt` + allgemeine `defeat`-Ziele (`QuestService.onDefeat`: Entity-Typ, `#tag`, Entity-Tag `frosthain.id.<id>`). `/frosthain gruft` baut die Gruft an der eigenen Position.
- 2026-11-02: CI-Fehler im Gruft-Commit: Testwelt ist flach (Oberfläche −60, Weltboden −64), Gruft lag unter der Welt, Boss fiel ins Leere. Tiefe jetzt begrenzt auf den Platz über dem Weltboden. Boss-Ansagen und Stufenaufstieg über die Titelanzeige (`Announce`), nicht mehr hinter dem Chat. Client-GameTest lokal grün (alle 4 Quests abgeschlossen), Mod in die Instanz installiert.
- 2026-11-02: Runenmagie-Grundlage. Abweichung vom Referenzbild: kein eigener Altar-Block (Runenstein am Schrein wird zum Altar), Siegel sind glänzendes Papier statt eigenem Item, Reiter „Siegel“/„Rituale“ entfallen bis es Inhalte dafür gibt.

## Nächster Schritt

Skin auf der Spielfigur (SOLL 3) oder Talentbäume weiterer Klassen (SOLL 1, falls ChatGPT keine liefert). ChatGPT-Branch prüfen.
