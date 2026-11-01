package net.frosthain.core.content;

import net.frosthain.core.ContentId;

import java.util.Map;
import java.util.Optional;

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

    private static final Map<ContentId, FlagInfo> FLAGS = Map.of(
            ContentId.of("darkvision"), new FlagInfo(ContentId.of("darkvision"), Implementation.PLANNED,
                    "Sicht in der Dunkelheit: aufgehellte Umgebung in geringer Reichweite, ohne Nachtsicht-Effekt."),
            ContentId.of("water_breathing"), new FlagInfo(ContentId.of("water_breathing"), Implementation.PLANNED,
                    "Kein Luftverlust unter Wasser."),
            ContentId.of("powerful_build"), new FlagInfo(ContentId.of("powerful_build"), Implementation.PLANNED,
                    "Höhere Traglast und Rückstoßresistenz."),
            ContentId.of("nimble_escape"), new FlagInfo(ContentId.of("nimble_escape"), Implementation.PLANNED,
                    "Ausweichrolle kostet weniger Ausdauer."),
            ContentId.of("breath_weapon"), new FlagInfo(ContentId.of("breath_weapon"), Implementation.PLANNED,
                    "Odemwaffe; Schadensart über die Abstammung."),
            ContentId.of("fey_ancestry"), new FlagInfo(ContentId.of("fey_ancestry"), Implementation.PLANNED,
                    "Vorteil gegen Bezauberung, immun gegen magischen Schlaf."),
            ContentId.of("relentless_endurance"), new FlagInfo(ContentId.of("relentless_endurance"), Implementation.PLANNED,
                    "Einmal pro langer Rast: tödlicher Treffer lässt mit 1 Lebenspunkt überleben."),
            ContentId.of("lucky"), new FlagInfo(ContentId.of("lucky"), Implementation.PLANNED,
                    "Misslungene Proben (Würfel 1) werden einmal neu gewürfelt.")
    );

    private EngineFlags() {
    }

    public static Optional<FlagInfo> lookup(ContentId id) {
        return Optional.ofNullable(FLAGS.get(id));
    }

    public static Map<ContentId, FlagInfo> all() {
        return FLAGS;
    }
}
