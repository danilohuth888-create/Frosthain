package net.frosthain.ui;

import net.frosthain.core.registry.ContentRegistry;

import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;

/**
 * Looks up texts from the content pack's language files: primary locale, then English, then the key itself
 * (so a missing text is visible instead of silently empty). Arguments use Minecraft's {@code %s} syntax.
 */
public final class Translator {

    public static final String FALLBACK_LOCALE = "en_us";

    private final Map<String, String> primary;
    private final Map<String, String> fallback;

    public Translator(ContentRegistry registry, String locale) {
        this.primary = registry.translations(locale);
        this.fallback = registry.translations(FALLBACK_LOCALE);
    }

    public boolean has(String key) {
        return primary.containsKey(key) || fallback.containsKey(key);
    }

    public String t(String key, Object... args) {
        String pattern = primary.getOrDefault(key, fallback.getOrDefault(key, key));
        if (args.length == 0) {
            return pattern;
        }
        try {
            return String.format(Locale.ROOT, pattern, args);
        } catch (IllegalFormatException e) {
            return pattern;
        }
    }
}
