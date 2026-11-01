# ADR 0002 · Zielversion Minecraft Java 26.3 mit Fabric, Java 25

**Status:** angenommen, Verifikation der Toolchain offen (M0.3) · **Datum:** 2026-11-01

## Kontext
Gewünscht ist Minecraft Java 26.3 mit Fabric. Laut Sekundärquellen erschien 26.3 am 15.09.2026. Seit 26.1 ist Minecraft nicht mehr obfuskiert; Fabric empfiehlt das Plugin `net.fabricmc.fabric-loom` (ohne Remapping), Loom 1.15+, Gradle 9.4 und Java 25.

## Entscheidung
- Ziel: Minecraft Java 26.3, Fabric Loader, Fabric API. Exakte Versionen werden in M0.3 aus den offiziellen Quellen ermittelt und gepinnt.
- Build mit Gradle 9.4 und JDK 25 (der Kern baut bereits so).
- Jede FROSTHAIN-Version pinnt genau eine Minecraft-Version; Updates auf neue Drops sind eigene, geplante Arbeitspakete.
- Rendering ausschließlich über Vanilla-Abstraktionen, kein direktes OpenGL (Vulkan-Backend ab 26.2).

## Fallback
Fehlt für 26.3 eine zwingend benötigte Komponente, wird auf 26.2 ausgewichen und das hier dokumentiert.
