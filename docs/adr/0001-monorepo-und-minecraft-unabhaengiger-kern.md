# ADR 0001 · Monorepo mit Minecraft-unabhängigem Kern

**Status:** angenommen · **Datum:** 2026-11-01

## Kontext
Launcher, Mod und Inhalte müssen zueinander passen. Minecraft ändert mit jedem Drop interne APIs (26.1: GUI-Rendering, 26.2: Vulkan). Regeln eines RPGs ändern sich dagegen nicht, wenn Mojang die Render-API umbaut.

## Entscheidung
- Ein Repository für Kern, Inhalte, Mod, Launcher und Dokumentation.
- Ein Modul `core` ohne jede Minecraft-Abhängigkeit enthält Regeln, Datenmodell, Lader, Validator und Speicherformat. Die Mod ist ein dünner Adapter darüber.

## Folgen
- Regeln sind in Sekunden testbar, auch ohne Minecraft und auf jedem CI-Runner.
- Minecraft-Updates betreffen nur den Adapter.
- Gson ist die einzige Laufzeitabhängigkeit des Kerns; sie wird von Minecraft bereitgestellt und nicht mitgebündelt.
