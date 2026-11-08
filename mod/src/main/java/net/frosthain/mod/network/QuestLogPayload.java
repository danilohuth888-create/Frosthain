package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client: the player's quest log in the {@code QuestLogCodec} format. */
public record QuestLogPayload(String logJson) implements CustomPacketPayload {

    public static final Type<QuestLogPayload> TYPE = new Type<>(FrosthainMod.id("quest_log"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestLogPayload> CODEC =
            CustomPacketPayload.codec(QuestLogPayload::write, QuestLogPayload::read);

    private static QuestLogPayload read(RegistryFriendlyByteBuf buf) {
        return new QuestLogPayload(buf.readUtf());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(logJson);
    }

    @Override
    public Type<QuestLogPayload> type() {
        return TYPE;
    }
}
