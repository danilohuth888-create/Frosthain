package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: show a quest in the HUD tracker or stop tracking it (max. 3, rules in QuestRules). */
public record QuestActionPayload(boolean pin, String quest) implements CustomPacketPayload {

    public static final Type<QuestActionPayload> TYPE = new Type<>(FrosthainMod.id("quest_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuestActionPayload> CODEC =
            CustomPacketPayload.codec(QuestActionPayload::write, QuestActionPayload::read);

    private static QuestActionPayload read(RegistryFriendlyByteBuf buf) {
        return new QuestActionPayload(buf.readBoolean(), buf.readUtf(128));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(pin);
        buf.writeUtf(quest);
    }

    @Override
    public Type<QuestActionPayload> type() {
        return TYPE;
    }
}
