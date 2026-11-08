package net.frosthain.ui.hud;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudPolicyTest {

    @Test
    void vanillaHudStaysDuringNormalPlay() {
        for (HudPolicy.Element element : HudPolicy.Element.values()) {
            assertTrue(HudPolicy.isVisible(element, HudPolicy.Mode.GAMEPLAY, HudPolicy.Settings.DEFAULT), element.name());
        }
    }

    @Test
    void dialogueHidesBarsAndTrackerAndRestoresThemAfterwards() {
        assertTrue(HudPolicy.hiddenDuringDialogue().containsAll(EnumSet.of(HudPolicy.Element.HOTBAR, HudPolicy.Element.HEALTH,
                HudPolicy.Element.FOOD, HudPolicy.Element.ARMOR, HudPolicy.Element.EXPERIENCE, HudPolicy.Element.QUEST_TRACKER)));
        assertTrue(HudPolicy.isVisible(HudPolicy.Element.HOTBAR, HudPolicy.Mode.GAMEPLAY, HudPolicy.Settings.DEFAULT));
    }

    @Test
    void playerSettingsAndF1AreRespected() {
        HudPolicy.Settings noTracker = new HudPolicy.Settings(false, false, true, true);
        assertFalse(HudPolicy.isVisible(HudPolicy.Element.QUEST_TRACKER, HudPolicy.Mode.GAMEPLAY, noTracker));
        assertFalse(HudPolicy.isVisible(HudPolicy.Element.COORDINATES, HudPolicy.Mode.GAMEPLAY, noTracker));
        assertEquals(0, EnumSet.allOf(HudPolicy.Element.class).stream()
                .filter(e -> HudPolicy.isVisible(e, HudPolicy.Mode.HIDDEN, HudPolicy.Settings.DEFAULT)).count());
    }
}
