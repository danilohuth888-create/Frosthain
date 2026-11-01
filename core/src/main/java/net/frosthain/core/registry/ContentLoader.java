package net.frosthain.core.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import net.frosthain.core.ContentId;
import net.frosthain.core.content.ContentDefinition;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;
import net.frosthain.core.rules.AbilityScoreRules;
import net.frosthain.core.rules.ProgressionRules;
import net.frosthain.core.validation.Diagnostic;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds a {@link ContentRegistry} from content files. Errors in single files do not abort loading:
 * all problems are collected as {@link Diagnostic}s so authors see everything at once.
 */
public final class ContentLoader {

    public static final String RULES_FOLDER = "rules";

    public record Result(ContentRegistry registry, List<Diagnostic> diagnostics) {
        public boolean hasErrors() {
            return diagnostics.stream().anyMatch(d -> d.severity() == Diagnostic.Severity.ERROR);
        }
    }

    private ContentLoader() {
    }

    /**
     * Loads files in the given order. For identical ids, later files win (pack override semantics,
     * like Minecraft data packs); every override is reported as INFO.
     */
    public static Result load(List<ContentFile> files, List<LangFile> langFiles) {
        ContentRegistry.Builder builder = ContentRegistry.builder();
        List<Diagnostic> diagnostics = new ArrayList<>();
        Map<String, ContentType<?>> typesByFolder = new HashMap<>();
        for (ContentType<?> type : ContentType.ALL) {
            typesByFolder.put(type.folder(), type);
        }
        Map<String, String> originByKey = new HashMap<>();

        for (ContentFile file : files) {
            Optional<JsonElement> json = readJson(file.location(), file.opener(), diagnostics);
            if (json.isEmpty()) {
                continue;
            }
            if (file.folder().equals(RULES_FOLDER)) {
                loadRules(file, json.get(), builder, diagnostics);
                continue;
            }
            ContentType<?> type = typesByFolder.get(file.folder());
            if (type == null) {
                diagnostics.add(Diagnostic.warning(file.location(), "Unbekannter Inhaltsordner '" + file.folder() + "' wird ignoriert"));
                continue;
            }
            ContentId id;
            try {
                id = new ContentId(file.namespace(), file.path());
            } catch (IllegalArgumentException e) {
                diagnostics.add(Diagnostic.error(file.location(), e.getMessage()));
                continue;
            }
            parseInto(type, id, file, json.get(), builder, diagnostics, originByKey);
        }

        for (LangFile lang : langFiles) {
            readJson(lang.location(), lang.opener(), diagnostics).ifPresent(json -> {
                if (!json.isJsonObject()) {
                    diagnostics.add(Diagnostic.error(lang.location(), "Sprachdatei muss ein Objekt sein"));
                    return;
                }
                Map<String, String> entries = new LinkedHashMap<>();
                json.getAsJsonObject().entrySet().forEach(entry -> {
                    if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                        entries.put(entry.getKey(), entry.getValue().getAsString());
                    } else {
                        diagnostics.add(Diagnostic.error(lang.location(), "Wert für '" + entry.getKey() + "' muss Text sein"));
                    }
                });
                builder.translations(lang.locale(), entries);
            });
        }
        return new Result(builder.build(), List.copyOf(diagnostics));
    }

    private static <T extends ContentDefinition> void parseInto(ContentType<T> type, ContentId id, ContentFile file,
                                                                JsonElement json, ContentRegistry.Builder builder,
                                                                List<Diagnostic> diagnostics, Map<String, String> originByKey) {
        try {
            T definition = type.parse(id, JsonObjectReader.root(json));
            String key = type.folder() + "/" + id;
            if (builder.put(type, definition).isPresent()) {
                diagnostics.add(Diagnostic.info(file.location(),
                        type.folder() + " " + id + " überschreibt die Definition aus " + originByKey.get(key)));
            }
            originByKey.put(key, file.location());
        } catch (ContentParseException e) {
            diagnostics.add(Diagnostic.error(file.location(), e.getMessage()));
        }
    }

    private static void loadRules(ContentFile file, JsonElement json, ContentRegistry.Builder builder, List<Diagnostic> diagnostics) {
        try {
            JsonObjectReader reader = JsonObjectReader.root(json);
            switch (file.path()) {
                case "progression" -> builder.progression(ProgressionRules.parse(reader));
                case "ability_scores" -> builder.abilityScores(AbilityScoreRules.parse(reader));
                default -> {
                    diagnostics.add(Diagnostic.warning(file.location(), "Unbekannte Regeldatei wird ignoriert"));
                    return;
                }
            }
            reader.finish();
        } catch (ContentParseException e) {
            diagnostics.add(Diagnostic.error(file.location(), e.getMessage()));
        }
    }

    private static Optional<JsonElement> readJson(String location, ContentFile.Opener opener, List<Diagnostic> diagnostics) {
        try (Reader reader = opener.open(); JsonReader jsonReader = new JsonReader(reader)) {
            jsonReader.setStrictness(Strictness.STRICT);
            JsonElement element = JsonParser.parseReader(jsonReader);
            if (jsonReader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) {
                diagnostics.add(Diagnostic.error(location, "Zusätzlicher Inhalt nach dem JSON-Dokument"));
                return Optional.empty();
            }
            return Optional.of(element);
        } catch (IOException | JsonParseException | IllegalStateException e) {
            diagnostics.add(Diagnostic.error(location, "Ungültiges JSON: " + e.getMessage()));
            return Optional.empty();
        }
    }
}
