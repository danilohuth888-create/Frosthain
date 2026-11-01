package net.frosthain.core.registry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Reads content packs from directories with data pack layout:
 * <pre>
 * &lt;pack&gt;/data/&lt;namespace&gt;/frosthain/&lt;folder&gt;/&lt;path&gt;.json
 * &lt;pack&gt;/assets/&lt;namespace&gt;/lang/&lt;locale&gt;.json
 * </pre>
 * Used by tests and the command line validator. The Fabric mod reads the same layout through
 * Minecraft's resource manager instead.
 */
public final class DirectoryContentSource {

    public record Scan(List<ContentFile> contentFiles, List<LangFile> langFiles) {
    }

    private DirectoryContentSource() {
    }

    /** Scans every sub directory of {@code contentRoot} as one pack, in alphabetical order. */
    public static Scan scanAllPacks(Path contentRoot) {
        try (Stream<Path> children = Files.list(contentRoot)) {
            List<Path> packs = children.filter(Files::isDirectory).sorted().toList();
            return scanPacks(packs);
        } catch (IOException e) {
            throw new UncheckedIOException("Inhaltsverzeichnis nicht lesbar: " + contentRoot, e);
        }
    }

    public static Scan scanPacks(List<Path> packRoots) {
        List<ContentFile> files = new ArrayList<>();
        List<LangFile> langs = new ArrayList<>();
        for (Path packRoot : packRoots) {
            scanPack(packRoot, files, langs);
        }
        return new Scan(List.copyOf(files), List.copyOf(langs));
    }

    private static void scanPack(Path packRoot, List<ContentFile> files, List<LangFile> langs) {
        String pack = packRoot.getFileName().toString();
        Path data = packRoot.resolve("data");
        if (Files.isDirectory(data)) {
            for (Path namespaceDir : listDirectories(data)) {
                String namespace = namespaceDir.getFileName().toString();
                Path contentDir = namespaceDir.resolve(ContentType.ROOT_FOLDER);
                if (!Files.isDirectory(contentDir)) {
                    continue;
                }
                for (Path folderDir : listDirectories(contentDir)) {
                    String folder = folderDir.getFileName().toString();
                    for (Path json : listJsonFiles(folderDir)) {
                        String relative = folderDir.relativize(json).toString().replace('\\', '/');
                        String path = relative.substring(0, relative.length() - ".json".length());
                        String location = pack + "/" + packRoot.relativize(json).toString().replace('\\', '/');
                        files.add(new ContentFile(pack, namespace, folder, path, location,
                                () -> Files.newBufferedReader(json, StandardCharsets.UTF_8)));
                    }
                }
            }
        }
        Path assets = packRoot.resolve("assets");
        if (Files.isDirectory(assets)) {
            for (Path namespaceDir : listDirectories(assets)) {
                Path langDir = namespaceDir.resolve("lang");
                if (!Files.isDirectory(langDir)) {
                    continue;
                }
                for (Path json : listJsonFiles(langDir)) {
                    String fileName = json.getFileName().toString();
                    String locale = fileName.substring(0, fileName.length() - ".json".length());
                    String location = pack + "/" + packRoot.relativize(json).toString().replace('\\', '/');
                    langs.add(new LangFile(pack, locale, location, () -> Files.newBufferedReader(json, StandardCharsets.UTF_8)));
                }
            }
        }
    }

    private static List<Path> listDirectories(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isDirectory).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> listJsonFiles(Path dir) {
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
