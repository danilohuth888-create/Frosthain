package net.frosthain.ui.text;

import net.frosthain.ui.Theme;

/** Measures text width in pixels; implemented with Java2D font metrics or Minecraft's font renderer. */
@FunctionalInterface
public interface TextMeasurer {
    int width(String text, Theme.FontRole role, int size);
}
