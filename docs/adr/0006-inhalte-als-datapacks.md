# ADR 0006 · Inhalte als Datapack-JSON mit strikter Validierung

**Status:** angenommen · **Datum:** 2026-11-01

## Entscheidung
- Inhalte liegen als JSON im Datapack-Layout `data/<namespace>/frosthain/<typ>/<pfad>.json`; dieselben Dateien lädt die Mod als Datapack.
- Der Lader ist strikt: unbekannte Felder und falsche Typen sind Fehler mit JSON-Pfad. Kommentare über `_`-Felder.
- Ein Validator prüft Querbezüge, Lizenz, Talentbaumstruktur und Übersetzungen – im CI und nach jedem `/reload`.
- Neue *Inhalte* brauchen keinen Code; neue *Mechaniken* (Modifikator-Typen, Engine-Funktionen) schon. Engine-Funktionen tragen einen ehrlichen Umsetzungsstatus.
- Mojang-Codecs werden im Kern nicht verwendet (keine Minecraft-Abhängigkeit); die Mod reicht Gson-`JsonElement`s an den Kern weiter.
