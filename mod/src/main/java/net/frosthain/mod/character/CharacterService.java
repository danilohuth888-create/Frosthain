package net.frosthain.mod.character;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.frosthain.core.ContentId;
import net.frosthain.core.character.CharacterFactory;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.QuickCreation;
import net.frosthain.core.persistence.CharacterSheetCodec;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.network.CharacterRejectedPayload;
import net.frosthain.mod.network.CharacterStatePayload;
import net.frosthain.mod.network.CreateCharacterPayload;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Server side of the character (milestone M2.2): the sheet lives as a persistent player attachment in the
 * versioned {@link CharacterSheetCodec} format; creation requests are validated with the core rules.
 */
public final class CharacterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrosthainMod.MOD_ID);

    /** JSON of {@link CharacterSheetCodec}; survives death and is saved with the player. */
    public static final AttachmentType<String> CHARACTER = AttachmentRegistry.create(FrosthainMod.id("character"),
            builder -> builder.persistent(Codec.STRING).copyOnDeath());

    private CharacterService() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(CharacterStatePayload.TYPE, CharacterStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CharacterRejectedPayload.TYPE, CharacterRejectedPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CreateCharacterPayload.TYPE, CreateCharacterPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendState(handler.getPlayer()));
        ServerPlayNetworking.registerGlobalReceiver(CreateCharacterPayload.TYPE,
                (payload, context) -> context.server().execute(() -> create(context.player(), payload)));
    }

    public static Optional<CharacterSheet> sheet(ServerPlayer player) {
        String json = player.getAttached(CHARACTER);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(CharacterSheetCodec.decode(JsonParser.parseString(json).getAsJsonObject()).sheet());
        } catch (RuntimeException e) {
            LOGGER.error("Charakter von {} ist nicht lesbar", player.getName().getString(), e);
            return Optional.empty();
        }
    }

    private static void sendState(ServerPlayer player) {
        CharacterStatePayload state = sheet(player)
                .map(sheet -> new CharacterStatePayload(true, sheet.name(), sheet.species().toString(),
                        sheet.classLevels().keySet().iterator().next().toString(), sheet.background().toString(),
                        sheet.classLevels().values().stream().mapToInt(Integer::intValue).sum()))
                .orElseGet(CharacterStatePayload::absent);
        ServerPlayNetworking.send(player, state);
    }

    private static void create(ServerPlayer player, CreateCharacterPayload payload) {
        if (sheet(player).isPresent()) {
            sendState(player);
            return;
        }
        CharacterFactory.Result result;
        try {
            result = new CharacterFactory(FrosthainMod.content()).create(QuickCreation.request(FrosthainMod.content(),
                    payload.name(), ContentId.parse(payload.species()), ContentId.parse(payload.characterClass()),
                    ContentId.parse(payload.background())), player.getUUID());
        } catch (IllegalArgumentException e) {
            result = new CharacterFactory.Result.Rejected(List.of(e.getMessage()));
        }
        switch (result) {
            case CharacterFactory.Result.Created created -> {
                player.setAttached(CHARACTER, CharacterSheetCodec.encode(created.sheet()).toString());
                LOGGER.info("Charakter erstellt: {} ({}, {}) für {}", created.sheet().name(), created.sheet().species(),
                        payload.characterClass(), player.getName().getString());
                sendState(player);
            }
            case CharacterFactory.Result.Rejected rejected ->
                    ServerPlayNetworking.send(player, new CharacterRejectedPayload(rejected.problems()));
        }
    }
}
