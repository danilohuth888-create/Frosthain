package net.frosthain.core;

import net.frosthain.core.registry.ContentFile;
import net.frosthain.core.registry.ContentLoader;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.DirectoryContentSource;
import net.frosthain.core.registry.LangFile;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test helpers: the real base content pack and small in-memory packs. */
public final class TestContent {

    private static ContentLoader.Result base;

    private TestContent() {
    }

    public static Path contentRoot() {
        String configured = System.getProperty("frosthain.contentRoot");
        Path root = configured != null ? Path.of(configured) : Path.of("..", "content");
        assertTrue(Files.isDirectory(root), "Inhaltsverzeichnis nicht gefunden: " + root.toAbsolutePath());
        return root;
    }

    /** The shipped base content, loaded once per test run. */
    public static synchronized ContentLoader.Result baseResult() {
        if (base == null) {
            DirectoryContentSource.Scan scan = DirectoryContentSource.scanAllPacks(contentRoot());
            base = ContentLoader.load(scan.contentFiles(), scan.langFiles());
            assertFalse(base.hasErrors(), () -> "Basisinhalte haben Ladefehler: " + base.diagnostics());
        }
        return base;
    }

    public static ContentRegistry baseRegistry() {
        return baseResult().registry();
    }

    /**
     * Builder for an in-memory pack. Keys are {@code folder/path} (namespace {@code frosthain}),
     * values are JSON strings. Starts with a minimal valid setup (sources, rules, ability translations).
     */
    public static final class Pack {
        private final Map<String, String> files = new LinkedHashMap<>();
        private final Map<String, String> german = new LinkedHashMap<>();

        public Pack() {
            files.put("sources/original", """
                    {"title": "Test", "publisher": "Test", "license": "FROSTHAIN-PROJECT", "status": "approved"}""");
            files.put("rules/progression", """
                    {"max_level": 5, "experience_thresholds": [0, 100, 300, 600, 1000], "talent_points_per_level": 1,
                     "multiclass": {"enabled": true, "max_classes": 2, "minimum_level_before_multiclass": 2}}""");
            files.put("rules/ability_scores", """
                    {"standard_array": [15, 14, 13, 12, 10, 8],
                     "point_buy": {"budget": 27, "costs": {"8": 0, "9": 1, "10": 2, "11": 3, "12": 4, "13": 5, "14": 7, "15": 9}}}""");
            for (String ability : List.of("strength", "dexterity", "constitution", "intelligence", "wisdom", "charisma")) {
                german.put("ability.frosthain." + ability, ability);
            }
        }

        public Pack file(String folderAndPath, String json) {
            files.put(folderAndPath, json);
            return this;
        }

        public Pack translate(String... keys) {
            for (String key : keys) {
                german.put(key, key);
            }
            return this;
        }

        public ContentLoader.Result load() {
            List<ContentFile> contentFiles = new ArrayList<>();
            files.forEach((key, json) -> {
                int slash = key.indexOf('/');
                contentFiles.add(new ContentFile("test", "frosthain", key.substring(0, slash), key.substring(slash + 1),
                        "test/" + key + ".json", () -> new StringReader(json)));
            });
            StringBuilder lang = new StringBuilder("{");
            german.forEach((k, v) -> lang.append(lang.length() > 1 ? "," : "").append('"').append(k).append("\":\"").append(v).append('"'));
            lang.append('}');
            String langJson = lang.toString();
            List<LangFile> langFiles = List.of(new LangFile("test", "de_de", "test/de_de.json", () -> new StringReader(langJson)));
            return ContentLoader.load(contentFiles, langFiles);
        }
    }
}
