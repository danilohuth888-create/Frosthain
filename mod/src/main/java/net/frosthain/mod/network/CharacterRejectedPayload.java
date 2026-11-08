package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/** Server → client: why a creation request was rejected (shown in the editor). */
public record CharacterRejectedPayload(List<String> problems) implements CustomPacketPayload {

    public static final Type<CharacterRejectedPayload> TYPE = new Type<>(FrosthainMod.id("character_rejected"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterRejectedPayload> CODEC =
            CustomPacketPayload.codec(CharacterRejectedPayload::write, CharacterRejectedPayload::read);

    private static final int MAX_PROBLEMS = 16;

    public CharacterRejectedPayload {
        problems = List.copyOf(problems.subList(0, Math.min(problems.size(), MAX_PROBLEMS)));
    }

    private static CharacterRejectedPayload read(RegistryFriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), MAX_PROBLEMS);
        List<String> problems = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            problems.add(buf.readUtf());
        }
        return new CharacterRejectedPayload(problems);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(problems.size());
        problems.forEach(buf::writeUtf);
    }

    @Override
    public Type<CharacterRejectedPayload> type() {
        return TYPE;
    }
}
