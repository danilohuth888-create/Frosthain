# Inhaltskatalog mit Herkunfts- und Lizenznachweis

Regel: Nur Inhalte mit einer Quelle im Status `approved` werden ausgeliefert. Der Validator erzwingt das technisch. Diese Seite ist die menschenlesbare Übersicht; maßgeblich sind die Dateien in `content/frosthain_base/data/frosthain/frosthain/sources/`.

## 1. Quellen

| ID | Quelle | Lizenz | Status | Bedingungen |
|---|---|---|---|---|
| `frosthain:srd_5_2_1` | System Reference Document 5.2.1, Wizards of the Coast LLC | CC-BY-4.0 | ✅ approved | Namensnennung (NOTICE.md), Änderungen kennzeichnen (`adapted`), keine Markennutzung als Produktname, keine Andeutung einer Billigung |
| `frosthain:original` | FROSTHAIN-Originalinhalte | durch Inhaber festzulegen | ✅ approved | eigene Texte, Mechaniken, Grafiken |

## 2. Im Basispaket umgesetzt

### Klassen (alle SRD 5.2.1, angepasst)
| ID | Deutsch | Unterklasse (SRD) | Talentbaum |
|---|---|---|---|
| `barbarian` | Barbar | Pfad des Berserkers | ⏳ |
| `bard` | Barde | Kolleg des Wissens | ⏳ |
| `cleric` | Kleriker | Domäne des Lebens | ⏳ |
| `druid` | Druide | Zirkel des Landes | ⏳ |
| `fighter` | Krieger | Champion · *Runenhüter (FROSTHAIN)* | ✅ Weg des Kriegers |
| `monk` | Mönch | Krieger der offenen Hand | ⏳ |
| `paladin` | Paladin | Eid der Hingabe | ⏳ |
| `ranger` | Waldläufer | Jäger | ⏳ |
| `rogue` | Schurke | Dieb | ⏳ |
| `sorcerer` | Zauberer | Drakonische Zauberei | ⏳ |
| `warlock` | Hexenmeister | Pakt mit dem Unhold | ⏳ |
| `wizard` | Magier | Hervorrufer · *Frostwirker (FROSTHAIN)* | ⏳ |

### Spezies (alle SRD 5.2.1, angepasst)
Mensch · Elf (Drow, Hochelf, Waldelf) · Zwerg · Halbling · Gnom (Waldgnom, Felsgnom) · Drachenblütiger (10 Drachenarten) · Goliath (6 Riesenabstammungen) · Ork · Tiefling (abyssisch, chthonisch, infernalisch)

### Hintergründe
SRD 5.2.1: Akolyth, Krimineller, Gelehrter, Soldat · FROSTHAIN: Hafenkind, Hochlandvolk

### Weitere
18 Fertigkeiten (SRD) · Attributsregeln: Standardreihe, Punktekauf, Hintergrundbonus (SRD) · Ressourcen, Fähigkeiten außer „Zweiter Atem“ und „Tatendrang“ (SRD-Klassenmerkmale), Talentbäume, Erfahrungskurve (FROSTHAIN)

## 3. Nicht nutzbar (geschützt, nicht im SRD)

Diese Liste dient dazu, versehentliche Übernahmen zu erkennen. Bei Bedarf entstehen **eigenständige** FROSTHAIN-Alternativen mit eigenen Namen, Texten und Mechaniken.

| Kategorie | Beispiele (nicht verwenden) | FROSTHAIN-Alternative |
|---|---|---|
| Unterklassen außerhalb des SRD | Kampfmeister (Battle Master), Eldritch Knight, Arkaner Betrüger, Seelenmesser, Sturmdomäne u. v. m. | eigene Spezialisierungen wie *Runenhüter*, *Frostwirker* |
| Klassen außerhalb des SRD | Artificer (Magieschmied), Blood Hunter (Drittanbieter) | ggf. eigene Klasse „Runenschmied“ (Phase 4/5 prüfen) |
| Spezies außerhalb des SRD | Aasimar, Tabaxi, Githyanki, Genasi, Kenku u. a. | eigene Völker Frosthains (Phase 5) |
| Spezifische Settings und Figuren | Forgotten Realms, Baldur's Gate, benannte Gottheiten und NPCs | eigene Welt, Götter, Figuren |
| Monster als „Product Identity“ | Beholder, Mind Flayer u. a. | eigene Kreaturen |
| Inhalte aus Baldur's Gate 3 | Texte, UI-Grafiken, Figuren, Dialoge | nur Bedienqualität als Maßstab |

## 4. Community-Erweiterungen

Werden nur aufgenommen, wenn **ausdrücklich** eine Lizenz vorliegt, die Bearbeitung und Weitergabe erlaubt (z. B. CC-BY-4.0 oder eine schriftliche Genehmigung). Ablauf:

1. Quelle mit Status `pending_review` anlegen – zugehörige Inhalte sind ab dann technisch gesperrt.
2. Lizenztext und Herkunft dokumentieren (Link, Datum, Version, Rechteinhaber).
3. Prüfung durch den Projektinhaber; bei Zustimmung Status `approved` und Namensnennung ergänzen.

Bisher wurde **keine** Community-Erweiterung geprüft; es sind daher auch keine aufgenommen.

## 5. Offene Prüfpunkte

- Abgleich der übernommenen Regelelemente mit dem Original-PDF des SRD 5.2.1 (M3.1).
- Festlegung der FROSTHAIN-eigenen Lizenz (ADR 0005).
- Schriftarten, Sounds und Texturen: nur eigene oder frei lizenzierte Werke (z. B. SIL OFL, CC0, CC-BY), jeweils mit Eintrag im Lizenzkatalog.
