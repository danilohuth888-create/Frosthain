package net.frosthain.ui.input;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyBindingPlanTest {

    @Test
    void defaultBindingsDoNotOverrideVanillaKeys() {
        assertEquals(List.of(), KeyBindingPlan.conflicts(KeyBindingPlan.defaults(KeyBindingPlan.InteractionMode.USE_KEY)));
    }

    @Test
    void eForTalkingConflictsWithTheInventory() {
        assertEquals(List.of(new KeyBindingPlan.Conflict(KeyBindingPlan.Action.TALK_TO_NPC, "key.keyboard.e", "key.inventory")),
                KeyBindingPlan.conflicts(KeyBindingPlan.defaults(KeyBindingPlan.InteractionMode.CONTEXT_E)));
    }
}
