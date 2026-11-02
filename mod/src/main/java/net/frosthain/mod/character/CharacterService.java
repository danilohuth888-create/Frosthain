package net.frosthain.mod.character;

import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.frosthain.core.ContentId;
import net.frosthain.core.character.CharacterFactory;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.QuickCreation;
import net.frosthain.core.character.TalentRef;
import net.frosthain.core.persistence.CharacterSheetCodec;
import net.frosthain.core.progression.Blocker;
import net.frosthain.core.progression.LevelingRules;
import net.frosthain.core.progression.TalentAllocator;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.network.CharacterRejectedPayload;
import net.frosthain.mod.network.CharacterStatePayload;
import net.frosthain.mod.network.CreateCharacterPayload;
import net.frosthain.mod.network.TalentActionPayload;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Server side of the character (milestones M2.2/M3.3): the sheet lives as a persistent player attachment in the
 * versioned {@link CharacterSheetCodec} format; creation and talent requests are validated with the core rules,
 * attribute talents are applied to the player ({@link TalentEffects}).
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
        PayloadTypeRegistry.serverboundPlay().register(TalentActionPayload.TYPE, TalentActionPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sheet(handler.getPlayer()).ifPresent(sheet -> TalentEffects.apply(handler.getPlayer(), sheet, FrosthainMod.content()));
            sendState(handler.getPlayer());
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                sheet(newPlayer).ifPresent(sheet -> TalentEffects.apply(newPlayer, sheet, FrosthainMod.content())));
        // Experience for defeating hostile creatures (rules/progression.json, "defeat_experience").
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getEntity() instanceof ServerPlayer player && !(entity instanceof Player)) {
                int experience = FrosthainMod.content().progression()
                        .map(rules -> rules.experienceForDefeat(entity.getMaxHealth(), entity instanceof Enemy))
                        .orElse(0);
                if (experience > 0 && addExperience(player, experience).isPresent()) {
                    player.sendOverlayMessage(Component.translatable("gui.frosthain.experience_gained", experience));
                }
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(CreateCharacterPayload.TYPE,
                (payload, context) -> context.server().execute(() -> create(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(TalentActionPayload.TYPE,
                (payload, context) -> context.server().execute(() -> talentAction(context.player(), payload)));

        // Test command until experience sources exist (M3.2): /frosthain ep <amount>, needs cheats/operator.
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
                Commands.literal("frosthain").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("ep").then(Commands.argument("menge", IntegerArgumentType.integer(1, 1_000_000))
                                .executes(context -> grantExperience(context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "menge"),
                                        message -> context.getSource().sendSuccess(() -> message, false)))))));
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

    private static void save(ServerPlayer player, CharacterSheet sheet) {
        player.setAttached(CHARACTER, CharacterSheetCodec.encode(sheet).toString());
        TalentEffects.apply(player, sheet, FrosthainMod.content());
        sendState(player);
        PowerService.invalidate(player);
    }

    private static void sendState(ServerPlayer player) {
        CharacterStatePayload state = sheet(player)
                .map(sheet -> new CharacterStatePayload(true, sheet.name(), sheet.species().toString(),
                        sheet.classLevels().keySet().iterator().next().toString(), sheet.background().toString(),
                        sheet.characterLevel(), CharacterSheetCodec.encode(sheet).toString()))
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
                save(player, created.sheet());
                QuestService.onCharacterCreated(player);
                DialogueService.placeEldrinIfNeeded(player);
                LOGGER.info("Charakter erstellt: {} ({}, {}) für {}", created.sheet().name(), created.sheet().species(),
                        payload.characterClass(), player.getName().getString());
            }
            case CharacterFactory.Result.Rejected rejected -> reject(player, rejected.problems());
        }
    }

    private static void talentAction(ServerPlayer player, TalentActionPayload payload) {
        Optional<CharacterSheet> current = sheet(player);
        if (current.isEmpty()) {
            reject(player, List.of("Noch kein Charakter erstellt."));
            return;
        }
        TalentAllocator allocator = new TalentAllocator(FrosthainMod.content());
        CharacterSheet sheet = current.get();
        if (payload.action() == TalentActionPayload.Action.RESET_ALL) {
            save(player, allocator.resetAll(sheet));
            return;
        }
        TalentRef ref;
        try {
            ref = new TalentRef(ContentId.parse(payload.tree()), payload.node());
        } catch (IllegalArgumentException e) {
            reject(player, List.of(e.getMessage()));
            return;
        }
        boolean allocate = payload.action() == TalentActionPayload.Action.ALLOCATE;
        List<Blocker> blockers = allocate ? allocator.checkAllocate(sheet, ref) : allocator.checkRefund(sheet, ref);
        if (!blockers.isEmpty()) {
            reject(player, blockers.stream().map(Blocker::toString).toList());
            sendState(player);
            return;
        }
        save(player, allocate ? allocator.allocate(sheet, ref) : allocator.refund(sheet, ref));
        LOGGER.info("Talent {} {} für {}", allocate ? "freigeschaltet:" : "zurückgenommen:", ref, player.getName().getString());
    }

    private static int grantExperience(ServerPlayer player, int amount, java.util.function.Consumer<Component> feedback) {
        Optional<CharacterSheet> sheet = addExperience(player, amount);
        if (sheet.isEmpty()) {
            feedback.accept(Component.translatable("commands.frosthain.no_character"));
            return 0;
        }
        feedback.accept(Component.translatable("commands.frosthain.ep", amount, sheet.get().characterLevel(),
                new TalentAllocator(FrosthainMod.content()).availablePoints(sheet.get())));
        return 1;
    }

    /**
     * Adds FROSTHAIN experience and levels up the first class while possible (multiclass choice comes with M3.4).
     * Returns the updated sheet, or empty if the player has no character yet.
     */
    public static Optional<CharacterSheet> addExperience(ServerPlayer player, int amount) {
        Optional<CharacterSheet> current = sheet(player);
        if (current.isEmpty()) {
            return Optional.empty();
        }
        LevelingRules leveling = new LevelingRules(FrosthainMod.content());
        CharacterSheet sheet = current.get().withExperience(current.get().experience() + amount);
        ContentId mainClass = sheet.classLevels().keySet().iterator().next();
        int before = sheet.characterLevel();
        while (leveling.pendingLevelUps(sheet) > 0 && leveling.checkLevelUp(sheet, mainClass).isEmpty()) {
            sheet = leveling.levelUp(sheet, mainClass);
        }
        save(player, sheet);
        if (sheet.characterLevel() > before) {
            player.sendSystemMessage(Component.translatable("gui.frosthain.level_up", sheet.characterLevel()));
        }
        return Optional.of(sheet);
    }

    private static void reject(ServerPlayer player, List<String> problems) {
        ServerPlayNetworking.send(player, new CharacterRejectedPayload(problems));
    }
}
