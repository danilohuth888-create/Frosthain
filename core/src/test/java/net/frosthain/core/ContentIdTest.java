package net.frosthain.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentIdTest {

    @Test
    void parsesNamespacedAndDefaultIds() {
        assertEquals(new ContentId("frosthain", "fighter"), ContentId.parse("frosthain:fighter"));
        assertEquals(new ContentId("frosthain", "fighter/champion"), ContentId.parse("fighter/champion"));
        assertEquals(new ContentId("my_pack", "necromancer"), ContentId.parse("my_pack:necromancer"));
    }

    @Test
    void rejectsInvalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> ContentId.parse("Frosthain:fighter"));
        assertThrows(IllegalArgumentException.class, () -> ContentId.parse("frosthain:Krieger"));
        assertThrows(IllegalArgumentException.class, () -> ContentId.parse("frosthain:a:b"));
        assertThrows(IllegalArgumentException.class, () -> ContentId.parse("frosthain:"));
        assertThrows(IllegalArgumentException.class, () -> ContentId.parse("frosthain:a//b"));
    }

    @Test
    void derivesTranslationFragmentsAndChildren() {
        ContentId elf = ContentId.parse("frosthain:elf");
        assertEquals("frosthain:elf/high_elf", elf.child("high_elf").toString());
        assertEquals("frosthain.fighter.champion", ContentId.parse("fighter/champion").toTranslationFragment());
    }
}
