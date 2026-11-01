package net.frosthain.core.content;

import net.frosthain.core.ContentId;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Engine features that content can switch on via {@link Modifier.Flag}.
 *
 * <p>A flag is only listed here once its behaviour is specified. {@link Implementation} documents
 * honestly whether the mod already implements it, so content can be authored ahead of the code
 * without pretending the feature works.
 */
public final class EngineFlags {

    public enum Implementation {
        /** Specified, not yet implemented in the mod. Content using it loads, but has no gameplay effect yet. */
        PLANNED,
        /** Implemented and covered by tests in the mod. */
        IMPLEMENTED
    }

    public record FlagInfo(ContentId id, Implementation implementation, String description) {
    }

    private static final Map<ContentId, FlagInfo> FLAGS = Stream.of(
            flag("darkvision", Implementation.PLANNED,
                    "Sicht in der Dunkelheit: aufgehellte Umgebung in geringer Reichweite, ohne Nachtsicht-Effekt."),
            flag("water_breathing", Implementation.PLANNED,
                    "Kein Luftverlust unter Wasser."),
            flag("powerful_build", Implementation.PLANNED,
                    "Höhere Traglast und Rückstoßresistenz."),
            flag("nimble_escape", Implementation.PLANNED,
                    "Ausweichrolle kostet weniger Ausdauer."),
            flag("breath_weapon", Implementation.PLANNED,
                    "Odemwaffe; Schadensart über die Abstammung."),
            flag("fey_ancestry", Implementation.PLANNED,
                    "Vorteil gegen Bezauberung, immun gegen magischen Schlaf."),
            flag("relentless_endurance", Implementation.PLANNED,
                    "Einmal pro langer Rast: tödlicher Treffer lässt mit 1 Lebenspunkt überleben."),
            flag("lucky", Implementation.PLANNED,
                    "Misslungene Proben (Würfel 1) werden einmal neu gewürfelt."),
            flag("rune_sight", Implementation.PLANNED,
                    "Verborgene Runen und Runensteine in der Nähe werden sichtbar."),
            flag("ore_sense", Implementation.PLANNED,
                    "Erze in kurzer Reichweite schimmern kurz auf (Abklingzeit)."),
            flag("thrifty_crafting", Implementation.PLANNED,
                    "Geringe Chance, beim Herstellen eine Zutat zurückzuerhalten."),
            flag("masterwork", Implementation.PLANNED,
                    "Selbst hergestellte Werkzeuge und Rüstungen erhalten zusätzliche Haltbarkeit.")
    ).collect(Collectors.toUnmodifiableMap(FlagInfo::id, info -> info));

    private static FlagInfo flag(String path, Implementation implementation, String description) {
        return new FlagInfo(ContentId.of(path), implementation, description);
    }

    private EngineFlags() {
    }

    public static Optional<FlagInfo> lookup(ContentId id) {
        return Optional.ofNullable(FLAGS.get(id));
    }

    public static Map<ContentId, FlagInfo> all() {
        return FLAGS;
    }
}
