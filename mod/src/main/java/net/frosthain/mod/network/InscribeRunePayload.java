package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: inscribe this rune at the open altar (materials and knowledge are checked on the server). */
public record InscribeRunePayload(String rune) implements CustomPacketPayload {

    public static final Type<InscribeRunePayload> TYPE = new Type<>(FrosthainMod.id("inscribe_rune"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InscribeRunePayload> CODEC =
            CustomPacketPayload.codec(InscribeRunePayload::write, InscribeRunePayload::read);

    private static InscribeRunePayload read(RegistryFriendlyByteBuf buf) {
        return new InscribeRunePayload(buf.readUtf(128));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(rune);
    }

    @Override
    public Type<InscribeRunePayload> type() {
        return TYPE;
    }
}
