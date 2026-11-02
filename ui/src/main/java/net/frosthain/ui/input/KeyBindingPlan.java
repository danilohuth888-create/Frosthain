package net.frosthain.ui.input;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default key bindings of FROSTHAIN and the check against Minecraft's vanilla defaults (UI specs B and F:
 * configurable, conflicts checked). Key names use Minecraft's notation ({@code key.keyboard.k}).
 *
 * <p>The vanilla list reflects the defaults up to Minecraft 1.21. In the game the mod additionally checks
 * against all bindings registered at runtime (including other mods).
 */
public final class KeyBindingPlan {

    public enum Action { TALK_TO_NPC, OPEN_TALENTS, OPEN_JOURNAL, TOGGLE_QUEST_TRACKER, OPEN_CHARACTER }

    /** How NPCs are addressed. USE_KEY is the default because E is the vanilla inventory key. */
    public enum InteractionMode { USE_KEY, CONTEXT_E }

    public record Binding(Action action, String key, boolean contextual) {
    }

    public record Conflict(Action action, String key, String vanillaAction) {
    }

    public static final Map<String, String> VANILLA_DEFAULTS;

    static {
        Map<String, String> keys = new LinkedHashMap<>();
        keys.put("key.attack", "key.mouse.left");
        keys.put("key.use", "key.mouse.right");
        keys.put("key.pickItem", "key.mouse.middle");
        keys.put("key.forward", "key.keyboard.w");
        keys.put("key.left", "key.keyboard.a");
        keys.put("key.back", "key.keyboard.s");
        keys.put("key.right", "key.keyboard.d");
        keys.put("key.jump", "key.keyboard.space");
        keys.put("key.sneak", "key.keyboard.left.shift");
        keys.put("key.sprint", "key.keyboard.left.control");
        keys.put("key.drop", "key.keyboard.q");
        keys.put("key.inventory", "key.keyboard.e");
        keys.put("key.swapOffhand", "key.keyboard.f");
        keys.put("key.chat", "key.keyboard.t");
        keys.put("key.command", "key.keyboard.slash");
        keys.put("key.playerlist", "key.keyboard.tab");
        keys.put("key.socialInteractions", "key.keyboard.p");
        keys.put("key.advancements", "key.keyboard.l");
        keys.put("key.saveToolbarActivator", "key.keyboard.c");
        keys.put("key.loadToolbarActivator", "key.keyboard.x");
        keys.put("key.screenshot", "key.keyboard.f2");
        keys.put("key.togglePerspective", "key.keyboard.f5");
        keys.put("key.fullscreen", "key.keyboard.f11");
        for (int i = 1; i <= 9; i++) {
            keys.put("key.hotbar." + i, "key.keyboard." + i);
        }
        // Hard-wired keys that cannot be rebound in vanilla.
        keys.put("hardwired.hideGui", "key.keyboard.f1");
        keys.put("hardwired.debug", "key.keyboard.f3");
        keys.put("hardwired.pause", "key.keyboard.escape");
        VANILLA_DEFAULTS = Map.copyOf(keys);
    }

    private KeyBindingPlan() {
    }

    public static List<Binding> defaults(InteractionMode mode) {
        List<Binding> bindings = new ArrayList<>();
        bindings.add(mode == InteractionMode.CONTEXT_E
                ? new Binding(Action.TALK_TO_NPC, "key.keyboard.e", true)
                // Uses the vanilla "use" binding; no extra key is registered.
                : new Binding(Action.TALK_TO_NPC, "key.mouse.right", true));
        bindings.add(new Binding(Action.OPEN_TALENTS, "key.keyboard.k", false));
        bindings.add(new Binding(Action.OPEN_JOURNAL, "key.keyboard.j", false));
        bindings.add(new Binding(Action.TOGGLE_QUEST_TRACKER, "key.keyboard.h", false));
        bindings.add(new Binding(Action.OPEN_CHARACTER, "key.keyboard.i", false));
        return List.copyOf(bindings);
    }

    /**
     * Conflicts with vanilla bindings. Talking via the use key is not a conflict: it is the vanilla interaction
     * and only triggers on NPCs. Any other shared key is reported.
     */
    public static List<Conflict> conflicts(List<Binding> bindings) {
        List<Conflict> conflicts = new ArrayList<>();
        for (Binding binding : bindings) {
            for (Map.Entry<String, String> vanilla : VANILLA_DEFAULTS.entrySet()) {
                if (!vanilla.getValue().equals(binding.key())) {
                    continue;
                }
                boolean sameMeaning = binding.action() == Action.TALK_TO_NPC && vanilla.getKey().equals("key.use");
                if (!sameMeaning) {
                    conflicts.add(new Conflict(binding.action(), binding.key(), vanilla.getKey()));
                }
            }
        }
        return conflicts;
    }
}
