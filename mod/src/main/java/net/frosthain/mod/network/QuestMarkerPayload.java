package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client: where the current quest objective is (shown as distance and direction under the tracker). */
public record QuestMarkerPayload(boolean present, String labelKey, int x, int y, int z) implements CustomPacketPayload {

    public static final Type<QuestMarkerPayload> TYPE = new Type<>(FrosthainMod.id("quest_marker"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestMarkerPayload> CODEC =
            CustomPacketPayload.codec(QuestMarkerPayload::write, QuestMarkerPayload::read);

    public static QuestMarkerPayload none() {
        return new QuestMarkerPayload(false, "", 0, 0, 0);
    }

    private static QuestMarkerPayload read(RegistryFriendlyByteBuf buf) {
        return new QuestMarkerPayload(buf.readBoolean(), buf.readUtf(128), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(present);
        buf.writeUtf(labelKey);
        buf.writeVarInt(x);
        buf.writeVarInt(y);
        buf.writeVarInt(z);
    }

    @Override
    public Type<QuestMarkerPayload> type() {
        return TYPE;
    }
}
