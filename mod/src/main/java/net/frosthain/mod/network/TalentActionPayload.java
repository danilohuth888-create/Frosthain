package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: unlock or refund one talent, or reset all talents. The server checks every rule. */
public record TalentActionPayload(Action action, String tree, String node) implements CustomPacketPayload {

    public enum Action { ALLOCATE, REFUND, RESET_ALL }

    public static final Type<TalentActionPayload> TYPE = new Type<>(FrosthainMod.id("talent_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TalentActionPayload> CODEC =
            CustomPacketPayload.codec(TalentActionPayload::write, TalentActionPayload::read);

    private static final int MAX_LENGTH = 128;

    private static TalentActionPayload read(RegistryFriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        Action[] actions = Action.values();
        if (ordinal < 0 || ordinal >= actions.length) {
            throw new IllegalArgumentException("Unbekannte Talentaktion " + ordinal);
        }
        return new TalentActionPayload(actions[ordinal], buf.readUtf(MAX_LENGTH), buf.readUtf(MAX_LENGTH));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(action.ordinal());
        buf.writeUtf(tree);
        buf.writeUtf(node);
    }

    @Override
    public Type<TalentActionPayload> type() {
        return TYPE;
    }
}
