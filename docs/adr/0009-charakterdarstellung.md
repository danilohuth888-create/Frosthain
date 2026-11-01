# ADR 0009 · Charakterdarstellung: Skin-Compositing und Zusatzteile statt Ersatzmodell

**Status:** angenommen · **Datum:** 2026-11-01

## Kontext
Gefordert ist umfangreiche optische Anpassung mit 3D-Vorschau. Ein komplett eigenes Spielermodell würde Rüstungen, Animationen und andere Mods brechen.

## Entscheidung
- Das Vanilla-Spielermodell bleibt. Aus den Aussehensdaten wird zur Laufzeit eine 64×64-Textur zusammengesetzt (Ebenen für Haut, Gesicht, Haare, Bart, Markierungen, Kleidung), zwischengespeichert.
- Ohren, Hörner, Schweife, Hauer, lange Haare und Accessoires sind zusätzliche Modellteile in einer eigenen Render-Ebene.
- Spezies-Größe über `minecraft:scale` (Gameplay-relevant), zusätzlich ±5 % rein optisch.
- Keine freien Körperproportionen, keine Gesichtsmorphs.
- Keine Pflichtabhängigkeit von Animationsbibliotheken (z. B. GeckoLib), solange deren Unterstützung für 26.3 nicht gesichert ist.
