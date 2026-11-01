# 10 · UI-Implementierungsplan: HUD (B), Talentbaum (D), NPC-Dialoge (F)

Stand: 01.11.2026 · Grundlage: Referenzbild `docs/referenzen/referenzbilder_B_D_F.webp`, Spezifikationen `docs/referenzen/B_Survival_HUD.md`, `D_Talentbaum.md`, `F_NPC_Dialoge.md`, schematische Vorschau `UI_Mockups.html`, Architektur [02](02-architektur.md).

> Das Referenzbild ist eine Konzeptgrafik mit Shader-Beleuchtung. Übernommen werden Layout, Hierarchie, Farben, Formen und Bedienung, **nicht** die Licht- und Weltdarstellung, die von Minecraft und den Grafikeinstellungen des Spielers abhängt.

## 10.1 Analyse der Referenzbilder

### B · Survival-HUD
| Element im Bild | Position | Umsetzung |
|---|---|---|
| Runde Minimap mit Kompassring (N/O/S/W), Spielerpfeil | oben rechts | eigene, schlanke Minimap (Phase 4, M4.4); im Prototyp als Platzhalter |
| Koordinaten „X: 1284 Z: −732“, Regionsname „Tal von Frosthain“ | unter der Minimap | Koordinaten abschaltbar; Region aus Weltgenerierung (Phase 5) |
| Questtracker: Abschnitte „Hauptquest“, „Nebenquest“; Questname mit ◆; Ziel mit ☐ und Fortschritt „(0/3)“; Pfeil ⌄ unten | rechts unter der Minimap, **nicht breiter als die Minimap** | `QuestTrackerView` (umgesetzt im Prototyp) |
| Vanilla-Leisten: Hotbar, Herzen, Rüstung, Hunger, Erfahrungsstufe | unten Mitte | bleiben unverändert sichtbar |
| Logo „FROSTHAIN · Das Erwachen“ | oben links | **Kapitelbanner**: erscheint beim Betreten eines Kapitels/einer Region und blendet nach einigen Sekunden aus (dauerhaft wäre gegen „wenige permanente Elemente“) |
| Keine Ressourcen- oder Fähigkeitsleisten sichtbar | – | Ressourcenleiste nur bei Nutzung oder im Kampf einblenden (Phase 4) |

### D · Talentbaum
| Element im Bild | Umsetzung |
|---|---|
| Kopf: Logo „FROSTHAIN · Talente“ | Bildschirmtitel |
| Reiter „Kampf · Magie · Handwerk · Überleben“ | Reiter = **Zweige des gewählten Baums**; Klick zentriert und hebt den Zweig hervor, erneuter Klick zeigt wieder den ganzen Baum |
| Links: „Verfügbare Punkte 3“, „Charakter Stufe 12“ | Punkte- und Stufenanzeige aus `TalentAllocator` |
| Links: Liste „Krieger, Runenmagier, Waldläufer, Handwerker“ | Liste der **Pfade**: allgemeiner Baum, Runenpfad, Klassenbäume. Bäume nicht gewählter Klassen sind als Vorschau sichtbar (gesperrt) |
| Mitte: goldener Wurzelknoten (Baum-Emblem), vier farbige Quadranten (rot oben links, blau oben rechts, grün unten links, violett unten rechts), sechseckige Knoten mit Symbolen, leuchtende Verbindungen, graue Knoten mit Schloss | radiales Layout um den Wurzelknoten; Zweigfarben aus dem Theme; Zustände gesperrt/verfügbar/freigeschaltet |
| Rechts: Symbol, „Kampfhaltung“, „Passives Talent“, Beschreibung, „Freischaltbar“ (grün), „Kosten: 1 Talentpunkt“, Knopf „Talent freischalten“ | Detailtafel mit Effekt, Voraussetzungen, Kosten, Rang-Fortschritt und Sperrgründen (Spezifikation D) |
| Goldrahmen mit Eckornamenten | gemeinsamer Rahmen aller FROSTHAIN-Fenster |

Hinweis: Im Bild steht „Passiver Talent“; korrekt ist „Passives Talent“.

### F · NPC-Dialog
| Element im Bild | Umsetzung |
|---|---|
| Kein HUD sichtbar | Hotbar, Herzen, Hunger, Rüstung, Erfahrungsleiste während des Dialogs ausgeblendet, danach wiederhergestellt |
| Großes NPC-Porträt links, ragt über die Tafel hinaus | im Spiel: 3D-Ansicht des NPC-Modells (gleiches Aussehen-System wie Spieler); im Prototyp: Pixel-Porträt |
| Namensschild „Eldrin“ in goldgerahmtem Reiter | Sprechername aus NPC-Definition |
| Dialogtext groß, Serifenschrift | EB Garamond, Größe skalierbar |
| Antworten rechts, aktive Antwort mit Goldrahmen und ▸ | Auswahl per Maus, Pfeiltasten, Ziffern 1–9, Enter |
| breite Tafel unten mit Ornamentrahmen | gemeinsamer Rahmen |

## 10.2 Abweichungen und offene Entscheidungen

| # | Thema | Vorgabe | Umsetzung | Begründung |
|---|---|---|---|---|
| A1 | Taste für NPC-Interaktion | F: „Standardwunsch E, aber Konflikte prüfen“ | **Standard: Benutzen-Taste (Rechtsklick)**. Optional einschaltbar: „E spricht mit NPC im Blick“ (sonst bleibt E Inventar) | E ist in Vanilla die Inventartaste; der Ursprungsauftrag verbietet das Überschreiben wichtiger Tasten. **Entscheidung des Inhabers erbeten.** |
| A2 | Linke Liste im Talentbaum | Bild: Klassen „Krieger, Runenmagier, Waldläufer, Handwerker“ | Liste aller Pfade: Allgemein, Runenpfad, Klassenbäume | „Runenmagier“ und „Handwerker“ sind keine SRD-Klassen. Runen und Handwerk sind als Pfad bzw. Zweig umgesetzt. **Offen:** Sollen es eigene FROSTHAIN-Klassen werden? |
| A3 | Logo oben links im HUD | Bild: dauerhaft sichtbar | Kapitelbanner, blendet aus | Spezifikation B: wenige permanente Elemente |
| A4 | Ziffern vor Antworten | Bild: keine; Mockup-HTML: „1 ·“ | dezente Ziffern, abschaltbar | Tastaturbedienung erkennbar machen (Spezifikation F) |
| A5 | Minimap | Bild: vorhanden | eigene Minimap erst in M4.4; Prototyp zeigt Platzhalter | Aufwand; optionale Anbindung an eine Minimap-Mod wird geprüft |

## 10.3 Design-Tokens (aus dem Referenzbild abgeleitet)

| Token | Wert | Verwendung |
|---|---|---|
| `panel.background` | `#0E1013` bei 92 % Deckkraft (HUD: 78 %) | Fenster, Tafeln |
| `panel.inner` | `#16191E` | innere Felder |
| `frame.gold` | `#B8955A` | Rahmenlinien |
| `frame.goldBright` | `#E6C27A` | aktive Elemente, Überschriften |
| `text.primary` | `#EDE3CF` | Fließtext |
| `text.secondary` | `#B9AE98` | Nebentext |
| `text.disabled` | `#6F6A60` | gesperrt |
| `state.available` | `#6FD15A` | „Freischaltbar“ |
| `state.blocked` | `#D9594C` | Sperrgründe |
| `branch.combat` | `#E0452E` | Kampf |
| `branch.magic` | `#3BA4F0` | Magie |
| `branch.survival` | `#6DC54A` | Überleben |
| `branch.crafting` | `#B25FE0` | Handwerk |
| `branch.rune` | `#7FD3E6` | Runenpfad |
| Schrift Überschrift | Cinzel (SIL OFL 1.1) | Titel, Namen, Reiter |
| Schrift Text | EB Garamond (SIL OFL 1.1) | Beschreibungen, Dialoge |

Animationen 150–250 ms, kein Blinken; alle Größen skalieren mit der GUI-Skalierung von Minecraft und einem eigenen Textgrößen-Regler.

## 10.4 Architektur der Oberflächen

```
core (Regeln, Daten)          ui (Präsentationslogik, ohne Minecraft)        Darstellung
──────────────────────        ───────────────────────────────────────       ─────────────────────────────
TalentAllocator      ───────▶ TalentTreeView: Knotenzustände, Kanten,  ───▶ Mod: TalentTreeScreen (M3.3)
LevelingRules                  Layout, Kamera, Fokus-Navigation,            Prototyp: Java2D-Fenster
                               Detailtafel, Reiter                          
QuestLog             ───────▶ QuestTrackerView: Abschnitte, Zeilen,   ───▶ Mod: HUD-Element (M4.4)
                               Umbruch, Breitenlimit, Einklappen            Prototyp: Java2D
DialogueSession      ───────▶ DialogueView: Text, Antworten, Auswahl, ───▶ Mod: DialogueScreen (M4.3)
                               Unterbrechung                                Prototyp: Java2D
                               HudPolicy: welche Vanilla-Elemente      ───▶ Mod: Fabric HUD-API
                               wann sichtbar sind
                               KeyBindingPlan: Belegung + Konfliktprüfung
                               Theme, Translator, TextLayout (Umbruch)
```

Regel: Jede Entscheidung, *was* angezeigt wird und *was* eine Eingabe bewirkt, liegt im Modul `ui` und ist ohne Minecraft getestet. Die Minecraft-Bildschirme und der Prototyp zeichnen nur. Dadurch verhalten sich Prototyp und Spiel gleich, und der nächste Minecraft-Drop betrifft nur die Zeichenschicht.

## 10.5 Komponenten und Verhalten

### Talentbaum (D)
- **Zustände je Knoten:** gesperrt (grau, Schloss, Gründe im Tooltip) · verfügbar (Zweigfarbe gedämpft, grüner Status) · freigeschaltet (Zweigfarbe leuchtend) · Höchstrang (Goldkranz). Kanten leuchten, wenn beide Enden freigeschaltet sind.
- **Detailtafel:** Name, Art (Passives/Aktives Talent, Schlüsseltalent), Beschreibung, Rang „1/3“, Voraussetzungen mit ✓/✗, Kosten, Status, Knöpfe „Talent freischalten“ und „Zurücknehmen“.
- **Respec-Regeln sichtbar:** Einzelrücknahme kostenlos, solange nichts darauf aufbaut; vollständiges Zurücksetzen über einen Knopf mit Bestätigung (im Spiel später mit Ritualkosten, ADR-pflichtig).
- **Bedienung:** Maus (Klick wählen, Ziehen verschieben, Mausrad zoomen), Tastatur (Pfeiltasten wechseln zum nächsten Knoten in Pfeilrichtung, Enter freischalten, Entf/Rücktaste zurücknehmen, 1–4 Reiter, +/− Zoom, 0 Ansicht zurücksetzen, Esc schließen), Controller über dieselben Aktionen (M3.3).
- **Pfadliste:** allgemeiner Baum „Pfad der Legende“ (4 Zweige wie im Bild), „Runenpfad“, Klassenbäume; Bäume ohne Zugang als gesperrte Vorschau.

### Questtracker (B)
- Abschnitte Haupt-/Nebenquest, je Quest der Name und das **aktuelle** Ziel (bei aufeinanderfolgenden Zielen nur das nächste offene), Fortschritt „(n/m)“.
- Höchstens **N angeheftete Quests** (Standard 3, einstellbar 1–5); aktive Quests sind unbegrenzt. Hauptquests werden beim Annehmen bevorzugt angeheftet.
- Breite höchstens Minimap-Breite; Text wird umbrochen, nie abgeschnitten; überzählige Quests werden durch „⌄ +n weitere“ angedeutet.
- Einklappbar (Taste und Klick im Journal); im eingeklappten Zustand nur die Kopfzeile.
- Benachrichtigungen (Quest begonnen/abgeschlossen) kurz, abschaltbar, skalierbar.
- Ausgeblendet mit F1, bei offenem Debug-Bildschirm und während Dialogen.

### NPC-Dialog (F)
- **Interaktionshinweis** bei Blick auf einen NPC in Reichweite: „[Rechtsklick] Mit Eldrin sprechen“ – die angezeigte Taste kommt aus der tatsächlichen Belegung.
- **HUD:** Hotbar, Herzen, Hunger, Rüstung, Luft, Erfahrungsleiste und Questtracker aus; nach dem Dialog wieder an.
- **Echtzeit-Sicherheit:** Das Spiel pausiert nicht. Erleidet der Spieler Schaden, wird der Dialog unterbrochen („Du wirst angegriffen!“) und kann später an derselben Stelle fortgesetzt werden. Startet ein Gespräch, während Feinde den Spieler verfolgen, erscheint eine Warnung.
- **Text:** lokalisiert, Umbruch nach Breite, Textgröße einstellbar, keine Zeitlimits für Antworten.

## 10.6 Tastenbelegung (Standard, alles änderbar)

| Aktion | Standard | Konfliktprüfung gegen Vanilla-Standardbelegung |
|---|---|---|
| Mit NPC sprechen | Benutzen (Rechtsklick) | keine neue Taste |
| Talentbaum | `K` | frei |
| Questjournal | `J` | frei |
| Questtracker ein-/ausklappen | `H` | frei |
| Charakterübersicht | `I` | frei |
| Option „E spricht mit NPC“ | `E` kontextabhängig | Konflikt mit **Inventar** → nur auf Wunsch |

Die Prüfung ist als Test hinterlegt (Vanilla-Belegung bis 1.21). Im Spiel prüft die Mod beim Start zusätzlich gegen die tatsächlich registrierten Tasten aller Mods und meldet Konflikte (M0.3/M3.3).

## 10.7 Daten, die die Oberflächen brauchen (neu im Kern)

| Typ | Inhalt | Status |
|---|---|---|
| `talent_trees` | zusätzlich `category` (`general`, `class`, `rune`), Knoten-`icon` | umgesetzt |
| `quests` | Art (Haupt-, Neben-, Entdeckungs-, Klassen-, Charakter-, NPC-Quest), Ziele mit Typ/Ziel/Anzahl, aufeinanderfolgend ja/nein, Erfahrung | umgesetzt (Daten + Laufzeit `QuestLog`) |
| `npcs` | Name, Titel, Start-Dialog | umgesetzt (minimal) |
| `dialogues` | Knoten, Antworten, Bedingungen (Queststatus), Folgen (Quest starten, Ziel erfüllen, Erfahrung) | umgesetzt (Daten + Laufzeit `DialogueSession`) |
| Beispielinhalte | Eldrin, Dialog „Das Erwachen“ (Text aus dem Referenzbild), Quests „Das Erwachen“ und „Verlorene Vorräte“, allgemeiner Baum mit vier Zweigen inkl. „Kampfhaltung“, Runenpfad | umgesetzt |

## 10.8 Umsetzungsschritte

| # | Schritt | Ergebnis | Prüfung |
|---|---|---|---|
| U1 | Kern-Erweiterungen (Quests, NPCs, Dialoge, Baumkategorie) + Beispielinhalte | Daten und Laufzeitlogik | JUnit, Inhaltsvalidator |
| U2 | Modul `ui`: Theme, Translator, Textumbruch, `TalentTreeView`, `QuestTrackerView`, `DialogueView`, `HudPolicy`, `KeyBindingPlan` | testbare Präsentationslogik | JUnit |
| U3 | **Prototyp** `tools/ui-prototype`: interaktives Desktop-Fenster (Java2D) für B, D, F mit echten Regeln; Bildschirmfotos ohne Bildschirm erzeugbar | sofort ausprobierbar ohne Minecraft | Screenshot-Durchsicht, Smoke-Test |
| U4 | Mod-Skelett (M0.3) | lädt Inhalte im Spiel | CI auf GitHub |
| U5 | Minecraft-Zeichenschicht: TalentTreeScreen, Questtracker-HUD-Element, DialogueScreen, HUD-Ausblendung über die Fabric-HUD-API, TTF-Schriften als Font-Provider | Oberflächen im Spiel | Client-GameTests mit Screenshots |
| U6 | Spieltest, Feinschliff, Barrierefreiheit (Textgröße, Kontrast, Controller) | Abnahme durch den Inhaber | Checkliste |

## 10.9 Tests

- **Logik (U1/U2):** Knotenzustände und Sperrgründe, Fokus-Navigation, Kamera-Grenzen, Reiter-Fokus, Anheft-Limit, Umbruch innerhalb der Breite, Abschnittsreihenfolge, Dialogfluss mit Bedingungen und Folgen, Unterbrechung und Fortsetzen, HUD-Sichtbarkeit je Modus, Tastenkonflikte.
- **Darstellung (U3/U5):** Bildschirmfotos in festen Auflösungen (1920×1080, 1280×720) werden bei jeder Änderung neu erzeugt und begutachtet; im Spiel über Fabric Client-GameTests.
- **Manuell:** Bedienung nur mit Tastatur; GUI-Skalierung 2–4; Textgröße groß.
