package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A quest. Quests are started by game events or dialogue effects, never by timers, and there is no limit
 * on how many can be active at once.
 *
 * @param sequential if true, objectives are shown and progressed one after another
 * @param followUps  quests that start automatically when this one is completed
 */
public record QuestDefinition(
        ContentId id,
        ContentId source,
        boolean adapted,
        Kind kind,
        boolean sequential,
        List<Objective> objectives,
        int experience,
        List<ContentId> followUps
) implements ContentDefinition {

    public enum Kind {
        MAIN, SIDE, DISCOVERY, CLASS, CHARACTER, NPC;

        /** Main quests are listed first and preferred when pinning. */
        public boolean isMain() {
            return this == MAIN;
        }
    }

    public enum ObjectiveType { TALK, COLLECT, REACH, DEFEAT, DISCOVER, CUSTOM }

    /**
     * @param target what the objective refers to: an NPC id for {@code talk}, an item, entity, structure or
     *               location id otherwise. NPC targets are validated by the content validator; Minecraft ids by the mod.
     */
    public record Objective(String key, ObjectiveType type, String target, int count) {
        public String translationKey(QuestDefinition quest) {
            return quest.translationKey() + ".objective." + key;
        }
    }

    private static final Pattern KEY = Pattern.compile("[a-z0-9_]+");

    public static QuestDefinition parse(ContentId id, JsonObjectReader json) {
        Kind kind = json.enumValue("kind", Kind.class);
        List<Objective> objectives = json.objectList("objectives", objective -> {
            String key = objective.string("key");
            if (!KEY.matcher(key).matches()) {
                throw new ContentParseException(objective.childPath("key"), "nur a-z, 0-9 und _ erlaubt");
            }
            ObjectiveType type = objective.enumValue("type", ObjectiveType.class);
            String target = objective.string("target");
            String bare = target.startsWith("#") ? target.substring(1) : target;
            if (!Modifier.MINECRAFT_ID.matcher(bare).matches()) {
                throw new ContentParseException(objective.childPath("target"), "ID 'namespace:pfad' erwartet");
            }
            return new Objective(key, type, target, objective.integerInRange("count", 1, 1, 999));
        });
        if (objectives.isEmpty()) {
            throw new ContentParseException(json.childPath("objectives"), "mindestens ein Ziel erwartet");
        }
        Set<String> keys = new HashSet<>();
        for (Objective objective : objectives) {
            if (!keys.add(objective.key())) {
                throw new ContentParseException(json.childPath("objectives"), "Ziel '" + objective.key() + "' doppelt");
            }
        }
        return new QuestDefinition(id, json.id("source"), json.bool("adapted", false), kind,
                json.bool("sequential", true), objectives,
                json.integerInRange("experience", 0, 0, 1_000_000), json.idList("follow_ups"));
    }

    public Optional<Objective> objective(String key) {
        return objectives.stream().filter(o -> o.key().equals(key)).findFirst();
    }

    public String translationKey() {
        return "quest." + id.toTranslationFragment();
    }

    @Override
    public List<String> requiredTranslationKeys() {
        List<String> keys = new ArrayList<>();
        keys.add(translationKey());
        keys.add(translationKey() + ".desc");
        for (Objective objective : objectives) {
            keys.add(objective.translationKey(this));
        }
        return keys;
    }
}
