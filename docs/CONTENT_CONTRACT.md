# FROSTHAIN – Content-Vertrag (Sprint-Version 2)

Verbindliche Schnittstelle zwischen Inhalten (ChatGPT) und Technik (Claude Code). Maßgeblich ist der **Validator** (`core/.../validation/ContentValidator.java`, ADR 0006); dieses Dokument beschreibt ihn. Ausführliche Feldtabellen: [06-datenmodell.md](06-datenmodell.md). Schemaänderungen nur mit Eintrag unter „Migrationen“ am Ende.

## 1. Übergabe

- Branch: **`content/chatgpt-sprint`**, abgezweigt von `claude/ui-prototyp`.
- Alle Dateien in einem eigenen Paket: **`content/frosthain_story/`**. Keine anderen Dateien ändern (kein Code, nicht `content/frosthain_base/`, keine Doku außer `content/frosthain_story/README.md`).
- Jeder Push startet GitHub Actions; der Job „Kern und Inhalte“ führt den Validator aus. **Inhalte gelten erst als geliefert, wenn er 0 Fehler meldet.**
- `content/frosthain_story/README.md` listet: neue Orte/Objekte, die Technik bauen muss (Ziele vom Typ `discover`/`custom`, `frosthain:`-Ziele bei `collect`), neue Fähigkeiten mit gewünschter Wirkung, Balancing-Vorschläge.

## 2. Ablage, IDs, Grundregeln

```
content/frosthain_story/
  data/frosthain/frosthain/<typ>/<pfad>.json   → ID frosthain:<pfad>
  assets/frosthain/lang/de_de.json             → deutsche Texte (Pflicht)
  assets/frosthain/lang/en_us.json             → englische Texte (empfohlen)
```

- ID = `frosthain:<pfad>`, Pfad nur `a-z 0-9 _ - . /`, klein geschrieben. Unterordner erlaubt: `npcs/village/mira.json` → `frosthain:village/mira`.
- Dateien UTF-8, gültiges JSON, keine Kommentare (Felder mit `_` am Anfang werden ignoriert, z. B. `"_note"`).
- **Unbekannte Felder sind Fehler.** Keine eigenen Felder erfinden.
- Jede Datei braucht `"source"`. Eigene Inhalte: `"frosthain:original"`. SRD-Material nur mit `"frosthain:srd_5_2_1"` und `"adapted": true`, Texte selbst formuliert, keine Markennamen (ADR 0005).
- Gleiche ID wie in `frosthain_base` ersetzt den Basisinhalt (z. B. ein neuer Eldrin-Dialog). Nur bewusst einsetzen und im README nennen.
- **Keine Texte im JSON.** Alle sichtbaren Texte stehen in den Sprachdateien unter abgeleiteten Schlüsseln (Abschnitt 4).

## 3. Inhaltstypen

| Typ (Ordner) | Zweck | Pflichtfelder | Optionale Felder |
|---|---|---|---|
| `npcs` | benannte Figur | `source` | `dialogue` (Dialog-ID, Sprecher muss diese Figur sein) |
| `quests` | Quest | `source`, `kind`, `objectives` (≥1) | `sequential` (true), `experience` (0), `follow_ups` (Quest-IDs) |
| `dialogues` | Dialoggraph | `source`, `speaker`, `start`, `nodes` | je Antwort: `next`, `conditions`, `effects`, `once` |
| `powers` | Fähigkeit/Zauber | `source`, `kind` | `resource`, `cost`, `cooldown_ticks`, `school`, `tags`, `values` |
| `talent_trees` | Talentbaum | `source`, `owner`, `root`, `branches`, `nodes` | siehe 06-datenmodell §6.8 |
| `runes` | Rune am Runenaltar | `source`, `effect`, `materials` (≥1) | `unlock_quest`, `values` |

Andere Typen (`classes`, `species`, `backgrounds`, `skills`, `resources`, `rules`, `sources`) nur nach Absprache.

### Werte und Referenzen

| Feld | Erlaubte Werte |
|---|---|
| `quests.kind` | `main`, `side`, `discovery`, `class`, `character`, `npc` |
| `objectives[].type` | `talk` (Ziel = NPC-ID), `collect`, `reach`, `defeat`, `discover`, `custom` |
| `objectives[].key` | `a-z 0-9 _`, eindeutig je Quest |
| `objectives[].target` | `namespace:pfad` oder `#namespace:tag`; z. B. `minecraft:zombie`, `minecraft:sweet_berries`, `frosthain:supply_crate` |
| `objectives[].count` | 1–999 (Standard 1) |
| `experience` | 0–1 000 000 |
| `conditions[].type` | `quest_state` mit `quest`, `state` (`not_started`, `active`, `completed`) |
| `effects[].type` | `start_quest` (`quest`), `advance_objective` (`quest`, `objective`, `amount` 1–999), `experience` (`amount`) |
| `powers.kind` | `active`, `passive` (ohne `cost`/`cooldown_ticks`), `toggle` |
| `powers.resource` | `frosthain:spell_power`, `frosthain:stamina`, `frosthain:fury`, `frosthain:focus`, `frosthain:pact_magic` |
| `powers.cost` / `cooldown_ticks` | 0–1000 / 0–72 000 (20 Ticks = 1 s) |
| `powers.school` | `abjuration`, `conjuration`, `divination`, `enchantment`, `evocation`, `illusion`, `necromancy`, `transmutation`, `rune`, `martial`, `nature`, `divine` |
| `powers.values` | nur `range`, `damage_min`, `damage_max`, `knockback`, `heal_fraction`, `duration_ticks`, `amplifier`, `radius`, `dash`; je 0–10 000; `damage_min` ≤ `damage_max` |
| `runes.effect` | `fire_burst`, `frost_burst`, `ward`, `heal`, `swiftness` (Wirkung ist programmiert; neue Wirkungen nur über die Technik) |
| `runes.materials[]` | `item` (Minecraft-Item-ID, z. B. `minecraft:coal`), `count` 1–64 (Standard 1), jedes Item höchstens einmal |
| `runes.unlock_quest` | Quest-ID; die Rune ist bekannt, sobald die Quest abgeschlossen ist (ohne = von Anfang an) |
| `runes.values` | wie `powers.values` |

Klassen-IDs: `frosthain:barbarian`, `bard`, `cleric`, `druid`, `fighter`, `monk`, `paladin`, `ranger`, `rogue`, `rune_mage`, `sorcerer`, `warlock`, `wizard`.

### Was im Spiel bereits wirkt

- `talk`: über einen Dialog-Effekt `advance_objective` beim NPC.
- `defeat`, `collect`, `discover`, `reach`, `custom`: Auslöser baut die Technik. `collect` mit `minecraft:`-Item zählt aufgesammelte Gegenstände; `frosthain:`-Ziele sind Orte/Objekte, die im README beschrieben werden müssen.
- Fähigkeiten: Wirkung ist je Fähigkeit programmiert. **Neue Fähigkeiten haben erst nach Umsetzung eine Wirkung** – im README beschreiben. Texte und `values` für die vorhandenen 8 Fähigkeiten können direkt geliefert werden.
- Talent-Effekte vom Typ `attribute` wirken sofort; Flags (`flag`) sind meist noch nicht umgesetzt.
- Ein Klassenbaum für eine Klasse heißt `frosthain:<klasse>`; die Verknüpfung in der Klassendatei setzt die Technik.

## 4. Lokalisierung

`de_de.json` ist Pflicht (fehlende Schlüssel = **Fehler**), `en_us.json` empfohlen (fehlend = Warnung). Flaches JSON-Objekt `"schlüssel": "Text"`. Schlüssel werden aus der ID abgeleitet, `/` und `:` werden zu `.`:

| Inhalt | Pflichtschlüssel |
|---|---|
| NPC `frosthain:village/mira` | `npc.frosthain.village.mira`, `….title` |
| Quest `frosthain:q` | `quest.frosthain.q`, `….desc`, `….objective.<key>` je Ziel |
| Dialog `frosthain:d` | `dialogue.frosthain.d.<knoten>` (Sprechertext), `dialogue.frosthain.d.<knoten>.choice.<antwort>` |
| Fähigkeit `frosthain:p` | `power.frosthain.p`, `….desc` |
| Talentbaum `frosthain:t` | `talent_tree.frosthain.t`, `….branch.<zweig>`, `talent.frosthain.t.<knoten>`, `….desc` |
| Rune `frosthain:r` | `rune.frosthain.r`, `….desc` |

Textrichtwerte für die Oberfläche: Sprechertext ≤ 300 Zeichen, Antwort ≤ 60, Questname ≤ 32, Zielbeschreibung ≤ 70, Fähigkeitsbeschreibung ≤ 140. Keine Formatcodes (`§`). Platzhalter wie `%s` nur in UI-Texten nach Absprache.

## 5. Validierungsregeln (Auszug, vollständig im Validator)

Fehler (Inhalt wird abgelehnt): ungültiges JSON, unbekannte oder fehlende Pflichtfelder, ungültige Werte, unbekannte Quelle oder nicht freigegebene Lizenz, Verweis auf nicht existierende ID (NPC, Quest, Dialog, Ressource, Fähigkeit, Klasse), Dialog-Sprecher ≠ NPC, Antwort führt zu unbekanntem Knoten, Startknoten fehlt, Effekt nennt unbekanntes Questziel, doppelte Schlüssel, Quest ist eigene Folgequest, Talentbaum-Fehler (Zyklen, unerreichbare Knoten, doppelte Positionen), fehlende deutsche Übersetzung.
Warnungen: fehlende englische Übersetzung, nicht erreichbare Dialogknoten.

## 6. Prüfen

```
./gradlew :core:validateContent          # Linux/macOS
.\gradlew.bat :core:validateContent      # Windows (JDK 25)
```

Prüft alle Pakete unter `content/`. Erwartet: `Ergebnis: 0 Fehler, … Warnungen`, Exit-Code 0. Ohne lokale Umgebung: pushen und den Job „Kern und Inhalte“ in GitHub Actions ansehen.

## 7. Beispiele (gemeinsam mit dem Validator geprüft: 0 Fehler)

`npcs/village/mira.json`
```json
{
  "source": "frosthain:original",
  "dialogue": "frosthain:village/mira"
}
```

`quests/village/healing_herbs.json`
```json
{
  "source": "frosthain:original",
  "kind": "side",
  "sequential": true,
  "experience": 60,
  "objectives": [
    { "key": "gather_herbs", "type": "collect", "target": "minecraft:sweet_berries", "count": 5 },
    { "key": "return_mira", "type": "talk", "target": "frosthain:village/mira" }
  ]
}
```

`dialogues/village/mira.json`
```json
{
  "source": "frosthain:original",
  "speaker": "frosthain:village/mira",
  "start": "greeting",
  "nodes": [
    {
      "key": "greeting",
      "choices": [
        {
          "key": "offer_help",
          "next": "thanks",
          "conditions": [{ "type": "quest_state", "quest": "frosthain:village/healing_herbs", "state": "not_started" }],
          "effects": [{ "type": "start_quest", "quest": "frosthain:village/healing_herbs" }]
        },
        {
          "key": "bring_herbs",
          "next": "reward",
          "conditions": [{ "type": "quest_state", "quest": "frosthain:village/healing_herbs", "state": "active" }],
          "effects": [{ "type": "advance_objective", "quest": "frosthain:village/healing_herbs", "objective": "return_mira" }]
        },
        { "key": "goodbye" }
      ]
    },
    { "key": "thanks" },
    { "key": "reward" }
  ]
}
```

`powers/frost_lance.json`
```json
{
  "source": "frosthain:original",
  "kind": "active",
  "resource": "frosthain:spell_power",
  "cost": 30,
  "cooldown_ticks": 120,
  "school": "rune",
  "tags": ["projectile"],
  "values": { "range": 14, "damage_min": 6, "damage_max": 9, "duration_ticks": 60, "amplifier": 1 }
}
```

`talent_trees/survival.json`
```json
{
  "source": "frosthain:original",
  "owner": { "type": "general" },
  "root": "wanderer",
  "branches": ["endurance"],
  "nodes": [
    { "key": "wanderer", "kind": "root", "position": [0, 0] },
    { "key": "tough_hide", "kind": "passive", "position": [0, 1], "branch": "endurance",
      "requires": ["wanderer"], "max_rank": 3,
      "effects": [{ "type": "attribute", "attribute": "minecraft:max_health", "operation": "add_value", "amount": 1 }] }
  ]
}
```

`assets/frosthain/lang/de_de.json`
```json
{
  "npc.frosthain.village.mira": "Mira",
  "npc.frosthain.village.mira.title": "Kräuterkundige",
  "quest.frosthain.village.healing_herbs": "Heilkräuter für Mira",
  "quest.frosthain.village.healing_herbs.desc": "Mira braucht Beeren für ihre Salben.",
  "quest.frosthain.village.healing_herbs.objective.gather_herbs": "Sammle 5 Süßbeeren.",
  "quest.frosthain.village.healing_herbs.objective.return_mira": "Bring sie zu Mira.",
  "dialogue.frosthain.village.mira.greeting": "Ah, ein neues Gesicht. Hast du kurz Zeit?",
  "dialogue.frosthain.village.mira.greeting.choice.offer_help": "Kann ich dir helfen?",
  "dialogue.frosthain.village.mira.greeting.choice.bring_herbs": "Hier sind deine Beeren.",
  "dialogue.frosthain.village.mira.greeting.choice.goodbye": "Ein andermal.",
  "dialogue.frosthain.village.mira.thanks": "Fünf Süßbeeren, mehr brauche ich nicht. Danke!",
  "dialogue.frosthain.village.mira.reward": "Wunderbar. Nimm diese Salbe als Dank.",
  "power.frosthain.frost_lance": "Frostlanze",
  "power.frosthain.frost_lance.desc": "Eine Lanze aus Eis: Schaden und Verlangsamung.",
  "talent_tree.frosthain.survival": "Überleben",
  "talent_tree.frosthain.survival.branch.endurance": "Ausdauer",
  "talent.frosthain.survival.wanderer": "Wanderer",
  "talent.frosthain.survival.wanderer.desc": "Ausgangspunkt des Baums.",
  "talent.frosthain.survival.tough_hide": "Zähe Haut",
  "talent.frosthain.survival.tough_hide.desc": "+1 maximale Gesundheit je Rang."
}
```

## 8. Was die Technik braucht (Sprint-Prioritäten)

1. **Startdorf:** 3–4 NPCs mit Dialogen (Pfad `village/…`), Eldrin bleibt Dorfältester. Je NPC ein Satz zur Rolle im README (Schmied, Händlerin, …).
2. **Nebenquest „Verlorene Vorräte“** (`frosthain:lost_supplies`, Ziel `find_crates`, 3× `frosthain:supply_crate`): Texte, ggf. Auftraggeber-Dialog.
3. **Dungeon und Boss:** technisch umgesetzt als Quest `frosthain:frozen_crypt` (Ziele `find_crypt`, `defeat_warden` → `frosthain:boss/crypt_warden`), Boss „Hrímgar, der Frostwächter“ (`entity.frosthain.crypt_warden`), Ansagen `gui.frosthain.boss.awakens`, `.phase_two`, `.nova`, `.defeated`. Bessere Texte dürfen diese Schlüssel in `frosthain_story` überschreiben; IDs und Zielschlüssel bleiben.
4. Talentbäume für 2–3 weitere Klassen (`frosthain:rune_mage`, `frosthain:rogue`, `frosthain:cleric`).
5. Tutorial- und UI-Texte als Vorschlag im README (Schlüssel vergibt die Technik).

## 9. Migrationen

| Version | Datum | Änderung |
|---|---|---|
| 1 | 2026-11-02 | Erstfassung |
| 2 | 2026-11-02 | Neuer Inhaltstyp `runes` (rein additiv, bestehende Inhalte bleiben gültig) |
