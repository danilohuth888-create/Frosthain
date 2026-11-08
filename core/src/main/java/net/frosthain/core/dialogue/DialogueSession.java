package net.frosthain.core.dialogue;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.DialogueDefinition;
import net.frosthain.core.content.DialogueDefinition.Choice;
import net.frosthain.core.content.DialogueDefinition.Condition;
import net.frosthain.core.content.DialogueDefinition.Effect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Running conversation with an NPC. The session never pauses the world (UI spec F); it can be interrupted
 * (e.g. when the player takes damage) and resumed later at the same line.
 *
 * <p>The session is game-agnostic: quest state and effects go through {@link Context}, implemented by the
 * mod on the server and by the prototype on the desktop.
 */
public final class DialogueSession {

    /** Access to the world state the dialogue depends on. */
    public interface Context {
        DialogueDefinition.QuestState questState(ContentId quest);

        void apply(Effect effect);
    }

    public enum State { ACTIVE, INTERRUPTED, FINISHED }

    private final DialogueDefinition dialogue;
    private final Context context;
    private final Set<String> takenOnceChoices = new HashSet<>();
    private String node;
    private State state = State.ACTIVE;
    private Optional<String> interruptReason = Optional.empty();

    public DialogueSession(DialogueDefinition dialogue, Context context) {
        this.dialogue = dialogue;
        this.context = context;
        this.node = dialogue.start();
    }

    public DialogueDefinition dialogue() {
        return dialogue;
    }

    public State state() {
        return state;
    }

    public String currentNode() {
        return node;
    }

    /** Language key of the current speaker line. */
    public String lineKey() {
        return dialogue.lineKey(node);
    }

    public Optional<String> interruptReason() {
        return interruptReason;
    }

    /** Choices whose conditions are met and that were not already taken (for {@code once} choices). */
    public List<Choice> visibleChoices() {
        if (state != State.ACTIVE) {
            return List.of();
        }
        List<Choice> visible = new ArrayList<>();
        for (Choice choice : dialogue.nodes().get(node).choices()) {
            if (choice.once() && takenOnceChoices.contains(node + "." + choice.key())) {
                continue;
            }
            if (choice.conditions().stream().allMatch(this::isMet)) {
                visible.add(choice);
            }
        }
        return visible;
    }

    private boolean isMet(Condition condition) {
        return switch (condition) {
            case Condition.QuestStateIs(ContentId quest, DialogueDefinition.QuestState expected) ->
                    context.questState(quest) == expected;
        };
    }

    /** Takes the visible choice at {@code index}, applies its effects and moves on. */
    public void choose(int index) {
        if (state != State.ACTIVE) {
            throw new IllegalStateException("Dialog ist nicht aktiv: " + state);
        }
        List<Choice> visible = visibleChoices();
        if (index < 0 || index >= visible.size()) {
            throw new IndexOutOfBoundsException("Antwort " + index + " von " + visible.size());
        }
        Choice choice = visible.get(index);
        if (choice.once()) {
            takenOnceChoices.add(node + "." + choice.key());
        }
        choice.effects().forEach(context::apply);
        if (choice.next().isPresent()) {
            node = choice.next().get();
        } else {
            state = State.FINISHED;
        }
    }

    /** Closes the dialogue after an end line (a node without choices) was read. */
    public void acknowledgeEnd() {
        if (state == State.ACTIVE && dialogue.nodes().get(node).isEnd()) {
            state = State.FINISHED;
        }
    }

    public boolean isAtEndLine() {
        return state == State.ACTIVE && dialogue.nodes().get(node).isEnd();
    }

    /** Stops the conversation because the real-time world requires attention (e.g. damage). */
    public void interrupt(String reason) {
        if (state == State.ACTIVE) {
            state = State.INTERRUPTED;
            interruptReason = Optional.of(reason);
        }
    }

    /** Continues an interrupted conversation at the same line. */
    public void resume() {
        if (state == State.INTERRUPTED) {
            state = State.ACTIVE;
            interruptReason = Optional.empty();
        }
    }
}
