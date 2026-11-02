package net.frosthain.mod.character;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.frosthain.core.ContentId;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.power.PowerRules;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.network.PowerStatePayload;
import net.frosthain.mod.network.UsePowerPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Active powers on the server (milestone M4.1, first part): resource and cooldowns per player in memory
 * (refilled on login), regeneration every few ticks, the ability bar synced to the client. Rules from
 * {@link PowerRules}, effects from {@link PowerEffects}.
 */
public final class PowerService {

    private static final int TICK_INTERVAL = 5;

    private static final class Entry {
        CharacterSheet sheet;
        PowerRules.State state;
        String lastSent = "";
    }

    private static final Map<UUID, Entry> PLAYERS = new ConcurrentHashMap<>();
    private static int tickCounter;

    private PowerService() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(PowerStatePayload.TYPE, PowerStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UsePowerPayload.TYPE, UsePowerPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(UsePowerPayload.TYPE,
                (payload, context) -> context.server().execute(() -> use(context.player(), payload.slot())));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PLAYERS.remove(handler.getPlayer().getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter % TICK_INTERVAL != 0) {
                return;
            }
            PowerRules rules = new PowerRules(FrosthainMod.content());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                entry(player).ifPresent(entry -> {
                    entry.state = rules.tick(entry.state, entry.sheet, TICK_INTERVAL, inCombat(player));
                    sync(player, entry, false);
                });
            }
        });
    }

    /** Called whenever the character sheet changed (creation, level, talents). */
    public static void invalidate(ServerPlayer player) {
        Entry entry = PLAYERS.get(player.getUUID());
        Optional<CharacterSheet> sheet = CharacterService.sheet(player);
        if (entry == null || sheet.isEmpty()) {
            PLAYERS.remove(player.getUUID());
            entry(player).ifPresent(fresh -> sync(player, fresh, true));
            return;
        }
        entry.sheet = sheet.get();
        sync(player, entry, true);
    }

    public static Optional<PowerRules.State> state(ServerPlayer player) {
        return Optional.ofNullable(PLAYERS.get(player.getUUID())).map(entry -> entry.state);
    }

    private static Optional<Entry> entry(ServerPlayer player) {
        Entry existing = PLAYERS.get(player.getUUID());
        if (existing != null) {
            return Optional.of(existing);
        }
        Optional<CharacterSheet> sheet = CharacterService.sheet(player);
        if (sheet.isEmpty()) {
            return Optional.empty();
        }
        Entry entry = new Entry();
        entry.sheet = sheet.get();
        entry.state = new PowerRules(FrosthainMod.content()).fresh(sheet.get());
        PLAYERS.put(player.getUUID(), entry);
        return Optional.of(entry);
    }

    private static boolean inCombat(ServerPlayer player) {
        int last = Math.max(player.getLastHurtByMobTimestamp(), player.getLastHurtMobTimestamp());
        return last > 0 && player.tickCount - last < PowerRules.COMBAT_TICKS;
    }

    private static void use(ServerPlayer player, int slot) {
        Optional<Entry> found = entry(player);
        if (found.isEmpty()) {
            return;
        }
        Entry entry = found.get();
        PowerRules rules = new PowerRules(FrosthainMod.content());
        List<ContentId> powers = rules.knownPowers(entry.sheet);
        if (slot < 0 || slot >= powers.size()) {
            player.sendOverlayMessage(Component.translatable("gui.frosthain.power.empty_slot", slot + 1));
            return;
        }
        ContentId power = powers.get(slot);
        List<PowerRules.Problem> problems = rules.check(entry.state, entry.sheet, power);
        if (!problems.isEmpty()) {
            player.sendOverlayMessage(problemMessage(problems.getFirst(), entry, power));
            return;
        }
        if (!PowerEffects.cast(player, power)) {
            player.sendOverlayMessage(Component.translatable("gui.frosthain.power.not_implemented", powerName(power)));
            return;
        }
        entry.state = rules.use(entry.state, entry.sheet, power);
        sync(player, entry, true);
    }

    private static Component problemMessage(PowerRules.Problem problem, Entry entry, ContentId power) {
        return switch (problem) {
            case ON_COOLDOWN -> Component.translatable("gui.frosthain.power.cooldown", powerName(power),
                    String.format(Locale.ROOT, "%.1f", entry.state.cooldown(power) / 20.0));
            case NOT_ENOUGH_RESOURCE -> Component.translatable("gui.frosthain.power.not_enough",
                    new PowerRules(FrosthainMod.content()).resource(entry.sheet)
                            .map(r -> (Component) Component.translatable(r.translationKey()))
                            .orElse(Component.literal("?")));
            default -> Component.translatable("gui.frosthain.power.unavailable", powerName(power));
        };
    }

    private static Component powerName(ContentId power) {
        return Component.translatable("power." + power.toTranslationFragment());
    }

    private static void sync(ServerPlayer player, Entry entry, boolean force) {
        PowerRules rules = new PowerRules(FrosthainMod.content());
        List<ContentId> powers = rules.knownPowers(entry.sheet);
        PowerStatePayload payload = new PowerStatePayload(
                rules.resource(entry.sheet).map(r -> r.id().toString()).orElse(""),
                (int) Math.floor(entry.state.resource()), rules.maximum(entry.sheet),
                powers.stream().map(ContentId::toString).toList(),
                powers.stream().map(entry.state::cooldown).toList());
        // Only send when something visible changed: whole resource points or cooldown tenths of a second.
        String key = payload.resource() + payload.current() + "/" + payload.maximum() + payload.powers()
                + payload.cooldowns().stream().map(c -> (c + 1) / 2).toList();
        if (force || !key.equals(entry.lastSent)) {
            entry.lastSent = key;
            ServerPlayNetworking.send(player, payload);
        }
    }
}
