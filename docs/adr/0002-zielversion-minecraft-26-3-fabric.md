# ADR 0002 · Zielversion Minecraft Java 26.3 mit Fabric, Java 25

**Status:** angenommen, Toolchain verifiziert (CI, 01.11.2026) · **Datum:** 2026-11-01

## Kontext
Gewünscht ist Minecraft Java 26.3 mit Fabric. Laut Sekundärquellen erschien 26.3 am 15.09.2026. Seit 26.1 ist Minecraft nicht mehr obfuskiert; Fabric empfiehlt das Plugin `net.fabricmc.fabric-loom` (ohne Remapping), Loom 1.15+, Gradle 9.4 und Java 25.

## Entscheidung
- Ziel: Minecraft Java 26.3, Fabric Loader, Fabric API. Exakte Versionen werden in M0.3 aus den offiziellen Quellen ermittelt und gepinnt.
- Build mit Gradle 9.4 und JDK 25 (der Kern baut bereits so).
- Jede FROSTHAIN-Version pinnt genau eine Minecraft-Version; Updates auf neue Drops sind eigene, geplante Arbeitspakete.
- Rendering ausschließlich über Vanilla-Abstraktionen, kein direktes OpenGL (Vulkan-Backend ab 26.2).

## Verifikation (01.11.2026, GitHub Actions)
- `meta.fabricmc.net` führt Minecraft **26.3 als stabil**.
- Gepinnt in `gradle.properties`: Fabric Loader **0.20.1**, Fabric API **0.165.0+26.3**, Loom **1.19.3**.
- Loom 1.19.3 verlangt **Gradle ≥ 9.8.0** → Wrapper auf Gradle 9.8.2 angehoben.
- `:mod:build` erfolgreich; das Jar enthält `fabric.mod.json`, Inhalte, Schriften sowie `core` und `ui` als Jar-in-Jar.
- Dedicated Server startet im CI (privater Testserver, EULA laut Freigabe des Inhabers nur dort): `Done (3.149s)`, FROSTHAIN lädt 12 Klassen, 9 Spezies, 3 Talentbäume, 3 Quests, 1 Dialog ohne Fehler.
- Noch offen: Start im Client und GameTest (M0.3, Teil 2).

## Fallback
Fehlt für 26.3 eine zwingend benötigte Komponente, wird auf 26.2 ausgewichen und das hier dokumentiert.
