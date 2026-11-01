package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client: the player's character (or that none exists yet, which opens the editor). */
public record CharacterStatePayload(boolean present, String name, String species, String characterClass,
                                    String background, int level) implements CustomPacketPayload {

    public static final Type<CharacterStatePayload> TYPE = new Type<>(FrosthainMod.id("character_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterStatePayload> CODEC =
            CustomPacketPayload.codec(CharacterStatePayload::write, CharacterStatePayload::read);

    public static CharacterStatePayload absent() {
        return new CharacterStatePayload(false, "", "", "", "", 0);
    }

    private static CharacterStatePayload read(RegistryFriendlyByteBuf buf) {
        return new CharacterStatePayload(buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(present);
        buf.writeUtf(name);
        buf.writeUtf(species);
        buf.writeUtf(characterClass);
        buf.writeUtf(background);
        buf.writeVarInt(level);
    }

    @Override
    public Type<CharacterStatePayload> type() {
        return TYPE;
    }
}
