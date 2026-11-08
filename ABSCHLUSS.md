# FROSTHAIN – Abschlussstand

Stand: 8.11.2026. FROSTHAIN wird hier eingefroren. Der Weg zur Wiederaufnahme steht unten.

## Spielbar

- Charaktererstellung mit Spezies, Aussehen, Klasse und Hintergrund, serverseitig geprüft
- Talentbäume (Taste K) für Krieger, **Magier, Schurke, Kleriker, Druide und Barde**, dazu der allgemeine Baum und der Runenpfad
- Questtracker (U) und Journal (J), NPC Eldrin mit Dialog, Hauptquests „Das Erwachen“ und „Die erste Rune“
- Startdorf mit Nebenquest „Verlorene Vorräte“, Stufenaufstieg mit Titel und Fanfare
- Dungeon „Die gefrorene Gruft“ mit Boss Hrímgar in zwei Phasen
- Fähigkeitenleiste mit Ressourcen und Abklingzeiten, Runenaltar mit fünf Runen und Runensiegeln
- Installation: ein PowerShell-Befehl, siehe `docs/11-windows-test-und-uebergabe.md`

## Am 8.11. ergänzt

Talentbäume für Magier, Schurke, Kleriker, Druide und Barde, je 10 Knoten in drei Zweigen. Sie wirken über Attribute (Zauberkraft, Rüstung, Gesundheit, Tempo, Glück) und Fertigkeiten-Übung. Neue Fähigkeiten wurden nicht erfunden, weil jede Fähigkeit Code in der Mod braucht. Die Werte orientieren sich am Krieger-Baum. Die Bäume liegen in `content/frosthain_base/data/frosthain/frosthain/talent_trees/`, die Texte auf Deutsch und Englisch in `lang/`.

## Offen bei Wiederaufnahme

Nach Priorität:

1. NPC-Inhalte von ChatGPT für das Startdorf (`content/chatgpt-sprint`, Vertrag in `docs/CONTENT_CONTRACT.md`)
2. Skin auf der Spielfigur (Sprint SOLL 3, Roadmap M2.4)
3. Talentbäume für Barbar, Mönch, Paladin, Waldläufer, Hexenmeister und Zauberer
4. Eigene Fähigkeiten für die neuen Bäume (Code in `mod`, Daten in `powers/`)
5. Weltgenerierung des Tals, Hafen und Ruinen (Roadmap M5.1 bis M5.3)
6. Vertical Slice (Version 0.7)

Den eigenen Launcher (M1.1, M1.2) würde ich für den Eigengebrauch weglassen: Installer und offizieller Minecraft Launcher reichen.

## Branches

- `claude/ui-prototyp`: aktueller Entwicklungsstand, CI grün
- `main`: nur Planung, kein Merge ohne Freigabe
- `content/chatgpt-sprint`: Content-Vertrag, noch ohne NPC-Inhalte
