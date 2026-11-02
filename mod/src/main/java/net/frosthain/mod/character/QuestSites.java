package net.frosthain.mod.character;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.frosthain.core.ContentId;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.network.QuestMarkerPayload;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quest places in the world until the starting village exists (M5.2): the old shrine with the glowing rune stone
 * is built north of the player when "Finde den alten Schrein" becomes the current objective. Coming close discovers
 * it, right-clicking the rune stone completes "Berühre den leuchtenden Runenstein". A particle column and the
 * tracker's distance line point the way.
 */
public final class QuestSites {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrosthainMod.MOD_ID);
    private static final ContentId AWAKENING = ContentId.of("awakening");
    private static final ContentId FIRST_RUNE = ContentId.of("first_rune");
    private static final int DISTANCE_NORTH = 40;
    private static final int DISCOVERY_RADIUS = 6;

    /** Centre of the shrine (the rune stone) built for this player. */
    public static final AttachmentType<BlockPos> SHRINE = AttachmentRegistry.create(FrosthainMod.id("shrine"),
            builder -> builder.persistent(BlockPos.CODEC).copyOnDeath());

    private static final Map<UUID, QuestMarkerPayload> LAST_MARKER = new ConcurrentHashMap<>();
    private static int tickCounter;

    private QuestSites() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(QuestMarkerPayload.TYPE, QuestMarkerPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            LAST_MARKER.remove(handler.getPlayer().getUUID());
            onQuestLogChanged(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_MARKER.remove(handler.getPlayer().getUUID()));
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                player instanceof ServerPlayer serverPlayer && touch(serverPlayer, hit.getBlockPos())
                        ? InteractionResult.SUCCESS : InteractionResult.PASS);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter % 20 != 0) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tick(player);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
                Commands.literal("frosthain").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("schrein").executes(context -> {
                            buildShrine(context.getSource().getPlayerOrException());
                            return 1;
                        }))));
    }

    public static Optional<BlockPos> shrine(ServerPlayer player) {
        return Optional.ofNullable(player.getAttached(SHRINE));
    }

    /** Called after every quest log change: builds the shrine once it is needed and updates the marker. */
    public static void onQuestLogChanged(ServerPlayer player) {
        if (isCurrent(player, AWAKENING, "find_shrine") && shrine(player).isEmpty()) {
            buildShrine(player);
        }
        sendMarker(player);
    }

    private static boolean isCurrent(ServerPlayer player, ContentId quest, String objective) {
        QuestLog log = QuestService.log(player);
        return log.isActive(quest) && QuestService.currentObjectives(log, quest).contains(objective);
    }

    private static void tick(ServerPlayer player) {
        Optional<BlockPos> center = shrine(player);
        if (center.isEmpty()) {
            return;
        }
        boolean findOpen = isCurrent(player, AWAKENING, "find_shrine");
        boolean touchOpen = isCurrent(player, FIRST_RUNE, "touch_rune");
        if (findOpen && player.blockPosition().closerThan(center.get(), DISCOVERY_RADIUS)) {
            QuestService.advance(player, AWAKENING, "find_shrine", 1);
            player.level().playSound(null, center.get(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 1.0f);
            return;
        }
        if (findOpen || touchOpen) {
            // Light column above the rune stone, visible from far away (long-distance particles).
            for (int i = 1; i <= 14; i++) {
                player.level().sendParticles(player, ParticleTypes.END_ROD, true, true,
                        center.get().getX() + 0.5, center.get().getY() + 1 + i, center.get().getZ() + 0.5, 1, 0.05, 0.3, 0.05, 0.0);
            }
        }
    }

    /** Right-click on the rune stone (or the crystal on top). Returns true if it advanced the quest. */
    public static boolean touch(ServerPlayer player, BlockPos pos) {
        Optional<BlockPos> center = shrine(player);
        if (center.isEmpty() || !(pos.equals(center.get()) || pos.equals(center.get().above()))) {
            return false;
        }
        if (!isCurrent(player, FIRST_RUNE, "touch_rune")) {
            return false;
        }
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.ENCHANT, center.get().getX() + 0.5, center.get().getY() + 1.5, center.get().getZ() + 0.5,
                60, 0.6, 0.8, 0.6, 0.5);
        level.playSound(null, center.get(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5f, 0.7f);
        return QuestService.advance(player, FIRST_RUNE, "touch_rune", 1);
    }

    /** Small ruined shrine: 7×7 stone brick platform, corner pillars with lanterns, candles, rune stone with amethyst. */
    public static BlockPos buildShrine(ServerPlayer player) {
        ServerLevel level = player.level();
        int x = player.getBlockX();
        int z = player.getBlockZ() - DISTANCE_NORTH;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos center = new BlockPos(x, y, z);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos floor = center.offset(dx, -1, dz);
                Block stone = (dx * 7 + dz * 3) % 5 == 0 ? Blocks.MOSSY_STONE_BRICKS
                        : (dx * 3 + dz) % 7 == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS;
                level.setBlock(floor, stone.defaultBlockState(), 3);
                for (int depth = 2; depth <= 4; depth++) {
                    BlockPos below = center.offset(dx, -depth, dz);
                    if (level.getBlockState(below).canBeReplaced()) {
                        level.setBlock(below, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    }
                }
                for (int up = 0; up <= 4; up++) {
                    level.setBlock(center.offset(dx, up, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (int dx : new int[]{-3, 3}) {
            for (int dz : new int[]{-3, 3}) {
                for (int up = 0; up < 3; up++) {
                    level.setBlock(center.offset(dx, up, dz), Blocks.STONE_BRICK_WALL.defaultBlockState(), 3);
                }
                level.setBlock(center.offset(dx, 3, dz), Blocks.LANTERN.defaultBlockState(), 3);
            }
        }
        BlockState candles = Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(BlockStateProperties.LIT, true);
        for (int[] offset : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
            level.setBlock(center.offset(offset[0], 0, offset[1]), candles, 3);
        }
        level.setBlock(center, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
        level.setBlock(center.above(), Blocks.AMETHYST_CLUSTER.defaultBlockState(), 3);
        player.setAttached(SHRINE, center);
        LOGGER.info("Alter Schrein bei {} für {} errichtet", center.toShortString(), player.getName().getString());
        return center;
    }

    private static void sendMarker(ServerPlayer player) {
        QuestMarkerPayload marker = shrine(player)
                .filter(c -> isCurrent(player, AWAKENING, "find_shrine") || isCurrent(player, FIRST_RUNE, "touch_rune"))
                .map(c -> new QuestMarkerPayload(true, isCurrent(player, FIRST_RUNE, "touch_rune")
                        ? "gui.frosthain.marker.rune_stone" : "gui.frosthain.marker.shrine", c.getX(), c.getY(), c.getZ()))
                .orElseGet(QuestMarkerPayload::none);
        if (!marker.equals(LAST_MARKER.get(player.getUUID()))) {
            LAST_MARKER.put(player.getUUID(), marker);
            ServerPlayNetworking.send(player, marker);
        }
    }
}
