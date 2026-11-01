package net.frosthain.mod.client.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Talent icons (content ids like {@code frosthain:icon/swords}) shown as vanilla items until FROSTHAIN has its
 * own icon textures – keeps the screen recognisably Minecraft (Design 2.0).
 */
public final class TalentIcons {

    private static final Map<String, String> ITEMS = Map.ofEntries(
            Map.entry("swords", "iron_sword"), Map.entry("sword", "golden_sword"), Map.entry("bolt", "lightning_rod"),
            Map.entry("flame", "blaze_powder"), Map.entry("shield", "shield"), Map.entry("mountain", "cobblestone"),
            Map.entry("horn", "goat_horn"), Map.entry("eye", "ender_eye"), Map.entry("tree", "oak_sapling"),
            Map.entry("heart", "golden_apple"), Map.entry("boot", "leather_boots"), Map.entry("rune", "amethyst_shard"),
            Map.entry("star", "nether_star"), Map.entry("drop", "honey_bottle"), Map.entry("crystal", "prismarine_crystals"),
            Map.entry("pickaxe", "iron_pickaxe"), Map.entry("hammer", "anvil"), Map.entry("gem", "emerald"),
            Map.entry("hourglass", "clock"), Map.entry("crown", "golden_helmet"), Map.entry("leaf", "oak_leaves"),
            Map.entry("wave", "nautilus_shell"), Map.entry("paw", "bone"), Map.entry("book", "book"),
            Map.entry("circle", "ender_pearl"), Map.entry("snowflake", "snowball"));

    private TalentIcons() {
    }

    /** {@code icon} is the last path segment ("swords") or a full id; unknown icons fall back to a book. */
    public static ItemStack item(String icon) {
        String name = icon == null ? "" : icon.substring(icon.lastIndexOf('/') + 1);
        String item = ITEMS.getOrDefault(name, "book");
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(item)));
    }
}
