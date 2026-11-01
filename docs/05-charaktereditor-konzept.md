# 5 · Konzept: Charaktereditor

> Aktualisiert am 01.11.2026: Die Referenzbilder für B, D und F liegen jetzt vor (`docs/referenzen/referenzbilder_B_D_F.webp`) zusammen mit den abgestimmten Spezifikationen. Die konkrete Umsetzung von HUD, Talentbaum und Dialogen steht in [10 · UI-Implementierungsplan](10-ui-implementierungsplan.md); §5.9 unten ist dadurch präzisiert.

## 5.1 Wann der Editor erscheint

- Betritt ein Spieler eine Welt ohne FROSTHAIN-Charakter, öffnet der Server den Editor, bevor der Spieler handeln kann. Der Spieler steht derweil geschützt in einem ruhigen Prolog-Bereich (keine Gegner, kein Hunger, keine Zeitlimits).
- Abbrechen ist möglich (Welt verlassen); das Spiel startet erst nach Bestätigung.
- Der Server prüft die Auswahl mit `CharacterFactory` – ein manipulierter Client kann keine ungültigen Charaktere erzeugen.

## 5.2 Ablauf (Schritte)

| # | Schritt | Inhalt | Regeln im Kern |
|---|---|---|---|
| 1 | **Spezies** | 9 Spezies, Karten mit Kurzbeschreibung und Eigenschaften | `SpeciesDefinition` |
| 2 | **Abstammung** | nur wenn vorhanden (Elf, Gnom, Drachenblütiger, Goliath, Tiefling) | `lineage_required` |
| 3 | **Klasse** | 12 Klassen mit Rolle, Hauptattributen, Spielstil-Vorschau, empfohlenen Attributen | `ClassDefinition` |
| 4 | **Hintergrund** | Herkunftsgeschichte, Fertigkeiten, Attributsbonus | `BackgroundDefinition` |
| 5 | **Attribute** | Standardreihe (Ziehen & Ablegen) oder Punktekauf; Hintergrundbonus +2/+1 oder +1/+1/+1; Empfehlung per Knopf | `AbilityScoreRules` |
| 6 | **Aussehen** | siehe 5.4 | `Appearance.validate` |
| 7 | **Persönlichkeit & Biografie** | optionale Eigenschaften-Tags, freier Text (max. 4000 Zeichen) | `Biography` |
| 8 | **Zusammenfassung** | Charakterbogen, Name, letzte Prüfung, „In die Welt“ | `CharacterFactory.create` |

Die Unterklasse wird **nicht** bei der Erstellung gewählt, sondern bei Klassenstufe 3 (wie im SRD) – so lernen Spieler ihre Klasse erst kennen.

Für Spieler ohne D&D-Kenntnisse: jeder Schritt hat eine Ein-Satz-Erklärung, „Empfohlen für Einsteiger“-Markierungen und eine **Schnellstart-Option** („Vorgefertigten Helden wählen“), die alle Schritte mit sinnvollen Werten füllt und nur Name und Aussehen offenlässt.

## 5.3 Bildschirmaufbau

```
┌──────────────────────────────────────────────────────────────────────────┐
│  FROSTHAIN · Charaktererstellung                          Schritt 3 / 8  │
├────────────┬───────────────────────────────────────┬─────────────────────┤
│ 1 Spezies ✓│                                       │  KLASSE             │
│ 2 Abstamm.✓│                                       │  ◆ Krieger          │
│ 3 Klasse ● │        3D-Vorschau des Charakters     │    Barbar           │
│ 4 Hintergr.│        (ziehen = drehen, Mausrad =     │    Barde  …         │
│ 5 Attribute│         zoomen, Doppelklick = Reset)   │                     │
│ 6 Aussehen │                                       │  Waffenmeisterschaft│
│ 7 Persönl. │   [Rüstung zeigen] [Pose] [Licht]     │  und Verteidigung.  │
│ 8 Abschluss│                                       │  Hauptattribut: STÄ │
├────────────┴───────────────────────────────────────┴─────────────────────┤
│  ← Zurück                                    Erklärung ein/aus   Weiter →│
└──────────────────────────────────────────────────────────────────────────┘
```

- Links: Schrittliste, frei anspringbar; erledigte Schritte markiert, fehlerhafte Schritte mit Hinweis.
- Mitte: 3D-Vorschau (Spielermodell mit allen Anpassungen), drehbar, zoombar, Posen (Stand, Kampf, Zauber), Umschalter „Rüstung zeigen“.
- Rechts: Auswahl und Erklärung. Kein Bildschirm zeigt mehr als eine Entscheidung auf einmal.
- Bedienung vollständig mit Maus, Tastatur und Controller (Fokusnavigation, Schultertasten = Schritt wechseln, rechter Stick = Modell drehen).

## 5.4 Anpassbares Aussehen und technische Umsetzung

| Merkmal | Umsetzung | Machbarkeit |
|---|---|---|
| Körpertyp | klassische oder schmale Arme (Vanilla-Modellvarianten) | 🟢 |
| Größe | Spezies-Basisgröße über `minecraft:scale` (wirkt auf Hitbox) + ±5 % rein optisch | 🟢 |
| Freie Proportionen | nicht angeboten (bricht Rüstungen/Animationen) | 🔴 |
| Hautfarbe, Muster | Farbebene + Musterebene (Sommersprossen, Schuppen, Fell) im Skin-Compositing | 🟢 |
| Gesicht, Augen, Details | Varianten von Augen, Brauen, Mund, Nase als Pixel-Ebenen; Augenfarbe frei | 🟢 |
| Frisuren, Haarfarbe | Haar-Ebene auf Kopf + Hutschicht; lange Frisuren als Zusatz-Modellteil | 🟢 / 🟡 |
| Bärte | Ebene + optionales Zusatzteil für lange Bärte | 🟢 |
| Ohren, Hörner, Schweif, Hauer | Zusatz-Modellteile, an Kopf/Rumpf gebunden, mit animiert; Erlaubnis je Spezies | 🟢 |
| Narben, Tattoos, magische Markierungen | bis zu 8 Ebenen mit Region und Farbe; magische Markierungen optional leuchtend (Emissive-Ebene) | 🟢 / 🟡 |
| Kleidung, Accessoires | kosmetische Kleidungsebenen und kleine Zusatzteile (Umhang, Schärpe, Schmuck), Farben frei | 🟢 |
| Rüstungsdarstellung | Vanilla-Rüstung bleibt sichtbar; optional ausblendbar für Kosmetik (nur Darstellung) | 🟢 |

**Technischer Kern:** Ein `SkinCompositor` erzeugt aus `Appearance` eine 64×64-Textur (zwischengespeichert, neu nur bei Änderung). Zusatzteile werden als eigene Render-Ebene am Spielermodell gezeichnet. Teile und Muster sind Inhalte (`appearance_parts`, Phase 2) mit Texturen im Ressourcenpaket – neue Frisuren oder Hörner brauchen keinen Code.

Kein Austausch des Spielermodells: Kompatibilität mit Rüstungen, Animationen und anderen Mods bleibt erhalten ([ADR 0009](adr/0009-charakterdarstellung.md)).

## 5.5 Datenmodell (im Kern umgesetzt)

`Appearance` (Körpertyp, Größe, Farben, Augen, Haar, Bart, Merkmale je Slot, Markierungen) wird gegen die Spezies geprüft: erlaubte Teile je Slot, Pflicht-Slots (z. B. Tiefling: Hörner und Schweif), Größenbereich, Farbformat, Höchstzahl der Markierungen. Getestet: Standard-Aussehen ist für jede Spezies gültig; fehlende Pflichtmerkmale und unerlaubte Merkmale werden abgelehnt.

## 5.6 Änderungen im Spiel

| Änderung | Weg | Regeln |
|---|---|---|
| Kosmetik (Farben, Frisur, Bart, Markierungen, Kleidung) | Charaktermenü → „Aussehen“ jederzeit; zusätzlich Spiegel-Block / Barbier im Dorf als atmosphärische Variante | kostenlos, sofort, rückgängig machbar |
| Name, Biografie | Charaktermenü | jederzeit |
| Talente | Talentbaum: Einzelrücknahme (wenn nichts davon abhängt) kostenlos; Komplett-Reset mit fairen Kosten im Spiel (Ritual/Trank aus normalen Ressourcen) | kein Echtgeld, keine Wartezeiten |
| Unterklasse | einmalig wählbar bei Klassenstufe 3; Wechsel über Questritual | Bestätigungsdialog |
| Klasse | „Neuer Weg“-Ritual: verteilt Klassenstufen neu, Charakterstufe bleibt | Bestätigung, Hinweis auf Folgen |
| Spezies / Abstammung | seltenes Ritual (Story-Inhalt) | Bestätigung, Aussehen wird angepasst |

## 5.7 Persistenz

Charakterbogen im Spieleranhang der Welt, Format `CharacterSheetCodec` v1 (umgesetzt, Rundlauf getestet). Migrationen nur auf Kopien, Backup vor dem Speichern, neuere Formate werden nicht geladen.

## 5.8 Barrierefreiheit

Skalierbare Schrift, hohe Kontraste im Dark-Fantasy-Stil, keine reinen Farbsignale (Symbole zusätzlich), alle Aktionen per Tastatur/Controller, Animationen reduzierbar.

## 5.9 UI-Leitlinien für alle Oberflächen

Gilt für Mod und Launcher. Verbindliche Details: [10 · UI-Implementierungsplan](10-ui-implementierungsplan.md) und `docs/referenzen/`.

| Richtung | Leitlinie |
|---|---|
| **B – Dezentes Survival-HUD** | Im normalen Spiel nur das Nötige: Vanilla-Leisten plus schmale Ressourcenleiste und Fähigkeitsleiste, die bei Nichtnutzung ausblendet. Quest-HUD klein, einklappbar, höchstens 3 Ziele sichtbar; Haupt- und Nebenquests per Symbol unterscheidbar. |
| **D – Verzweigter Talentbaum** | Großer, schwenk- und zoombarer Baum um einen zentralen Wurzelknoten; Zweige farblich und beschriftet; gesperrte Knoten zeigen *warum* (Gründe aus `TalentAllocator.checkAllocate`); Vorschau künftiger Talente; Bestätigung vor dem Vergeben. |
| **F – Filmische NPC-Dialoge** | HUD ausgeblendet, Kamera auf den NPC, Letterbox-Balken, Sprechername und Porträt/3D-Ansicht, gut lesbarer Text, nummerierte Antworten mit Kennzeichnung von Proben („[Überzeugen]“). |
| ~~E – Königreichsverwaltung~~ | wird ausdrücklich nicht umgesetzt. |

Allgemein: dunkle Flächen mit leichter Textur, dezente Goldlinien, eine Serifenschrift für Überschriften (Cinzel) und eine gut lesbare Schrift für Fließtext (EB Garamond), beide SIL OFL 1.1, ruhige Übergänge (150–250 ms), keine blinkenden Elemente, keine Informationsflut.

**Tastenbelegung:** NPCs werden mit der normalen „Benutzen“-Taste (Rechtsklick) angesprochen – keine neue Taste nötig, Hinweis „Rechtsklick: Sprechen“ erscheint dezent in der Nähe. Für Menüs gibt es *eine* FROSTHAIN-Menütaste (Vorschlag: `B` – bis 1.21 in Vanilla allein unbelegt, `Strg+B` ist der Erzähler; gegen die Tastenliste von 26.3 in M0.3 prüfen) mit Radialmenü (Charakter, Talente, Zauberbuch, Journal, Chronik). Alle Belegungen sind über die Vanilla-Steuerung änderbar; beim Start prüft die Mod auf Konflikte und weist darauf hin.
