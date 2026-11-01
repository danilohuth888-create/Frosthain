# Third-party notices

Stand: 01.11.2026. Nur Komponenten, die im Repository tatsächlich enthalten sind oder von ausgelieferten Teilen genutzt werden. Jede Angabe wurde an der genannten Quelle geprüft; nichts ist pauschal eingetragen. Vor jeder Veröffentlichung aktualisieren (Vorlage: `docs/lizenzen/RECHTE_UND_LIZENZEN.md`).

| Komponente | Version | Rechteinhaber | Quelle | Lizenz (geprüft an) | Änderungen | Verwendung / Pflichten |
|---|---|---|---|---|---|---|
| System Reference Document 5.2.1 | 5.2.1 | Wizards of the Coast LLC | https://www.dndbeyond.com/srd | CC-BY-4.0 (Lizenzhinweis des SRD) | Regelelemente angepasst, Texte neu formuliert, gekennzeichnet mit `"adapted": true` | Namensnennung siehe `NOTICE.md`; keine Andeutung einer Billigung |
| Cinzel (Schrift) | Variable Font aus google/fonts, abgerufen 01.11.2026; Upstream-Commit `8271e162a7c6` | The Cinzel Project Authors (Natanael Gama) | https://github.com/NDISCOVER/Cinzel | SIL Open Font License 1.1 (`content/frosthain_base/assets/frosthain/font/Cinzel-OFL.txt`); keine Reserved Font Names | keine | Lizenztext mitliefern; Schrift nicht einzeln verkaufen |
| EB Garamond (Schrift, normal und kursiv) | Variable Fonts aus google/fonts, abgerufen 01.11.2026; Upstream-Commit `106a4a6d3779` | The EB Garamond Project Authors (Georg Duffner, Octavio Pardo) | https://github.com/octaviopardo/EBGaramond12 | SIL Open Font License 1.1 (`…/font/EBGaramond-OFL.txt`); keine Reserved Font Names | keine | wie oben |
| Gson | 2.13.2 | Google LLC | https://github.com/google/gson | Apache-2.0 (`gson-parent-2.13.2.pom`, Maven Central) | keine | Wird zur Laufzeit von Minecraft bereitgestellt; vom Prototyp mitgeliefert → Apache-2.0-Hinweis beilegen |
| Gradle Wrapper | 9.4.0 (Distribution), `gradle-wrapper.jar` im Repository | Gradle Inc. | https://gradle.org | Apache-2.0 (`META-INF/LICENSE` im JAR) | keine | nur Build-Werkzeug, nicht Teil des Spiels |
| JUnit | 6.1.3 | JUnit-Team | https://junit.org | EPL-2.0 (`junit-bom-6.1.3.pom`) | keine | nur Tests, wird nicht ausgeliefert |

## Noch nicht enthalten, aber geplant (vor Aufnahme prüfen)

Fabric Loader, Fabric API, Fabric Loom, Tauri und alle npm-/Cargo-Abhängigkeiten des Launchers. Einträge erst nach Prüfung der konkret verwendeten Version.
