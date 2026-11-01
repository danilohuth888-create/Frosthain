package net.frosthain.core.registry;

import java.io.IOException;
import java.io.Reader;

/**
 * One JSON file of a content pack, independent of where it comes from
 * (a directory in the repository, a mod jar, or Minecraft's resource manager).
 *
 * @param pack      name of the pack, used for override diagnostics
 * @param namespace namespace of the resulting id
 * @param folder    content folder below {@code frosthain/}, e.g. {@code classes} or {@code rules}
 * @param path      path below the folder without {@code .json}, e.g. {@code fighter}
 * @param location  human-readable origin for error messages
 */
public record ContentFile(String pack, String namespace, String folder, String path, String location, Opener opener) {

    @FunctionalInterface
    public interface Opener {
        Reader open() throws IOException;
    }

    public Reader open() throws IOException {
        return opener.open();
    }
}
