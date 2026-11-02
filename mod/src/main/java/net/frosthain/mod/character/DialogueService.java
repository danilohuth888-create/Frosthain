package net.frosthain.mod.character;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.frosthain.core.ContentId;
import net.frosthain.core.content.DialogueDefinition;
import net.frosthain.core.content.NpcDefinition;
import net.frosthain.core.dialogue.DialogueSession;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.registry.ContentType;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.network.DialogueChoicePayload;
import net.frosthain.mod.network.DialogueStatePayload;
import net.frosthain.mod.world.StarterVillage;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC conversations on the server (milestone M4.3, first part). Sessions run the core {@link DialogueSession};
 * effects (start quest, advance objective, experience) are applied here, so a client cannot fake progress.
 *
 * <p>Until the starting village exists (M5.2), Eldrin is a villager placed next to the player when
 * "Sprich mit Eldrin" becomes the current objective. NPCs are recognised by the entity tag {@code frosthain.npc.<id>}.
 */
public final class DialogueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrosthainMod.MOD_ID);
    private static final String NPC_TAG_PREFIX = "frosthain.npc.";
    private static final ContentId ELDRIN = ContentId.of("eldrin");
    private static final ContentId AWAKENING = ContentId.of("awakening");
    private static final String TALK_ELDRIN = "talk_eldrin";

    /** Whether Eldrin was already placed for this player (placed once; the command places him again). */
    public static final AttachmentType<Boolean> ELDRIN_PLACED = AttachmentRegistry.create(FrosthainMod.id("eldrin_placed"),
            builder -> builder.persistent(Codec.BOOL).copyOnDeath());

    private static final Map<UUID, DialogueSession> SESSIONS = new ConcurrentHashMap<>();

    private DialogueService() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(DialogueStatePayload.TYPE, DialogueStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DialogueChoicePayload.TYPE, DialogueChoicePayload.CODEC);

        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            Optional<ContentId> npc = entity.entityTags().stream()
                    .filter(tag -> tag.startsWith(NPC_TAG_PREFIX))
                    .map(tag -> ContentId.of(tag.substring(NPC_TAG_PREFIX.length())))
                    .findFirst();
            if (npc.isEmpty()) {
                return InteractionResult.PASS;
            }
            open(serverPlayer, npc.get());
            return InteractionResult.SUCCESS;
        });
        ServerPlayNetworking.registerGlobalReceiver(DialogueChoicePayload.TYPE,
                (payload, context) -> context.server().execute(() -> choose(context.player(), payload.index())));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
            if (entity instanceof ServerPlayer player && damage > 0 && SESSIONS.remove(player.getUUID()) != null) {
                ServerPlayNetworking.send(player, DialogueStatePayload.closed());
                player.sendSystemMessage(Component.translatable("ui.frosthain.dialogue.interrupted"));
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> placeEldrinIfNeeded(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SESSIONS.remove(handler.getPlayer().getUUID()));

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
                Commands.literal("frosthain").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("eldrin").executes(context -> {
                            placeEldrin(context.getSource().getPlayerOrException());
                            return 1;
                        }))));
    }

    /** Places Eldrin once when talking to him is the player's current objective and there is no village (he lives there). */
    public static void placeEldrinIfNeeded(ServerPlayer player) {
        if (StarterVillage.village(player.level()).isPresent()) {
            return;
        }
        QuestLog log = QuestService.log(player);
        boolean waitingForEldrin = log.isActive(AWAKENING)
                && log.progress(AWAKENING).map(p -> p.count(TALK_ELDRIN) == 0).orElse(false);
        if (waitingForEldrin && !Boolean.TRUE.equals(player.getAttached(ELDRIN_PLACED))) {
            placeEldrin(player);
        }
    }

    /** Villager "Eldrin" three blocks in front of the player, facing them; no AI, invulnerable, never despawns. */
    public static void placeEldrin(ServerPlayer player) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 dir = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        double x = player.getX() + dir.x * 3;
        double z = player.getZ() + dir.z * 3;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        float yaw = (float) Math.toDegrees(Math.atan2(-(player.getX() - x), player.getZ() - z));
        spawnNpc(level, ELDRIN, BlockPos.containing(x, y, z), yaw, VillagerProfession.LIBRARIAN);
        player.setAttached(ELDRIN_PLACED, true);
    }

    /** A content NPC as villager: named, no AI, invulnerable, never despawns, recognised by its entity tag. */
    public static void spawnNpc(ServerLevel level, ContentId npc, BlockPos pos, float yaw, ResourceKey<VillagerProfession> profession) {
        Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.EVENT);
        if (villager == null) {
            return;
        }
        villager.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        villager.setYHeadRot(yaw);
        villager.setYBodyRot(yaw);
        villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), profession).withLevel(5));
        villager.setCustomName(Component.translatable("npc." + npc.toTranslationFragment()));
        villager.setNoAi(true);
        villager.setPersistenceRequired();
        villager.setPermanentlyInvulnerable(true);
        villager.addTag(NPC_TAG_PREFIX + npc.path());
        level.addFreshEntity(villager);
        LOGGER.info("NPC {} bei {} platziert", npc, pos.toShortString());
    }

    public static void open(ServerPlayer player, ContentId npcId) {
        Optional<DialogueDefinition> dialogue = FrosthainMod.content().get(ContentType.NPCS, npcId)
                .flatMap(NpcDefinition::dialogue)
                .flatMap(id -> FrosthainMod.content().get(ContentType.DIALOGUES, id));
        if (dialogue.isEmpty()) {
            return;
        }
        DialogueSession session = new DialogueSession(dialogue.get(), context(player));
        SESSIONS.put(player.getUUID(), session);
        send(player, session);
    }

    private static void choose(ServerPlayer player, int index) {
        DialogueSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            ServerPlayNetworking.send(player, DialogueStatePayload.closed());
            return;
        }
        if (index == DialogueChoicePayload.LEAVE) {
            SESSIONS.remove(player.getUUID());
            ServerPlayNetworking.send(player, DialogueStatePayload.closed());
            return;
        }
        if (session.isAtEndLine()) {
            session.acknowledgeEnd();
        } else if (index >= 0 && index < session.visibleChoices().size()) {
            session.choose(index);
        }
        if (session.state() != DialogueSession.State.ACTIVE) {
            SESSIONS.remove(player.getUUID());
        }
        send(player, session);
    }

    private static void send(ServerPlayer player, DialogueSession session) {
        if (session.state() != DialogueSession.State.ACTIVE) {
            ServerPlayNetworking.send(player, DialogueStatePayload.closed());
            return;
        }
        ServerPlayNetworking.send(player, new DialogueStatePayload(true, session.dialogue().id().toString(), session.currentNode(),
                session.visibleChoices().stream().map(DialogueDefinition.Choice::key).toList(), session.isAtEndLine()));
    }

    private static DialogueSession.Context context(ServerPlayer player) {
        return new DialogueSession.Context() {
            @Override
            public DialogueDefinition.QuestState questState(ContentId quest) {
                QuestLog log = QuestService.log(player);
                if (log.isCompleted(quest)) {
                    return DialogueDefinition.QuestState.COMPLETED;
                }
                return log.isActive(quest) ? DialogueDefinition.QuestState.ACTIVE : DialogueDefinition.QuestState.NOT_STARTED;
            }

            @Override
            public void apply(DialogueDefinition.Effect effect) {
                switch (effect) {
                    case DialogueDefinition.Effect.StartQuest start -> QuestService.start(player, start.quest());
                    case DialogueDefinition.Effect.AdvanceObjective advance ->
                            QuestService.advance(player, advance.quest(), advance.objective(), advance.amount());
                    case DialogueDefinition.Effect.GrantExperience experience -> CharacterService.addExperience(player, experience.amount());
                }
            }
        };
    }
}
