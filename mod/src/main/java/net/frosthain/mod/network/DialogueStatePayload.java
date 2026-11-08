package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: the current state of a conversation. The client only shows translated lines and the visible
 * answers; conditions and effects stay on the server ({@code DialogueSession}).
 */
public record DialogueStatePayload(boolean open, String dialogue, String node, List<String> choices, boolean atEnd)
        implements CustomPacketPayload {

    public static final Type<DialogueStatePayload> TYPE = new Type<>(FrosthainMod.id("dialogue_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueStatePayload> CODEC =
            CustomPacketPayload.codec(DialogueStatePayload::write, DialogueStatePayload::read);

    private static final int MAX_CHOICES = 9;

    public DialogueStatePayload {
        choices = List.copyOf(choices.subList(0, Math.min(choices.size(), MAX_CHOICES)));
    }

    public static DialogueStatePayload closed() {
        return new DialogueStatePayload(false, "", "", List.of(), false);
    }

    private static DialogueStatePayload read(RegistryFriendlyByteBuf buf) {
        boolean open = buf.readBoolean();
        String dialogue = buf.readUtf();
        String node = buf.readUtf();
        int count = Math.min(buf.readVarInt(), MAX_CHOICES);
        List<String> choices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            choices.add(buf.readUtf());
        }
        return new DialogueStatePayload(open, dialogue, node, choices, buf.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(open);
        buf.writeUtf(dialogue);
        buf.writeUtf(node);
        buf.writeVarInt(choices.size());
        choices.forEach(buf::writeUtf);
        buf.writeBoolean(atEnd);
    }

    @Override
    public Type<DialogueStatePayload> type() {
        return TYPE;
    }
}
