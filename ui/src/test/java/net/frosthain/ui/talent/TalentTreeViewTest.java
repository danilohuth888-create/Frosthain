package net.frosthain.ui.talent;

import net.frosthain.core.ContentId;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.TalentRef;
import net.frosthain.core.progression.TalentAllocator;
import net.frosthain.core.content.TalentTreeDefinition;
import net.frosthain.ui.Theme;
import net.frosthain.ui.UiTestContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentTreeViewTest {

    private static final ContentId GENERAL = ContentId.of("general");
    private final TalentAllocator allocator = new TalentAllocator(UiTestContent.registry());

    private TalentTreeView view(CharacterSheet sheet, ContentId tree) {
        return new TalentTreeView(UiTestContent.registry(), allocator, UiTestContent.german(), sheet, tree);
    }

    private static NodeState state(TalentTreeView view, String key) {
        return view.node(key).orElseThrow().state();
    }

    @Test
    void tabsFollowReferenceImageOrderAndColors() {
        TalentTreeView view = view(UiTestContent.character("fighter"), GENERAL);
        assertEquals(List.of("Kampf", "Magie", "Handwerk", "Überleben"), view.tabs().stream().map(TalentTreeView.Tab::name).toList());
        assertEquals(List.of(Theme.BRANCH_COMBAT, Theme.BRANCH_MAGIC, Theme.BRANCH_CRAFTING, Theme.BRANCH_SURVIVAL),
                view.tabs().stream().map(TalentTreeView.Tab::color).toList());
    }

    @Test
    void detailPanelMatchesReferenceImage() {
        TalentTreeView.Detail detail = view(UiTestContent.character("fighter"), GENERAL).detail("combat_stance");
        assertEquals("Kampfhaltung", detail.name());
        assertEquals("Passives Talent", detail.kindLabel());
        assertEquals("Erhöht deinen Nahkampfschaden um 10 %.", detail.description());
        assertEquals("Freischaltbar", detail.statusText());
        assertEquals(Theme.STATE_AVAILABLE, detail.statusColor());
        assertEquals("Kosten: 1 Talentpunkt", detail.costText());
        assertTrue(detail.canUnlock());
        assertFalse(detail.canRefund());
    }

    @Test
    void statesReflectRules() {
        CharacterSheet sheet = UiTestContent.character("fighter");
        TalentTreeView before = view(sheet, GENERAL);
        assertEquals(NodeState.ORIGIN, state(before, "awakening"));
        assertEquals(NodeState.AVAILABLE, state(before, "combat_stance"));
        assertEquals(NodeState.LOCKED, state(before, "toughness"));

        CharacterSheet after = allocator.allocate(sheet, new TalentRef(GENERAL, "combat_stance"));
        TalentTreeView view = view(after, GENERAL);
        assertEquals(NodeState.MAXED, state(view, "combat_stance"));
        // Prerequisite met, but no points left: reachable, not locked.
        assertEquals(NodeState.REACHABLE, state(view, "toughness"));
        assertEquals(0, view.availablePoints());
        assertTrue(view.edges().stream().anyMatch(e -> e.from().equals("awakening") && e.to().equals("combat_stance") && e.active()));
    }

    @Test
    void lockedNodesExplainWhyInPlainGerman() {
        TalentTreeView.Detail detail = view(UiTestContent.character("fighter"), GENERAL).detail("stalwart");
        assertEquals("Gesperrt", detail.statusText());
        assertTrue(detail.reasons().contains("Benötigt Charakterstufe 5"), detail.reasons()::toString);
        assertTrue(detail.reasons().contains("Benötigt: Waffenübung"), detail.reasons()::toString);
        assertTrue(detail.requirements().contains(new TalentTreeView.Requirement("Waffenübung", false)));
        assertTrue(detail.requirements().contains(new TalentTreeView.Requirement("Charakterstufe 5", false)));
    }

    @Test
    void pathListShowsGeneralRuneAndClassTreesWithPreview() {
        List<TalentTreeView.PathEntry> fighterPaths = TalentTreeView.paths(UiTestContent.registry(), allocator,
                UiTestContent.german(), UiTestContent.character("fighter"));
        assertEquals(List.of("Pfad der Legende", "Runenpfad", "Weg des Kriegers"),
                fighterPaths.stream().map(TalentTreeView.PathEntry::name).toList());
        assertEquals(TalentTreeDefinition.Category.RUNE, fighterPaths.get(1).category());

        CharacterSheet wizard = UiTestContent.character("wizard");
        TalentTreeView preview = view(wizard, ContentId.of("fighter"));
        assertFalse(preview.accessible());
        assertEquals(NodeState.LOCKED, state(preview, "weapon_master"));
        assertTrue(preview.detail("weapon_focus").reasons().stream().anyMatch(r -> r.contains("Klasse, die du nicht hast")));
    }

    @Test
    void branchCenterPointsIntoTheRightQuadrant() {
        TalentTreeView view = view(UiTestContent.character("fighter"), GENERAL);
        double[] combat = view.branchCenter("combat");
        double[] survival = view.branchCenter("survival");
        assertTrue(combat[0] < 0 && combat[1] < 0, "Kampf oben links");
        assertTrue(survival[0] < 0 && survival[1] > 0, "Überleben unten links");
    }
}
