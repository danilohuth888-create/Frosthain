package net.frosthain.ui.hud;

import java.util.EnumSet;
import java.util.Set;

/**
 * Which HUD elements are visible in which situation (UI specs B and F). The vanilla HUD stays during normal
 * play; during dialogues hotbar, health, food, armor, air and experience are hidden and restored afterwards.
 */
public final class HudPolicy {

    public enum Element {
        HOTBAR, HEALTH, FOOD, ARMOR, AIR, EXPERIENCE, CROSSHAIR,
        MINIMAP, COORDINATES, QUEST_TRACKER, INTERACTION_HINT, CHAPTER_BANNER, NOTIFICATIONS
    }

    public enum Mode {
        /** Normal play. */
        GAMEPLAY,
        /** NPC dialogue open. */
        DIALOGUE,
        /** A FROSTHAIN menu (talents, journal) is open. */
        MENU,
        /** F1 – the player hid the GUI. */
        HIDDEN,
        /** F3 debug screen open. */
        DEBUG
    }

    /** Player settings that affect the HUD. */
    public record Settings(boolean showCoordinates, boolean showQuestTracker, boolean showNotifications, boolean showMinimap) {
        public static final Settings DEFAULT = new Settings(true, true, true, true);
    }

    private static final Set<Element> VANILLA_BARS = EnumSet.of(
            Element.HOTBAR, Element.HEALTH, Element.FOOD, Element.ARMOR, Element.AIR, Element.EXPERIENCE);

    private HudPolicy() {
    }

    public static boolean isVisible(Element element, Mode mode, Settings settings) {
        return switch (mode) {
            case HIDDEN -> false;
            case DIALOGUE -> false;
            case MENU -> VANILLA_BARS.contains(element);
            case DEBUG -> VANILLA_BARS.contains(element) || element == Element.CROSSHAIR;
            case GAMEPLAY -> switch (element) {
                case COORDINATES -> settings.showMinimap() && settings.showCoordinates();
                case MINIMAP -> settings.showMinimap();
                case QUEST_TRACKER -> settings.showQuestTracker();
                case NOTIFICATIONS -> settings.showNotifications();
                default -> true;
            };
        };
    }

    /** Elements that a dialogue hides and must restore afterwards. */
    public static Set<Element> hiddenDuringDialogue() {
        Set<Element> hidden = EnumSet.noneOf(Element.class);
        for (Element element : Element.values()) {
            if (isVisible(element, Mode.GAMEPLAY, Settings.DEFAULT) && !isVisible(element, Mode.DIALOGUE, Settings.DEFAULT)) {
                hidden.add(element);
            }
        }
        return hidden;
    }
}
