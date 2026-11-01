package net.frosthain.ui.text;

import net.frosthain.ui.Theme;
import net.frosthain.ui.UiTestContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextLayoutTest {

    @Test
    void wrapsWithinWidthWithoutLosingText() {
        String text = "Endlich bist du da. Die Zeichen haben sich verdichtet … Frosthain erwacht erneut.";
        List<String> lines = TextLayout.wrap(text, 160, UiTestContent.MONO, Theme.FontRole.BODY, 12);
        assertTrue(lines.size() > 1);
        lines.forEach(line -> assertTrue(line.length() * 8 <= 160, line));
        assertEquals(text, String.join(" ", lines));
    }

    @Test
    void breaksWordsLongerThanALine() {
        List<String> lines = TextLayout.wrap("Donaudampfschifffahrtsgesellschaft", 80, UiTestContent.MONO, Theme.FontRole.BODY, 12);
        lines.forEach(line -> assertTrue(line.length() <= 10, line));
        assertEquals("Donaudampfschifffahrtsgesellschaft", String.join("", lines));
    }

    @Test
    void keepsExplicitParagraphs() {
        assertEquals(List.of("A", "B"), TextLayout.wrap("A\nB", 100, UiTestContent.MONO, Theme.FontRole.BODY, 12));
    }
}
