package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A dialogue graph. Every node is one line of the speaker; choices lead to other nodes or end the dialogue.
 * Choices can depend on conditions and trigger effects (start a quest, progress an objective, grant experience).
 *
 * <p>Texts are never stored in the JSON, only derived language keys:
 * {@code dialogue.<id>.<node>} for the speaker line and {@code dialogue.<id>.<node>.choice.<choice>} for answers.
 */
public record DialogueDefinition(
        ContentId id,
        ContentId source,
        boolean adapted,
        ContentId speaker,
        String start,
        Map<String, Node> nodes
) implements ContentDefinition {

    public record Node(String key, List<Choice> choices) {
        /** A node without choices ends the dialogue after the line was read. */
        public boolean isEnd() {
            return choices.isEmpty();
        }
    }

    /**
     * @param next node to continue with; empty = the choice ends the dialogue
     * @param once if true, the choice disappears after it was taken in this conversation
     */
    public record Choice(String key, Optional<String> next, List<Condition> conditions, List<Effect> effects, boolean once) {
    }

    public enum QuestState { NOT_STARTED, ACTIVE, COMPLETED }

    public sealed interface Condition {
        record QuestStateIs(ContentId quest, QuestState state) implements Condition {
        }
    }

    public sealed interface Effect {
        record StartQuest(ContentId quest) implements Effect {
        }

        record AdvanceObjective(ContentId quest, String objective, int amount) implements Effect {
        }

        record GrantExperience(int amount) implements Effect {
        }
    }

    private static final Pattern KEY = Pattern.compile("[a-z0-9_]+");

    public static DialogueDefinition parse(ContentId id, JsonObjectReader json) {
        ContentId speaker = json.id("speaker");
        String start = json.string("start");
        Map<String, Node> nodes = new LinkedHashMap<>();
        List<Node> parsed = json.objectList("nodes", DialogueDefinition::parseNode);
        for (Node node : parsed) {
            if (nodes.putIfAbsent(node.key(), node) != null) {
                throw new ContentParseException(json.childPath("nodes"), "Knoten '" + node.key() + "' doppelt");
            }
        }
        if (!nodes.containsKey(start)) {
            throw new ContentParseException(json.childPath("start"), "Startknoten '" + start + "' existiert nicht");
        }
        for (Node node : nodes.values()) {
            for (Choice choice : node.choices()) {
                if (choice.next().isPresent() && !nodes.containsKey(choice.next().get())) {
                    throw new ContentParseException(json.childPath("nodes"),
                            "Antwort '" + node.key() + "." + choice.key() + "' verweist auf unbekannten Knoten '" + choice.next().get() + "'");
                }
            }
        }
        return new DialogueDefinition(id, json.id("source"), json.bool("adapted", false), speaker, start,
                Collections.unmodifiableMap(nodes));
    }

    private static Node parseNode(JsonObjectReader json) {
        String key = checkKey(json, "key");
        List<Choice> choices = json.objectList("choices", choice -> new Choice(
                checkKey(choice, "key"),
                choice.optString("next"),
                choice.objectList("conditions", DialogueDefinition::parseCondition),
                choice.objectList("effects", DialogueDefinition::parseEffect),
                choice.bool("once", false)));
        List<String> seen = new ArrayList<>();
        for (Choice choice : choices) {
            if (seen.contains(choice.key())) {
                throw new ContentParseException(json.childPath("choices"), "Antwort '" + choice.key() + "' doppelt");
            }
            seen.add(choice.key());
        }
        return new Node(key, choices);
    }

    private static Condition parseCondition(JsonObjectReader json) {
        String type = json.string("type");
        if (type.equals("quest_state")) {
            return new Condition.QuestStateIs(json.id("quest"), json.enumValue("state", QuestState.class));
        }
        throw new ContentParseException(json.childPath("type"), "Unbekannte Bedingung '" + type + "', erlaubt: [quest_state]");
    }

    private static Effect parseEffect(JsonObjectReader json) {
        String type = json.string("type");
        return switch (type) {
            case "start_quest" -> new Effect.StartQuest(json.id("quest"));
            case "advance_objective" -> new Effect.AdvanceObjective(json.id("quest"), json.string("objective"),
                    json.integerInRange("amount", 1, 1, 999));
            case "experience" -> new Effect.GrantExperience(json.integerInRange("amount", 0, 1, 1_000_000));
            default -> throw new ContentParseException(json.childPath("type"),
                    "Unbekannte Folge '" + type + "', erlaubt: [start_quest, advance_objective, experience]");
        };
    }

    private static String checkKey(JsonObjectReader json, String field) {
        String key = json.string(field);
        if (!KEY.matcher(key).matches()) {
            throw new ContentParseException(json.childPath(field), "nur a-z, 0-9 und _ erlaubt");
        }
        return key;
    }

    public String translationKey() {
        return "dialogue." + id.toTranslationFragment();
    }

    public String lineKey(String node) {
        return translationKey() + "." + node;
    }

    public String choiceKey(String node, String choice) {
        return translationKey() + "." + node + ".choice." + choice;
    }

    @Override
    public List<String> requiredTranslationKeys() {
        List<String> keys = new ArrayList<>();
        for (Node node : nodes.values()) {
            keys.add(lineKey(node.key()));
            for (Choice choice : node.choices()) {
                keys.add(choiceKey(node.key(), choice.key()));
            }
        }
        return keys;
    }
}
