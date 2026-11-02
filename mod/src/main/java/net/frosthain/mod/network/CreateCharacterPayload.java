package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: the choices of the character editor. {@code appearanceJson} uses the appearance part of the
 * {@code CharacterSheetCodec} format (empty = species default). The server validates everything with the core rules.
 */
public record CreateCharacterPayload(String name, String species, String characterClass, String background, String appearanceJson)
        implements CustomPacketPayload {

    public static final Type<CreateCharacterPayload> TYPE = new Type<>(FrosthainMod.id("create_character"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreateCharacterPayload> CODEC =
            CustomPacketPayload.codec(CreateCharacterPayload::write, CreateCharacterPayload::read);

    private static final int MAX_LENGTH = 128;
    private static final int MAX_APPEARANCE_LENGTH = 4096;

    private static CreateCharacterPayload read(RegistryFriendlyByteBuf buf) {
        return new CreateCharacterPayload(buf.readUtf(MAX_LENGTH), buf.readUtf(MAX_LENGTH), buf.readUtf(MAX_LENGTH), buf.readUtf(MAX_LENGTH),
                buf.readUtf(MAX_APPEARANCE_LENGTH));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeUtf(species);
        buf.writeUtf(characterClass);
        buf.writeUtf(background);
        buf.writeUtf(appearanceJson);
    }

    @Override
    public Type<CreateCharacterPayload> type() {
        return TYPE;
    }
}
