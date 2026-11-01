package net.frosthain.core.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.frosthain.core.ContentId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Strict, path-aware reader for content JSON.
 *
 * <p>Every accessor reports errors with the JSON path (e.g. {@code $.nodes[3].requires[0]}), and
 * {@link #finish()} rejects unknown keys so that typos in content files are caught at load time instead
 * of silently being ignored. Keys starting with {@code _} are treated as comments and allowed anywhere.
 */
public final class JsonObjectReader {

    private final JsonObject object;
    private final String path;
    private final Set<String> consumed = new HashSet<>();

    public JsonObjectReader(JsonObject object, String path) {
        this.object = object;
        this.path = path;
    }

    public static JsonObjectReader root(JsonElement element) {
        return of(element, "$");
    }

    public static JsonObjectReader of(JsonElement element, String path) {
        if (element == null || !element.isJsonObject()) {
            throw new ContentParseException(path, "Objekt erwartet, gefunden: " + describe(element));
        }
        return new JsonObjectReader(element.getAsJsonObject(), path);
    }

    public String path() {
        return path;
    }

    public String childPath(String key) {
        return path + "." + key;
    }

    /** All keys of the object (for map-like objects such as talent allocations). */
    public Set<String> keys() {
        return Collections.unmodifiableSet(new TreeSet<>(object.keySet()));
    }

    public boolean has(String key) {
        return object.has(key) && !object.get(key).isJsonNull();
    }

    private Optional<JsonElement> take(String key) {
        consumed.add(key);
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? Optional.empty() : Optional.of(element);
    }

    private JsonElement require(String key) {
        return take(key).orElseThrow(() -> new ContentParseException(childPath(key), "Pflichtfeld fehlt"));
    }

    // ---- strings & ids --------------------------------------------------------------------------------

    public String string(String key) {
        return asString(require(key), childPath(key));
    }

    public Optional<String> optString(String key) {
        return take(key).map(e -> asString(e, childPath(key)));
    }

    public ContentId id(String key) {
        return asId(require(key), childPath(key));
    }

    public Optional<ContentId> optId(String key) {
        return take(key).map(e -> asId(e, childPath(key)));
    }

    public List<ContentId> idList(String key) {
        return list(key, (element, p) -> asId(element, p));
    }

    public List<String> stringList(String key) {
        return list(key, (element, p) -> asString(element, p));
    }

    public <E extends Enum<E>> E enumValue(String key, Class<E> type) {
        return asEnum(require(key), childPath(key), type);
    }

    public <E extends Enum<E>> E enumValue(String key, Class<E> type, E fallback) {
        return take(key).map(e -> asEnum(e, childPath(key), type)).orElse(fallback);
    }

    // ---- numbers & booleans ---------------------------------------------------------------------------

    public int integer(String key) {
        return asInt(require(key), childPath(key));
    }

    public int integer(String key, int fallback) {
        return take(key).map(e -> asInt(e, childPath(key))).orElse(fallback);
    }

    public int integerInRange(String key, int fallback, int min, int max) {
        int value = integer(key, fallback);
        if (value < min || value > max) {
            throw new ContentParseException(childPath(key), "Wert " + value + " liegt nicht in [" + min + ", " + max + "]");
        }
        return value;
    }

    public double number(String key) {
        return asDouble(require(key), childPath(key));
    }

    public double number(String key, double fallback) {
        return take(key).map(e -> asDouble(e, childPath(key))).orElse(fallback);
    }

    public boolean bool(String key, boolean fallback) {
        return take(key).map(e -> {
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) {
                return e.getAsBoolean();
            }
            throw new ContentParseException(childPath(key), "true/false erwartet, gefunden: " + describe(e));
        }).orElse(fallback);
    }

    public List<Integer> intList(String key) {
        return list(key, (element, p) -> asInt(element, p));
    }

    // ---- nested structures ----------------------------------------------------------------------------

    public JsonObjectReader object(String key) {
        return of(require(key), childPath(key));
    }

    public Optional<JsonObjectReader> optObject(String key) {
        return take(key).map(e -> of(e, childPath(key)));
    }

    /** Reads an optional array of objects; a missing key yields an empty list. */
    public <T> List<T> objectList(String key, Function<JsonObjectReader, T> parser) {
        return list(key, (element, p) -> {
            JsonObjectReader reader = of(element, p);
            T value = parser.apply(reader);
            reader.finish();
            return value;
        });
    }

    /** Reads an optional object whose values are integers, e.g. {@code {"strength": 13}}. */
    public Map<String, Integer> intMap(String key) {
        Optional<JsonElement> element = take(key);
        if (element.isEmpty()) {
            return Map.of();
        }
        JsonObjectReader reader = of(element.get(), childPath(key));
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String entryKey : reader.object.keySet()) {
            result.put(entryKey, asInt(reader.object.get(entryKey), reader.childPath(entryKey)));
        }
        return Collections.unmodifiableMap(result);
    }

    /** Raw access for polymorphic structures (e.g. modifier parameters). Marks the key as consumed. */
    public Optional<JsonElement> raw(String key) {
        return take(key);
    }

    @FunctionalInterface
    public interface ElementParser<T> {
        T parse(JsonElement element, String path);
    }

    public <T> List<T> list(String key, ElementParser<T> parser) {
        Optional<JsonElement> element = take(key);
        if (element.isEmpty()) {
            return List.of();
        }
        String listPath = childPath(key);
        if (!element.get().isJsonArray()) {
            throw new ContentParseException(listPath, "Liste erwartet, gefunden: " + describe(element.get()));
        }
        JsonArray array = element.get().getAsJsonArray();
        List<T> result = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            result.add(parser.parse(array.get(i), listPath + "[" + i + "]"));
        }
        return List.copyOf(result);
    }

    /** Rejects keys that were never read. Call after all fields were consumed. */
    public void finish() {
        Set<String> unknown = new TreeSet<>();
        for (String key : object.keySet()) {
            if (!consumed.contains(key) && !key.startsWith("_")) {
                unknown.add(key);
            }
        }
        if (!unknown.isEmpty()) {
            throw new ContentParseException(path, "Unbekannte Felder " + unknown + " (Tippfehler? Kommentare mit '_' beginnen)");
        }
    }

    // ---- static conversions ---------------------------------------------------------------------------

    public static String asString(JsonElement element, String path) {
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            return primitive.getAsString();
        }
        throw new ContentParseException(path, "Text erwartet, gefunden: " + describe(element));
    }

    public static ContentId asId(JsonElement element, String path) {
        String text = asString(element, path);
        try {
            return ContentId.parse(text);
        } catch (IllegalArgumentException e) {
            throw new ContentParseException(path, e.getMessage(), e);
        }
    }

    public static int asInt(JsonElement element, String path) {
        if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
            double value = primitive.getAsDouble();
            if (value == Math.rint(value) && Math.abs(value) <= Integer.MAX_VALUE) {
                return (int) value;
            }
        }
        throw new ContentParseException(path, "Ganzzahl erwartet, gefunden: " + describe(element));
    }

    public static double asDouble(JsonElement element, String path) {
        if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
            double value = primitive.getAsDouble();
            if (Double.isFinite(value)) {
                return value;
            }
        }
        throw new ContentParseException(path, "Zahl erwartet, gefunden: " + describe(element));
    }

    public static <E extends Enum<E>> E asEnum(JsonElement element, String path, Class<E> type) {
        String text = asString(element, path);
        try {
            return Enum.valueOf(type, text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            List<String> allowed = new ArrayList<>();
            for (E constant : type.getEnumConstants()) {
                allowed.add(constant.name().toLowerCase(Locale.ROOT));
            }
            throw new ContentParseException(path, "Unbekannter Wert '" + text + "', erlaubt: " + allowed, e);
        }
    }

    private static String describe(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "nichts";
        }
        if (element.isJsonObject()) {
            return "Objekt";
        }
        if (element.isJsonArray()) {
            return "Liste";
        }
        return "'" + element.getAsString() + "'";
    }
}
