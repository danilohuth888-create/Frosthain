package net.frosthain.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslatorTest {

    @Test
    void looksUpGermanFormatsArgumentsAndShowsMissingKeys() {
        Translator t = UiTestContent.german();
        assertEquals("Kampfhaltung", t.t("talent.frosthain.general.combat_stance"));
        assertEquals("Charakter Stufe 12", t.t("ui.frosthain.talents.character_level", 12));
        assertEquals("ui.frosthain.does_not_exist", t.t("ui.frosthain.does_not_exist"));
    }

    @Test
    void fallsBackToEnglishForUnknownLocales() {
        Translator t = new Translator(UiTestContent.registry(), "fr_fr");
        assertEquals("Combat Stance", t.t("talent.frosthain.general.combat_stance"));
    }
}
