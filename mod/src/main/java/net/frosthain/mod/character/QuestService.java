package net.frosthain.mod.character;

import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.frosthain.core.ContentId;
import net.frosthain.core.content.QuestDefinition;
import net.frosthain.core.persistence.QuestLogCodec;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.quest.QuestRules;
import net.frosthain.core.registry.ContentType;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.network.QuestActionPayload;
import net.frosthain.mod.network.QuestLogPayload;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Server side of quests (milestone M4.4, first part): the quest log is a persistent player attachment
 * ({@link QuestLogCodec}); progress goes through {@link QuestRules}; completed quests grant their experience.
 * Game events call {@link #advance}; until NPCs and places exist there is a test command.
 */
public final class QuestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrosthainMod.MOD_ID);

    public static final AttachmentType<String> QUESTS = AttachmentRegistry.create(FrosthainMod.id("quests"),
            builder -> builder.persistent(Codec.STRING).copyOnDeath());
    /** Entity tag prefix naming the content id an entity counts as for "defeat" objectives. */
    public static final String TARGET_TAG = "frosthain.id.";
    private static final double DEFEAT_CREDIT_RADIUS = 32;

    private QuestService() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(QuestLogPayload.TYPE, QuestLogPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(QuestActionPayload.TYPE, QuestActionPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.getPlayer(), log(handler.getPlayer())));
        ServerPlayNetworking.registerGlobalReceiver(QuestActionPayload.TYPE,
                (payload, context) -> context.server().execute(() -> pin(context.player(), payload)));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getEntity() instanceof ServerPlayer && entity.level() instanceof ServerLevel level) {
                onDefeat(level, entity);
            }
        });

        // Test commands until NPCs, places and items trigger objectives (needs cheats/operator).
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
                Commands.literal("frosthain").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("quest")
                                .then(Commands.literal("start").then(Commands.argument("quest", StringArgumentType.word())
                                        .executes(context -> start(context.getSource().getPlayerOrException(),
                                                ContentId.of(StringArgumentType.getString(context, "quest"))) ? 1 : 0)))
                                .then(Commands.literal("ziel").then(Commands.argument("quest", StringArgumentType.word())
                                        .then(Commands.argument("ziel", StringArgumentType.word())
                                                .executes(context -> advance(context.getSource().getPlayerOrException(),
                                                        ContentId.of(StringArgumentType.getString(context, "quest")),
                                                        StringArgumentType.getString(context, "ziel"), 1) ? 1 : 0)))))));
    }

    /**
     * "Defeat" objectives. A target matches an entity type ({@code minecraft:zombie}), an entity type tag
     * ({@code #minecraft:undead}) or a content id carried as entity tag {@code frosthain.id.<id>} (bosses).
     * Every player near the kill whose current objective it is gets credit, so groups can fight together.
     */
    public static void onDefeat(ServerLevel level, LivingEntity entity) {
        Set<String> ids = new HashSet<>();
        ids.add(EntityType.getKey(entity.getType()).toString());
        entity.entityTags().stream().filter(tag -> tag.startsWith(TARGET_TAG))
                .map(tag -> tag.substring(TARGET_TAG.length())).forEach(ids::add);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(entity) > DEFEAT_CREDIT_RADIUS * DEFEAT_CREDIT_RADIUS) {
                continue;
            }
            QuestLog log = log(player);
            for (ContentId quest : log.activeQuests()) {
                Optional<QuestDefinition> definition = FrosthainMod.content().get(ContentType.QUESTS, quest);
                if (definition.isEmpty()) {
                    continue;
                }
                for (String key : currentObjectives(log, quest)) {
                    definition.get().objective(key)
                            .filter(o -> o.type() == QuestDefinition.ObjectiveType.DEFEAT && matches(o.target(), ids, entity))
                            .ifPresent(o -> advance(player, quest, key, 1));
                }
            }
        }
    }

    private static boolean matches(String target, Set<String> ids, LivingEntity entity) {
        if (target.startsWith("#")) {
            return entity.getType().builtInRegistryHolder()
                    .is(TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(target.substring(1))));
        }
        return ids.contains(target);
    }

    public static QuestLog log(ServerPlayer player) {
        String json = player.getAttached(QUESTS);
        if (json == null) {
            return QuestLog.EMPTY;
        }
        try {
            return QuestLogCodec.decode(JsonParser.parseString(json).getAsJsonObject());
        } catch (RuntimeException e) {
            LOGGER.error("Questlog von {} ist nicht lesbar", player.getName().getString(), e);
            return QuestLog.EMPTY;
        }
    }

    /** Starts the opening main quests: main quests that are not the follow-up of another quest. */
    public static void onCharacterCreated(ServerPlayer player) {
        Set<ContentId> followUps = FrosthainMod.content().all(ContentType.QUESTS).values().stream()
                .flatMap(quest -> quest.followUps().stream())
                .collect(Collectors.toSet());
        for (QuestDefinition quest : FrosthainMod.content().all(ContentType.QUESTS).values()) {
            if (quest.kind().isMain() && !followUps.contains(quest.id())) {
                start(player, quest.id());
            }
        }
    }

    public static boolean start(ServerPlayer player, ContentId quest) {
        if (FrosthainMod.content().get(ContentType.QUESTS, quest).isEmpty()) {
            return false;
        }
        QuestRules.Outcome outcome = new QuestRules(FrosthainMod.content()).start(log(player), quest);
        apply(player, outcome);
        return !outcome.events().isEmpty();
    }

    /** Called by game events (talking, collecting, discovering) and the test command. */
    public static boolean advance(ServerPlayer player, ContentId quest, String objective, int amount) {
        QuestDefinition definition = FrosthainMod.content().get(ContentType.QUESTS, quest).orElse(null);
        if (definition == null || definition.objective(objective).isEmpty()) {
            return false;
        }
        QuestRules.Outcome outcome = new QuestRules(FrosthainMod.content()).advance(log(player), quest, objective, amount);
        apply(player, outcome);
        return !outcome.events().isEmpty();
    }

    private static void apply(ServerPlayer player, QuestRules.Outcome outcome) {
        if (outcome.events().isEmpty()) {
            return;
        }
        save(player, outcome.log());
        for (QuestRules.Event event : outcome.events()) {
            switch (event) {
                case QuestRules.Event.QuestStarted started -> player.sendSystemMessage(
                        Component.translatable("ui.frosthain.quests.started", questName(started.quest())));
                case QuestRules.Event.QuestCompleted completed -> {
                    player.sendSystemMessage(Component.translatable("ui.frosthain.quests.completed", questName(completed.quest())));
                    LOGGER.info("Quest abgeschlossen: {} von {}", completed.quest(), player.getName().getString());
                }
                default -> {
                }
            }
        }
        if (outcome.experienceGained() > 0) {
            CharacterService.addExperience(player, outcome.experienceGained());
        }
    }

    private static Component questName(ContentId quest) {
        return Component.translatable("quest." + quest.toTranslationFragment());
    }

    private static void pin(ServerPlayer player, QuestActionPayload payload) {
        ContentId quest;
        try {
            quest = ContentId.parse(payload.quest());
        } catch (IllegalArgumentException e) {
            return;
        }
        QuestRules rules = new QuestRules(FrosthainMod.content());
        QuestLog log = log(player);
        if (payload.pin()) {
            rules.pin(log, quest).ifPresent(updated -> save(player, updated));
        } else {
            save(player, rules.unpin(log, quest));
        }
        send(player, log(player));
    }

    private static void save(ServerPlayer player, QuestLog log) {
        player.setAttached(QUESTS, QuestLogCodec.encode(log).toString());
        send(player, log);
        QuestSites.onQuestLogChanged(player);
    }

    /** Keys of the objectives the player currently works on (only the next one for sequential quests). */
    public static List<String> currentObjectives(QuestLog log, ContentId quest) {
        Optional<QuestDefinition> definition = FrosthainMod.content().get(ContentType.QUESTS, quest);
        Optional<QuestLog.Progress> progress = log.progress(quest);
        if (definition.isEmpty() || progress.isEmpty()) {
            return List.of();
        }
        return new QuestRules(FrosthainMod.content()).currentObjectives(definition.get(), progress.get()).stream()
                .map(QuestDefinition.Objective::key)
                .toList();
    }

    private static void send(ServerPlayer player, QuestLog log) {
        ServerPlayNetworking.send(player, new QuestLogPayload(QuestLogCodec.encode(log).toString()));
    }
}
