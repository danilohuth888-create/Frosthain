# 6 · Datenmodell: Klassen, Spezies, Talente

Alle hier beschriebenen Typen sind **im Kern umgesetzt und getestet**, sofern nicht als „geplant“ markiert. Maßgeblich ist der Code in `core/src/main/java/net/frosthain/core/content/`; Beispiele stammen aus `content/frosthain_base/`.

## 6.1 Ablage, IDs, Übersetzungen

| Was | Konvention | Beispiel |
|---|---|---|
| Datei | `data/<namespace>/frosthain/<typ>/<pfad>.json` | `data/frosthain/frosthain/classes/fighter.json` |
| ID | `<namespace>:<pfad>` | `frosthain:fighter`, `frosthain:fighter/champion` |
| Erlaubte Zeichen | Namespace `[a-z0-9_.-]`, Pfad zusätzlich `/` (identisch zu Minecraft) | |
| Übersetzungsschlüssel | `<typ>.<namespace>.<pfad mit Punkten>` (+ `.desc`, `.role`, …) | `class.frosthain.fighter.desc` |
| Kommentare | Felder mit `_` am Anfang werden ignoriert | `"_comment": "…"` |
| Unbekannte Felder | **Fehler** (fängt Tippfehler ab) | `"adaptd": true` → Fehler mit JSON-Pfad |
| Überschreiben | spätere Pakete ersetzen gleiche IDs (Hinweis im Log) | Community-Paket ersetzt `frosthain:rogue` |

## 6.2 Gemeinsame Felder

| Feld | Pflicht | Bedeutung |
|---|---|---|
| `source` | ja | ID einer Quelle im Lizenzkatalog (`sources/`). Nur Status `approved` ist zulässig. |
| `adapted` | nein (false) | `true`, wenn gegenüber der Quelle verändert (CC-BY verlangt Kennzeichnung). |

### Quelle (`sources/`)
```json
{
  "title": "System Reference Document 5.2.1",
  "publisher": "Wizards of the Coast LLC",
  "license": "CC-BY-4.0",
  "license_url": "https://creativecommons.org/licenses/by/4.0/legalcode",
  "url": "https://www.dndbeyond.com/srd",
  "status": "approved",
  "attribution": "This work includes material from the System Reference Document 5.2.1 …"
}
```
`status`: `approved` · `pending_review` (Inhalt gesperrt) · `rejected` (Inhalt verboten). CC-BY-Quellen ohne `attribution` sind ein Fehler.

## 6.3 Spezies (`species/`)

| Feld | Typ | Bedeutung |
|---|---|---|
| `size` | `small`/`medium`/`large` | Größenkategorie (Regeln, z. B. schwere Waffen) |
| `traits` | Modifikatoren | dauerhafte Eigenschaften |
| `lineages` | Liste `{key, traits}` | Abstammungslinien; ID `<spezies>/<key>` |
| `lineage_required` | bool (Standard: true, wenn Linien existieren) | Auswahl erzwingen |
| `appearance.height_scale` | `[min, max]` | optische Feinanpassung relativ zur Spezies |
| `appearance.features` | Slot → erlaubte Teil-IDs | Slots: `ears`, `horns`, `tail`, `scales`, `tusks`, `markings` |
| `appearance.required_slots` | Liste | Pflichtmerkmale (Tiefling: Hörner, Schweif) |

```json
{
  "source": "frosthain:srd_5_2_1", "adapted": true, "size": "medium",
  "traits": [{ "type": "flag", "flag": "frosthain:darkvision" }],
  "lineages": [
    { "key": "infernal", "traits": [{ "type": "resistance", "damage_type": "#minecraft:is_fire", "amount": 0.5 }] }
  ],
  "appearance": {
    "height_scale": [0.95, 1.06],
    "features": { "horns": ["frosthain:horns/ram", "frosthain:horns/crown"], "tail": ["frosthain:tail/fiend"] },
    "required_slots": ["horns", "tail"]
  }
}
```

## 6.4 Klassen (`classes/`) und Unterklassen (`subclasses/`)

| Feld | Bedeutung |
|---|---|
| `hit_die` | 6/8/10/12 – steuert Lebenspunkte-Zuwachs pro Klassenstufe |
| `primary_abilities` | Hauptattribute (Anzeige, Empfehlungen) |
| `saving_throws` | Rettungswurf-Übung (Dialog-/Fallenproben) |
| `multiclass_requirements` | Liste von Alternativen; jede Alternative ist eine Menge Attribut → Mindestwert. Beispiel Krieger: `[{"strength":13},{"dexterity":13}]`, Paladin: `[{"strength":13,"charisma":13}]` |
| `caster_progression` | `none`/`third`/`half`/`full` – Beitrag zur gemeinsamen Zauberstufe |
| `resource` | Ressource (Zauberkraft, Ausdauer, Kampfrausch, Fokus, Paktmagie) |
| `subclass_level` | Klassenstufe der Unterklassenwahl (Standard 3) |
| `starting_powers` | Fähigkeiten ab Klassenstufe 1 |
| `skill_choices` | `{count, from}` – wählbare Fertigkeiten |
| `armor_training`, `weapon_proficiencies` | Kategorien (`light`, `medium`, `heavy`, `shields` / `simple`, `martial`) |
| `talent_tree` | zugehöriger Klassenbaum (muss als Besitzer diese Klasse haben) |
| `exclusive_with` | Klassen, die nicht kombiniert werden dürfen (Balancing-Hebel, standardmäßig leer) |

Unterklasse: `class` (Elternklasse), optional `talent_tree` (Besitzer muss diese Unterklasse sein).

## 6.5 Hintergründe (`backgrounds/`)

`ability_options` (genau 3 verschiedene Attribute für den Bonus), `skill_proficiencies` (genau 2), `starting_items` (Minecraft-Item-IDs, Prüfung gegen die Item-Registry in der Mod).

## 6.6 Fertigkeiten, Ressourcen, Fähigkeiten

- **Fertigkeit** (`skills/`): `ability`. 18 Fertigkeiten des SRD.
- **Ressource** (`resources/`): `color`, `base_maximum`, `regeneration_per_second`, `regenerates_in_combat`.
- **Fähigkeit** (`powers/`): `kind` (`active`/`passive`/`toggle`), `resource`, `cost`, `cooldown_ticks` (20 Ticks = 1 s), `school`, `tags`. Passive Fähigkeiten haben keine Kosten. *Die ausführbaren Effekte (Schaden, Projektile, Runen) folgen in Phase 4; bis dahin haben Fähigkeiten im Spiel keine Wirkung.*

## 6.7 Modifikatoren (für Spezies, Abstammungen, Talente)

| `type` | Felder | Wirkung in Minecraft |
|---|---|---|
| `attribute` | `attribute`, `operation` (`add_value`, `add_multiplied_base`, `add_multiplied_total`), `amount` | benannter Attribut-Modifikator (z. B. `minecraft:max_health`, `minecraft:scale`, `minecraft:step_height`, `frosthain:spell_power_max`) |
| `ability_score` | `ability`, `amount` (−4…4) | Attributsbonus |
| `grant_power` | `power` | schaltet Fähigkeit frei |
| `skill_proficiency` | `skill` | Übung in Fertigkeit |
| `resistance` | `damage_type` (ID oder `#tag`), `amount` (0–1] | Schadensreduktion |
| `flag` | `flag` | schaltet eine Engine-Funktion ein |

Bei Talenten mit mehreren Rängen werden Zahlenwerte mit dem Rang multipliziert.

**Engine-Funktionen** (`EngineFlags`): `darkvision`, `water_breathing`, `powerful_build`, `nimble_escape`, `breath_weapon`, `fey_ancestry`, `relentless_endurance`, `lucky`. Jede Funktion trägt den ehrlichen Status `PLANNED` oder `IMPLEMENTED`; der Validator listet, welche Inhalte noch nicht umgesetzte Funktionen nutzen. Aktuell sind alle `PLANNED`.

## 6.8 Talentbäume (`talent_trees/`)

```json
{
  "source": "frosthain:original",
  "owner": { "type": "class", "class": "frosthain:fighter" },
  "root": "weapon_master",
  "branches": ["mastery", "defense", "techniques"],
  "nodes": [
    { "key": "weapon_master", "kind": "root", "position": [0, 0] },
    { "key": "weapon_focus", "kind": "passive", "position": [-1, 0], "branch": "mastery",
      "requires": ["weapon_master"], "max_rank": 3,
      "effects": [{ "type": "attribute", "attribute": "minecraft:attack_damage", "operation": "add_value", "amount": 0.5 }] },
    { "key": "unbreakable", "kind": "keystone", "position": [0, 3], "branch": "defense",
      "requires": ["shield_bash"], "required_class_level": 7, "exclusive_group": "fighter_keystone",
      "effects": [{ "type": "attribute", "attribute": "minecraft:armor_toughness", "operation": "add_value", "amount": 2 }] }
  ]
}
```

| Feld | Bedeutung |
|---|---|
| `owner` | `general` (alle), `class`, `subclass` |
| `kind` | `root` (kostenlos, automatisch freigeschaltet), `passive`, `active` (muss `grant_power` enthalten), `keystone` (baumprägend) |
| `position` | Rasterkoordinaten um die Wurzel (−50…50), eindeutig je Baum |
| `requires` / `requires_any` | alle / mindestens eine dieser Voraussetzungen |
| `required_level` / `required_class_level` | Charakterstufe / Stufe der besitzenden Klasse |
| `exclusive_group` | höchstens ein Knoten je Gruppe |
| `max_rank`, `cost` | Ränge (1–5) und Punkte pro Rang |

**Validator-Regeln:** Wurzel existiert und ist einzig; Voraussetzungen existieren; keine Zyklen; jeder Knoten ist von der Wurzel erreichbar (auch unter Berücksichtigung von Exklusivgruppen); keine doppelten Positionen; Zweige deklariert; Klassenstufen nur in Klassenbäumen.

**Laufzeitregeln** (`TalentAllocator`): Punkte = Stufe × `talent_points_per_level` + Bonuspunkte (Stufen 4/8/12/16/19). Punkte sind in allen zugänglichen Bäumen frei verteilbar. Jede Sperre wird mit Grund gemeldet (`Blocker`). Rücknahme nur, wenn nichts davon abhängt; Komplett-Reset jederzeit (Kosten legt die Mod fest); `sanitize` repariert Spielstände nach Inhaltsänderungen.

## 6.9 Regeln (`rules/`)

- `progression.json`: `max_level` (20), `experience_thresholds` (EP(Stufe) = 150·(Stufe−1)·Stufe, d. h. 300 EP für Stufe 2, 57 000 EP für Stufe 20), `talent_points_per_level`, `bonus_talent_points`, `multiclass` (`enabled`, `max_classes` = 3, `minimum_level_before_multiclass` = 3).
- `ability_scores.json`: Standardreihe 15/14/13/12/10/8, Punktekauf 27 Punkte (8–15), Höchstwert 20.

**Eigene Erfahrung (EP):** vollständig getrennt von Vanilla-XP (Verzauberungsstufen). Quellen (Phase 4/5): Entdeckungen, Quests, besiegte Gegner (gedeckelt pro Gegnertyp und Zeit, damit Mobfarmen nicht dominieren), Handwerksmeilensteine. Erfahrung macht einen Stufenaufstieg nur *verfügbar*; der Spieler wählt im Charaktermenü, welche Klasse die Stufe erhält.

## 6.10 Multiclassing

1. Voraussetzungen der neuen **und** aller bisherigen Klassen (SRD-Regel, Standard 13).
2. Höchstens 3 Klassen, frühestens ab Charakterstufe 3.
3. Optionale Ausschlusslisten (`exclusive_with`).
4. **Gemeinsame Zauberstufe** statt gestapelter Ressourcen: Voll-Zauberwirker zählen ganz, Halb-Zauberwirker halb, Drittel-Zauberwirker zu einem Drittel (abgerundet je Klasse). Die Zauberkraft skaliert mit diesem Wert – Kombinationen mehrerer Zauberklassen werden vielseitiger, aber nicht doppelt so stark.
5. Klassen-Talentbaumknoten hängen an der Stufe *der jeweiligen Klasse* – tiefe Schlüsseltalente bleiben reinen Klassen vorbehalten.

## 6.11 Charakterbogen (Speicherformat v1)

```json
{
  "schema_version": 1,
  "character_id": "…uuid…",
  "name": "Arwen Eisherz",
  "species": "frosthain:tiefling", "lineage": "infernal",
  "background": "frosthain:soldier",
  "ability_scores": { "strength": 17, "dexterity": 13, "constitution": 15, "intelligence": 8, "wisdom": 12, "charisma": 10 },
  "classes": [ { "class": "frosthain:fighter", "level": 3, "subclass": "frosthain:fighter/rune_warden" } ],
  "experience": 1800,
  "talents": { "frosthain:fighter#weapon_focus": 1, "frosthain:general#toughness": 1 },
  "appearance": { "body_type": "slim", "height_scale": 1.03, "skin_color": "#a0405a", "…": "…" },
  "biography": { "text": "…", "personality_traits": ["neugierig", "loyal"] }
}
```

## 6.12 Attribute → Minecraft (Balancing-Startwerte, Phase 3)

| Attribut | Wirkung (Modifikator = ⌊(Wert−10)/2⌋) |
|---|---|
| Stärke | Nahkampfschaden, Rückstoß, Traglast schwerer Waffen |
| Geschicklichkeit | Angriffsgeschwindigkeit, Fernkampfgenauigkeit, Ausweichrolle |
| Konstitution | maximale Lebenspunkte (zusammen mit dem Trefferwürfel), Ausdauer |
| Intelligenz | arkane Zauberkraft, Runenmagie |
| Weisheit | göttliche/natürliche Zauberkraft, Wahrnehmung (Hinweise auf Geheimnisse) |
| Charisma | Handelspreise, Dialogproben, Barden-Inspiration |

Alle Effekte bewusst moderat, damit Vanilla-Balance und Rüstungen relevant bleiben.

## 6.13 Geplante Inhaltstypen

| Typ | Phase | Zweck |
|---|---|---|
| `appearance_parts` | 2 | Frisuren, Bärte, Hörner, Ohren, Muster, Markierungen mit Texturen |
| `power` Effekte | 4 | ausführbare Effektketten (Schaden, Heilung, Projektil, Fläche, Status) |
| `runes` | 4 | Runen, Runenkombinationen |
| `status_effects` | 4 | eigene Zustände (Brennend, Unterkühlt, Inspiriert, …) |
| `quests` | 4 | Ziele, Auslöser, Belohnungen, Folgequests |
| `dialogues` | 4 | Dialoggraphen mit Bedingungen und Konsequenzen |
| `npcs` | 4 | Name, Aussehen (gleiches `Appearance`-Modell), Dialoge, Zeitplan |
| `factions` | 5 | Fraktionen und Ruf |

## 6.14 Neue Klasse hinzufügen (Beispiel für Erweiterbarkeit)

1. Quelle prüfen und im Lizenzkatalog eintragen (`sources/…`, Status `approved`).
2. `classes/<id>.json` anlegen, mindestens eine `subclasses/<id>/<sub>.json`.
3. Optional `talent_trees/<id>.json` mit `owner.class`.
4. Übersetzungen in `de_de.json` (Pflicht) und `en_us.json`.
5. `./gradlew check` – der Validator meldet alles Fehlende.

Kein Java-Code nötig, solange vorhandene Modifikator- und Fähigkeitstypen genügen.
