package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: use the power in the given ability bar slot (0-based). */
public record UsePowerPayload(int slot) implements CustomPacketPayload {

    public static final Type<UsePowerPayload> TYPE = new Type<>(FrosthainMod.id("use_power"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UsePowerPayload> CODEC =
            CustomPacketPayload.codec(UsePowerPayload::write, UsePowerPayload::read);

    private static UsePowerPayload read(RegistryFriendlyByteBuf buf) {
        return new UsePowerPayload(buf.readVarInt());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(slot);
    }

    @Override
    public Type<UsePowerPayload> type() {
        return TYPE;
    }
}
