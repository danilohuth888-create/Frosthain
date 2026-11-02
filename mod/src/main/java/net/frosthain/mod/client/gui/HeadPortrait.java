package net.frosthain.mod.client.gui;

import net.frosthain.core.ContentId;
import net.frosthain.core.character.Appearance;
import net.frosthain.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Map;

/**
 * Minecraft-style 8×8 head portrait of an {@link Appearance} (reference "Charaktererstellung – Aussehen", Vorschau):
 * skin, eyes, hair style and colour, plus species features around the head. A preview until skin compositing
 * (milestone M2.4) puts the appearance on the player model.
 */
public final class HeadPortrait {

    /** H = hair, B = darker hair (braids), S = skin; eyes and mouth are added on top. */
    private static final Map<String, String[]> HAIR = Map.of(
            "short", new String[]{"HHHHHHHH", "HHHHHHHH", "HSSSSSSH", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS"},
            "long", new String[]{"HHHHHHHH", "HHHHHHHH", "HSSSSSSH", "HSSSSSSH", "HSSSSSSH", "HSSSSSSH", "HSSSSSSH", "HSSSSSSH"},
            "braided", new String[]{"HHHHHHHH", "HHHHHHHH", "HSSSSSSH", "BSSSSSSB", "HSSSSSSH", "BSSSSSSB", "HSSSSSSH", "BSSSSSSB"},
            "ponytail", new String[]{"HHHHHHHH", "HHHHHHHH", "HHHSSSSH", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS"},
            "mohawk", new String[]{"SSSHHSSS", "SSSHHSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS"},
            "bald", new String[]{"SSSSSSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS", "SSSSSSSS"});

    private HeadPortrait() {
    }

    /** Draws the head with its top-left corner at (x, y); {@code p} is the size of one skin pixel. */
    public static void draw(GuiGraphicsExtractor g, int x, int y, int p, Appearance appearance) {
        int skin = argb(appearance.skinColor());
        int hair = argb(appearance.hairColor());
        int braid = Theme.mix(hair, 0xFF000000, 0.3);
        int shade = Theme.mix(skin, 0xFF000000, 0.25);
        String[] rows = HAIR.getOrDefault(appearance.hairStyle().path().replace("hair/", ""), HAIR.get("short"));
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int color = switch (rows[r].charAt(c)) {
                    case 'H' -> hair;
                    case 'B' -> braid;
                    default -> r == 7 ? shade : skin;
                };
                px(g, x, y, p, c, r, color);
            }
        }
        // Eyes (white + iris) and mouth, as on the default Minecraft face.
        px(g, x, y, p, 1, 4, 0xFFF4F4F4);
        px(g, x, y, p, 2, 4, argb(appearance.eyeColor()));
        px(g, x, y, p, 5, 4, argb(appearance.eyeColor()));
        px(g, x, y, p, 6, 4, 0xFFF4F4F4);
        px(g, x, y, p, 3, 6, shade);
        px(g, x, y, p, 4, 6, shade);

        appearance.features().forEach((slot, part) -> feature(g, x, y, p, slot, part, skin, shade));
        g.outline(x - 1, y - 1, 8 * p + 2, 8 * p + 2, 0x80000000);
    }

    private static void feature(GuiGraphicsExtractor g, int x, int y, int p, String slot, ContentId part, int skin, int shade) {
        String kind = part.path().substring(part.path().indexOf('/') + 1);
        int bone = 0xFFD8CFB6;
        int dark = 0xFF3A2F2F;
        switch (slot) {
            case "ears" -> {
                int length = kind.equals("pointed_long") ? 2 : 1;
                for (int i = 1; i <= length; i++) {
                    px(g, x, y, p, -i, 3 - (i - 1), skin);
                    px(g, x, y, p, 7 + i, 3 - (i - 1), skin);
                }
                px(g, x, y, p, -1, 4, kind.equals("rounded_large") ? skin : shade);
                px(g, x, y, p, 8, 4, kind.equals("rounded_large") ? skin : shade);
            }
            case "horns" -> {
                switch (kind) {
                    case "ram" -> {
                        px(g, x, y, p, 0, -1, dark);
                        px(g, x, y, p, -1, 0, dark);
                        px(g, x, y, p, -1, 1, dark);
                        px(g, x, y, p, 7, -1, dark);
                        px(g, x, y, p, 8, 0, dark);
                        px(g, x, y, p, 8, 1, dark);
                    }
                    case "straight" -> {
                        px(g, x, y, p, 1, -1, bone);
                        px(g, x, y, p, 1, -2, bone);
                        px(g, x, y, p, 6, -1, bone);
                        px(g, x, y, p, 6, -2, bone);
                    }
                    default -> {
                        for (int c : new int[]{1, 3, 4, 6}) {
                            px(g, x, y, p, c, -1, bone);
                        }
                    }
                }
            }
            case "scales" -> {
                for (int r = 3; r <= 7; r++) {
                    px(g, x, y, p, (r % 2 == 0) ? 0 : 7, r, shade);
                }
                px(g, x, y, p, 1, 5, shade);
                px(g, x, y, p, 6, 5, shade);
            }
            case "tusks" -> {
                px(g, x, y, p, 2, 7, bone);
                px(g, x, y, p, 5, 7, bone);
                if (kind.equals("large")) {
                    px(g, x, y, p, 2, 6, bone);
                    px(g, x, y, p, 5, 6, bone);
                }
            }
            case "markings" -> {
                px(g, x, y, p, 0, 5, 0xFF4A4F55);
                px(g, x, y, p, 7, 3, 0xFF4A4F55);
                px(g, x, y, p, 6, 6, 0xFF4A4F55);
            }
            default -> {
            }
        }
    }

    private static void px(GuiGraphicsExtractor g, int x, int y, int p, int col, int row, int color) {
        g.fill(x + col * p, y + row * p, x + (col + 1) * p, y + (row + 1) * p, color);
    }

    private static int argb(String hex) {
        return 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
    }
}
