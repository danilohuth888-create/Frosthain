# ADR 0008 · Server-autoritative Spiellogik auch im Einzelspieler

**Status:** angenommen · **Datum:** 2026-11-01

## Entscheidung
Der (integrierte) Server hält jeden RPG-Zustand und prüft jede Aktion mit denselben Kernfunktionen, die der Client zur Anzeige nutzt. Der Client sendet nur Wünsche. Client-Code liegt in einem eigenen Source-Set und wird von gemeinsamem Code nie importiert.

## Folgen
Multiplayer ist später eine Erweiterung (Synchronisation, Berechtigungen), kein Umbau. Manipulierte Clients können keine ungültigen Charaktere oder Talente erzeugen.
