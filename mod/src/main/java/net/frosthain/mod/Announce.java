package net.frosthain.mod;

import net.frosthain.ui.Theme;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Large centred announcements (level-up, boss) through the vanilla title display, styled with the FROSTHAIN fonts.
 * Unlike overlay messages they are not hidden behind the chat.
 */
public final class Announce {

    private static final Style HEADING = Style.EMPTY.withFont(new FontDescription.Resource(FrosthainMod.id("heading")))
            .withColor(Theme.FRAME_GOLD_BRIGHT & 0xFFFFFF);
    private static final Style BODY = Style.EMPTY.withFont(new FontDescription.Resource(FrosthainMod.id("body")))
            .withColor(Theme.TEXT_PRIMARY & 0xFFFFFF);

    private Announce() {
    }

    public static MutableComponent heading(Component text) {
        return text.copy().withStyle(HEADING);
    }

    public static MutableComponent body(Component text) {
        return text.copy().withStyle(BODY);
    }

    /** Title (may be empty) with subtitle; {@code stay} in ticks. */
    public static void show(ServerPlayer player, Component title, Component subtitle, int stay) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, stay, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** Only a subtitle line in the middle of the screen (boss calls). */
    public static void line(ServerPlayer player, Component text) {
        show(player, Component.empty(), body(text), 50);
    }
}
