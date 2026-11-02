package net.frosthain.mod.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.frosthain.mod.network.PowerStatePayload;
import net.frosthain.mod.network.UsePowerPayload;
import net.minecraft.client.Minecraft;

import java.util.Optional;

/** Client copy of resource and ability bar; the keys only send the slot, the server decides. */
public final class ClientPowers {

    private static PowerStatePayload state;

    private ClientPowers() {
    }

    public static Optional<PowerStatePayload> state() {
        return Optional.ofNullable(state);
    }

    static void accept(PowerStatePayload payload) {
        state = payload;
    }

    public static void use(Minecraft client, int slot) {
        if (client.player != null && client.gui.screen() == null && state != null) {
            ClientPlayNetworking.send(new UsePowerPayload(slot));
        }
    }

    static void clear() {
        state = null;
    }
}
