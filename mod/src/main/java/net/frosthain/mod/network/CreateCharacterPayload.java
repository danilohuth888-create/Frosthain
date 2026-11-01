package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the choices of the character editor. The server validates them with the core rules. */
public record CreateCharacterPayload(String name, String species, String characterClass, String background)
        implements CustomPacketPayload {

    public static final Type<CreateCharacterPayload> TYPE = new Type<>(FrosthainMod.id("create_character"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreateCharacterPayload> CODEC =
            CustomPacketPayload.codec(CreateCharacterPayload::write, CreateCharacterPayload::read);

    private static final int MAX_LENGTH = 128;

    private static CreateCharacterPayload read(RegistryFriendlyByteBuf buf) {
        return new CreateCharacterPayload(buf.readUtf(MAX_LENGTH), buf.readUtf(MAX_LENGTH), buf.readUtf(MAX_LENGTH), buf.readUtf(MAX_LENGTH));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeUtf(species);
        buf.writeUtf(characterClass);
        buf.writeUtf(background);
    }

    @Override
    public Type<CreateCharacterPayload> type() {
        return TYPE;
    }
}
