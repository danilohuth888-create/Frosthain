package net.frosthain.ui.dialogue;

import net.frosthain.core.content.DialogueDefinition;
import net.frosthain.core.content.NpcDefinition;
import net.frosthain.core.dialogue.DialogueSession;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import net.frosthain.ui.Translator;

import java.util.ArrayList;
import java.util.List;

/**
 * Cinematic dialogue panel state (UI spec F): speaker, line, answers and the keyboard/mouse selection.
 * Wraps a {@link DialogueSession}; the renderer only draws {@link #speakerName()}, {@link #line()} and
 * {@link #choices()} and forwards input.
 */
public final class DialogueView {

    public record ChoiceView(int index, String text, boolean selected) {
    }

    private final DialogueSession session;
    private final Translator translator;
    private final String speakerName;
    private final String speakerTitle;
    private int selected;

    public DialogueView(ContentRegistry registry, DialogueSession session, Translator translator) {
        this.session = session;
        this.translator = translator;
        DialogueDefinition dialogue = session.dialogue();
        NpcDefinition npc = registry.get(ContentType.NPCS, dialogue.speaker()).orElse(null);
        this.speakerName = npc == null ? dialogue.speaker().toString() : translator.t(npc.translationKey());
        this.speakerTitle = npc == null ? "" : translator.t(npc.translationKey() + ".title");
    }

    public String speakerName() {
        return speakerName;
    }

    public String speakerTitle() {
        return speakerTitle;
    }

    public DialogueSession session() {
        return session;
    }

    public boolean isOpen() {
        return session.state() == DialogueSession.State.ACTIVE;
    }

    public boolean isInterrupted() {
        return session.state() == DialogueSession.State.INTERRUPTED;
    }

    public String line() {
        if (isInterrupted()) {
            return translator.t("ui.frosthain.dialogue.interrupted");
        }
        return translator.t(session.lineKey());
    }

    /** Answers as shown; an end line offers a single "Weiter" (continue). */
    public List<ChoiceView> choices() {
        List<ChoiceView> views = new ArrayList<>();
        if (session.isAtEndLine()) {
            views.add(new ChoiceView(0, translator.t("ui.frosthain.dialogue.continue"), true));
            return views;
        }
        List<DialogueDefinition.Choice> visible = session.visibleChoices();
        for (int i = 0; i < visible.size(); i++) {
            String key = session.dialogue().choiceKey(session.currentNode(), visible.get(i).key());
            views.add(new ChoiceView(i, translator.t(key), i == selected));
        }
        return views;
    }

    public int selected() {
        return selected;
    }

    public void moveSelection(int delta) {
        int count = choices().size();
        if (count > 0) {
            selected = Math.floorMod(selected + delta, count);
        }
    }

    public void select(int index) {
        if (index >= 0 && index < choices().size()) {
            selected = index;
        }
    }

    /** Confirms the selected answer (Enter, click). Returns true while the dialogue stays open. */
    public boolean confirm() {
        if (!isOpen()) {
            return false;
        }
        if (session.isAtEndLine()) {
            session.acknowledgeEnd();
        } else {
            session.choose(selected);
        }
        selected = 0;
        return isOpen();
    }

    /** Number keys 1–9 pick an answer directly. */
    public boolean chooseNumber(int number) {
        if (number < 1 || number > choices().size()) {
            return isOpen();
        }
        selected = number - 1;
        return confirm();
    }

    /** UI spec F: the world does not pause; damage interrupts the conversation. */
    public void onPlayerDamaged() {
        session.interrupt("damage");
    }
}
