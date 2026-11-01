package net.frosthain.core.persistence;

import com.google.gson.JsonObject;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Upgrades stored JSON step by step ({@code v1 -> v2 -> ...}) to the current schema.
 *
 * <p>Safety rules (docs/02-architektur.md, "Persistenz"):
 * <ul>
 *   <li>Data from a <em>newer</em> version is never loaded or overwritten ({@link FutureVersionException}).</li>
 *   <li>Migrations work on copies; the caller writes a backup of the original before saving the result.</li>
 * </ul>
 */
public final class SchemaMigrator {

    public static final String VERSION_KEY = "schema_version";

    /** One upgrade step from {@code fromVersion} to {@code fromVersion + 1}. */
    public record Migration(int fromVersion, String description, UnaryOperator<JsonObject> transform) {
    }

    public static final class FutureVersionException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;

        public FutureVersionException(int found, int supported) {
            super("Daten stammen aus einer neueren FROSTHAIN-Version (Schema " + found + ", unterstützt bis " + supported
                    + "). Sie werden nicht geladen, damit nichts überschrieben wird.");
        }
    }

    private final int currentVersion;
    private final List<Migration> migrations;

    public SchemaMigrator(int currentVersion, List<Migration> migrations) {
        this.currentVersion = currentVersion;
        this.migrations = List.copyOf(migrations);
        for (int version = 1; version < currentVersion; version++) {
            final int from = version;
            long count = this.migrations.stream().filter(m -> m.fromVersion() == from).count();
            if (count != 1) {
                throw new IllegalArgumentException("Genau eine Migration von Version " + from + " erwartet, gefunden: " + count);
            }
        }
    }

    public int currentVersion() {
        return currentVersion;
    }

    public record Outcome(JsonObject data, int originalVersion, List<String> appliedSteps) {
        public boolean migrated() {
            return !appliedSteps.isEmpty();
        }
    }

    public Outcome migrate(JsonObject stored) {
        int version = stored.has(VERSION_KEY) ? stored.get(VERSION_KEY).getAsInt() : 1;
        if (version > currentVersion) {
            throw new FutureVersionException(version, currentVersion);
        }
        if (version < 1) {
            throw new IllegalArgumentException("Ungültige Schemaversion " + version);
        }
        JsonObject data = stored.deepCopy();
        int original = version;
        List<String> applied = new ArrayList<>();
        while (version < currentVersion) {
            final int from = version;
            Migration migration = migrations.stream().filter(m -> m.fromVersion() == from).findFirst().orElseThrow();
            data = migration.transform().apply(data);
            version++;
            data.addProperty(VERSION_KEY, version);
            applied.add("v" + from + " → v" + version + ": " + migration.description());
        }
        return new Outcome(data, original, List.copyOf(applied));
    }
}
