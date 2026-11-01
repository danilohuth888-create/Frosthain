package net.frosthain.ui.text;

import net.frosthain.ui.Theme;

import java.util.ArrayList;
import java.util.List;

/** Greedy word wrapping. Words longer than the line are broken by characters, nothing is ever cut off. */
public final class TextLayout {

    private TextLayout() {
    }

    public static List<String> wrap(String text, int maxWidth, TextMeasurer measurer, Theme.FontRole role, int size) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            wrapParagraph(paragraph, maxWidth, measurer, role, size, lines);
        }
        return lines;
    }

    private static void wrapParagraph(String paragraph, int maxWidth, TextMeasurer measurer, Theme.FontRole role, int size,
                                      List<String> lines) {
        String[] words = paragraph.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (measurer.width(candidate, role, size) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
                line.setLength(0);
            }
            // The word alone may still be too long: break it by characters.
            String rest = word;
            while (measurer.width(rest, role, size) > maxWidth && rest.length() > 1) {
                int cut = rest.length() - 1;
                while (cut > 1 && measurer.width(rest.substring(0, cut), role, size) > maxWidth) {
                    cut--;
                }
                lines.add(rest.substring(0, cut));
                rest = rest.substring(cut);
            }
            line.append(rest);
        }
        lines.add(line.toString());
    }
}
