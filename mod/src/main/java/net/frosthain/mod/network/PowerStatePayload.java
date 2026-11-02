package net.frosthain.mod.network;

import net.frosthain.mod.FrosthainMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: resource and ability bar. {@code powers} are in slot order, {@code cooldowns} the remaining
 * ticks per slot; costs and full cooldowns come from the content definitions on the client.
 */
public record PowerStatePayload(String resource, int current, int maximum, List<String> powers, List<Integer> cooldowns)
        implements CustomPacketPayload {

    public static final Type<PowerStatePayload> TYPE = new Type<>(FrosthainMod.id("power_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerStatePayload> CODEC =
            CustomPacketPayload.codec(PowerStatePayload::write, PowerStatePayload::read);

    private static final int MAX_SLOTS = 16;

    public PowerStatePayload {
        powers = List.copyOf(powers);
        cooldowns = List.copyOf(cooldowns);
    }

    private static PowerStatePayload read(RegistryFriendlyByteBuf buf) {
        String resource = buf.readUtf();
        int current = buf.readVarInt();
        int maximum = buf.readVarInt();
        int count = Math.min(buf.readVarInt(), MAX_SLOTS);
        List<String> powers = new ArrayList<>(count);
        List<Integer> cooldowns = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            powers.add(buf.readUtf());
            cooldowns.add(buf.readVarInt());
        }
        return new PowerStatePayload(resource, current, maximum, powers, cooldowns);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(resource);
        buf.writeVarInt(current);
        buf.writeVarInt(maximum);
        buf.writeVarInt(powers.size());
        for (int i = 0; i < powers.size(); i++) {
            buf.writeUtf(powers.get(i));
            buf.writeVarInt(cooldowns.get(i));
        }
    }

    @Override
    public Type<PowerStatePayload> type() {
        return TYPE;
    }
}
