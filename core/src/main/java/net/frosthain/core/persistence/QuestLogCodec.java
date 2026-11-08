package net.frosthain.core.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.frosthain.core.ContentId;
import net.frosthain.core.json.JsonObjectReader;
import net.frosthain.core.quest.QuestLog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * JSON representation of {@link QuestLog} with schema versioning, stored next to the character sheet.
 * Field names are snake_case and stable; any change requires a new schema version plus a migration.
 */
public final class QuestLogCodec {

    public static final int CURRENT_VERSION = 1;
    private static final SchemaMigrator MIGRATOR = new SchemaMigrator(CURRENT_VERSION, List.of());
    private static final int MAX_COUNT = 1_000_000;

    private QuestLogCodec() {
    }

    public static JsonObject encode(QuestLog log) {
        JsonObject json = new JsonObject();
        json.addProperty(SchemaMigrator.VERSION_KEY, CURRENT_VERSION);
        JsonObject quests = new JsonObject();
        log.quests().forEach((quest, progress) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("status", progress.status().name().toLowerCase(Locale.ROOT));
            JsonObject counts = new JsonObject();
            progress.counts().forEach(counts::addProperty);
            entry.add("counts", counts);
            quests.add(quest.toString(), entry);
        });
        json.add("quests", quests);
        JsonArray pinned = new JsonArray();
        log.pinned().forEach(quest -> pinned.add(quest.toString()));
        json.add("pinned", pinned);
        return json;
    }

    public static QuestLog decode(JsonObject stored) {
        JsonObjectReader json = new JsonObjectReader(MIGRATOR.migrate(stored).data(), "$");
        json.integer(SchemaMigrator.VERSION_KEY);
        Map<ContentId, QuestLog.Progress> quests = new LinkedHashMap<>();
        JsonObjectReader questJson = json.object("quests");
        for (String key : questJson.keys()) {
            JsonObjectReader entry = questJson.object(key);
            QuestLog.Status status = entry.enumValue("status", QuestLog.Status.class);
            Map<String, Integer> counts = new TreeMap<>();
            JsonObjectReader countJson = entry.object("counts");
            for (String objective : countJson.keys()) {
                counts.put(objective, countJson.integerInRange(objective, 0, 0, MAX_COUNT));
            }
            countJson.finish();
            entry.finish();
            quests.put(ContentId.parse(key), new QuestLog.Progress(status, counts));
        }
        questJson.finish();
        List<ContentId> pinned = json.idList("pinned").stream().filter(quests::containsKey).toList();
        json.finish();
        return new QuestLog(quests, pinned);
    }
}
