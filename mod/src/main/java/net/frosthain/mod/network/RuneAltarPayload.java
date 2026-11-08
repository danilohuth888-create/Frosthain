package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client: open the rune altar screen (the server remembers where the altar is). */
public record RuneAltarPayload(boolean open) implements CustomPacketPayload {

    public static final Type<RuneAltarPayload> TYPE = new Type<>(FrosthainMod.id("rune_altar"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuneAltarPayload> CODEC =
            CustomPacketPayload.codec(RuneAltarPayload::write, RuneAltarPayload::read);

    private static RuneAltarPayload read(RegistryFriendlyByteBuf buf) {
        return new RuneAltarPayload(buf.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(open);
    }

    @Override
    public Type<RuneAltarPayload> type() {
        return TYPE;
    }
}
