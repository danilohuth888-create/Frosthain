package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the chosen answer (index into the visible answers), or {@link #LEAVE} to end the conversation. */
public record DialogueChoicePayload(int index) implements CustomPacketPayload {

    public static final int LEAVE = -1;

    public static final Type<DialogueChoicePayload> TYPE = new Type<>(FrosthainMod.id("dialogue_choice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueChoicePayload> CODEC =
            CustomPacketPayload.codec(DialogueChoicePayload::write, DialogueChoicePayload::read);

    private static DialogueChoicePayload read(RegistryFriendlyByteBuf buf) {
        return new DialogueChoicePayload(buf.readVarInt());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(index);
    }

    @Override
    public Type<DialogueChoicePayload> type() {
        return TYPE;
    }
}
