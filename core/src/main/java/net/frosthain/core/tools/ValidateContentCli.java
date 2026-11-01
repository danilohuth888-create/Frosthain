package net.frosthain.core.tools;

import net.frosthain.core.registry.ContentLoader;
import net.frosthain.core.registry.ContentType;
import net.frosthain.core.registry.DirectoryContentSource;
import net.frosthain.core.validation.ContentValidator;
import net.frosthain.core.validation.Diagnostic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line content check: {@code ./gradlew :core:validateContent}.
 * Exit code 0 = no errors, 1 = errors found, 2 = usage problem.
 */
public final class ValidateContentCli {

    private ValidateContentCli() {
    }

    public static void main(String[] args) {
        if (args.length != 1 || !Files.isDirectory(Path.of(args[0]))) {
            System.err.println("Aufruf: ValidateContentCli <content-verzeichnis>");
            System.exit(2);
            return;
        }
        Path root = Path.of(args[0]);
        DirectoryContentSource.Scan scan = DirectoryContentSource.scanAllPacks(root);
        ContentLoader.Result result = ContentLoader.load(scan.contentFiles(), scan.langFiles());
        List<Diagnostic> diagnostics = new ArrayList<>(result.diagnostics());
        diagnostics.addAll(ContentValidator.validate(result.registry()));
        diagnostics.sort(Diagnostic.ORDER);

        System.out.println("FROSTHAIN Inhaltsprüfung: " + root.toAbsolutePath());
        for (ContentType<?> type : ContentType.ALL) {
            System.out.printf("  %-14s %4d%n", type.folder(), result.registry().all(type).size());
        }
        System.out.println("  Sprachen       " + result.registry().locales());
        diagnostics.forEach(d -> System.out.println(d));
        long errors = diagnostics.stream().filter(d -> d.severity() == Diagnostic.Severity.ERROR).count();
        long warnings = diagnostics.stream().filter(d -> d.severity() == Diagnostic.Severity.WARNING).count();
        System.out.println("Ergebnis: " + errors + " Fehler, " + warnings + " Warnungen");
        System.exit(errors > 0 ? 1 : 0);
    }
}
