# ADR 0003 · Launcher mit Tauri 2 (Rust + TypeScript/React)

**Status:** angenommen · **Datum:** 2026-11-01

## Kontext
Gesucht ist ein wartbarer, moderner Windows-Launcher mit hochwertiger Oberfläche, sicherem Token-Umgang und signierten Updates. Geprüft: Electron, Tauri 2, .NET mit Avalonia/WinUI, Java mit JavaFX (Vergleich in docs/04-launcher-konzept.md).

## Entscheidung
Tauri 2. Die Logik liegt in einer Rust-Bibliothek `launcher-core` (ohne Fenster testbar), die Oberfläche ist TypeScript/React. Die UI darf nur ausdrücklich freigegebene Befehle aufrufen.

## Folgen
- Kleine Installation, geringer Speicherbedarf, nutzt das in Windows 11 vorhandene WebView2.
- Zwei Sprachen im Projekt (Rust, TypeScript) zusätzlich zu Java – bewusst in Kauf genommen, weil die Launcher-Logik so am sichersten und testbarsten ist.
- Windows-Builds entstehen im CI auf Windows-Runnern.
