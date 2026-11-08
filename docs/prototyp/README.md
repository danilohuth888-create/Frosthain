# UI-Prototyp (B · D · F)

Interaktiver Desktop-Prototyp der Oberflächen aus den Referenzbildern. Er nutzt **dieselben Regeln und dieselbe Präsentationslogik** wie später die Mod (`core`, `ui`); nur die Zeichenschicht ist Java2D statt Minecraft.

## Starten (Windows, JDK 25)

```bash
gradlew.bat :ui-prototype:run
```

| Ansicht | Steuerung |
|---|---|
| HUD (B) | `K` Talente · Rechtsklick oder `T` Eldrin ansprechen · `H` Questtracker ein-/ausklappen · `C` Kiste gefunden (Questfortschritt) · `L` Stufe +1 |
| Talente (D) | Klick wählen · Ziehen verschieben · Mausrad zoomen · Pfeiltasten · Enter freischalten · Entf zurücknehmen · `1`–`4` Zweig · `0` Ansicht · Esc zurück |
| Dialog (F) | Maus, Pfeiltasten oder `1`–`9` · Enter · Esc · `F9` Schaden simulieren (Unterbrechung) · `R` fortsetzen |

## Screenshots neu erzeugen (ohne Bildschirm)

```bash
./gradlew :ui-prototype:screenshots
```

Ergebnis: `tools/ui-prototype/build/screenshots/`. Die Bilder in diesem Ordner sind eine Momentaufnahme vom 01.11.2026.

| Datei | Zeigt |
|---|---|
| `B_hud.png` | Survival-HUD mit Minimap-Platzhalter, Questtracker (Haupt-/Nebenquest), Interaktionshinweis |
| `D_talente_kampfhaltung.png` | Talentbaum mit vier Zweigen, Detailtafel „Kampfhaltung · Freischaltbar · Kosten: 1 Talentpunkt“ |
| `D_talente_gesperrt_reiter_kampf.png` | Reiter „Kampf“ aktiv (übrige Zweige gedimmt), gesperrtes Talent mit Gründen |
| `D_runenpfad.png` | eigener Runenpfad als unterscheidbarer Pfad |
| `F_dialog_auswahl.png` | Dialog mit Eldrin, Text aus dem Referenzbild, Auswahl „Ich bin bereit.“ |
| `F_dialog_antwort.png` | Antwort mit „Weiter“ |
| `B_hud_nach_dialog.png` | Questtracker zeigt das nächste Ziel – der Dialog hat die Quest fortgeschrieben |

## Grenzen des Prototyps

- Hintergrund, Minimap und Vanilla-Leisten sind gezeichnete Platzhalter; im Spiel ist dort die echte Minecraft-Welt bzw. das Vanilla-HUD.
- Symbole sind einfache Vektorgrafiken; im Spiel werden es Texturen.
- Das NPC-Porträt ist ein Pixelbild; im Spiel eine 3D-Ansicht des NPC-Modells.
- Nichts davon läuft bisher in Minecraft – das ist Schritt U5 im [UI-Implementierungsplan](../10-ui-implementierungsplan.md).
