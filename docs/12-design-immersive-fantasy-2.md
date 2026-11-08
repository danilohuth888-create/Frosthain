# 12 · Design „Immersive Fantasy 2.0“

Stand: 01.11.2026 · Verbindliche Grundlage: `docs/referenzen/FROSTHAIN_Immersive_Fantasy_2.png`, `FROSTHAIN_UI_16_Ansichten.png`, `FROSTHAIN_UI_Weitere_Referenzen.png`, `FROSTHAIN_UI_B_D_F.png`. Ersetzt die stark verzierte Richtung aus [10](10-ui-implementierungsplan.md) §10.3 (Logik und Bedienung aus 10 gelten weiter).

## Prinzipien

Die Welt steht im Mittelpunkt, Minecraft bleibt erkennbar. Wenige, ruhige Flächen; dünne Linien statt Ornamentrahmen; Ornamente nur an Titeln. Konsistente Abstände (Raster 4 px), ruhige Übergänge (150–250 ms).

| Token | Wert | Einsatz |
|---|---|---|
| Schiefer | `#17191D` (Panels 90 %, HUD 70 %) | Fenster, HUD-Felder |
| Pergament / Tinte | `#E7DABF` / `#3A2F22` | Questjournal, Geschichten, Schriftrollen |
| Altgold | `#9C8452`, hell `#D2B172` | Titel, Auswahl, besondere Elemente |
| Runenblau | `#4FA3FF` | Magie, Runen, Fähigkeiten |
| Waldgrün | `#5FA54A` | Natur, Überleben, „freigeschaltet“ |
| Schrift | Cinzel (Titel), EB Garamond (Text) | beide SIL OFL 1.1 |

Umgesetzt in `ui/src/main/java/net/frosthain/ui/Theme.java`.

## Ansichten aus den Referenzen

| Ansicht | Kernpunkte | Grundlage im Code | Status im Spiel |
|---|---|---|---|
| Launcher (Desktop) | Navigation links, großer „Spielen“-Knopf, Status „Alles aktuell“, Version | Konzept 04, Installer | ❌ (Installer ✅, Launcher-App offen) |
| Hauptmenü | Einzelspieler, Mehrspieler, Mods/Charakter, Einstellungen | – | ❌ |
| Charaktererstellung (Spezies, Klasse, Aussehen, Hintergrund, Fertig) | großes Minecraft-Modell, Kacheln, Vorteile | `CharacterFactory`, `Appearance` | ❌ |
| Survival-HUD | Minimap, Koordinaten, **Questtracker standardmäßig eingeklappt** | `HudPolicy`, `QuestTrackerView` | ❌ (Prototyp ✅) |
| Talentbaum (K) | Reiter Kampf/Magie/Handwerk/Überleben/Runen, Pfadliste, Detailtafel | `TalentTreeView`, `TalentAllocator` | ❌ (Prototyp ✅) |
| Fähigkeitenleiste | 6 Plätze, gesperrte Plätze | `PowerDefinition` (nur Daten) | ❌ |
| Fähigkeitsdetails | Kosten, Abklingzeit, Reichweite, Schaden, Verbesserungen I–III | `PowerDefinition` | ❌ |
| Questjournal (Reisetagebuch) | Haupt-/Nebenquests/Abgeschlossen, Checkliste, Belohnungen | `QuestLog`, `QuestRules` | ❌ |
| NPC-Dialog | NPC in seiner Umgebung, Antworten rechts, HUD aus | `DialogueSession`, `DialogueView` | ❌ (Prototyp ✅) |
| Inventar & Ausrüstung | Modell, Slots, Werte | – | ❌ |
| Karte & Wegpunkte | Dorf, Schrein, Runenaltar, Boss, eigene Marker | – | ❌ |
| Runenmagie & Runenaltar | Runen, Siegel, Rituale, Materialien | Runenpfad (Daten) | ❌ |
| Handwerk & Spezialrezepte | Runenrelais, Siegelaltar, Runenkompass | Handwerk-Zweig (Daten) | ❌ |
| Charakter-Übersicht | Profil, Attribute, Werte, XP | `CharacterSheet` | ❌ |
| Einstellungen | HUD-Transparenz, Tracker/Minimap an/aus, Tasten | `HudPolicy.Settings`, `KeyBindingPlan` | ❌ |

## Abweichungen

- **Handwerker** erscheint in den Bildern als Klasse; laut Entscheidung des Inhabers bleibt Handwerk ein Talentzweig für alle Klassen.
- **Wolfsmensch** und **Halbelf** erscheinen als Spezies. Wolfsmensch wäre eine eigene FROSTHAIN-Spezies; Halbelf ist nicht im SRD 5.2.1 enthalten und wird vor Aufnahme lizenzrechtlich geprüft (eigene Fassung möglich).
- Die Tastenbelegung **C** für „Charakter“ (Einstellungen-Bild) kollidiert mit der Vanilla-Kreativtaste „Symbolleiste speichern“; Standard bleibt `I`, frei änderbar.
- Die kleinen Schnellknöpfe im HUD-Bild werden weggelassen (Vorgabe: keine überflüssigen Buttons).
