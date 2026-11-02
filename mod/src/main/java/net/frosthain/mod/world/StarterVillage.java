package net.frosthain.mod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.frosthain.core.ContentId;
import net.frosthain.core.content.NpcDefinition;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.registry.ContentType;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.character.CharacterService;
import net.frosthain.mod.character.DialogueService;
import net.frosthain.mod.character.QuestService;
import net.frosthain.mod.network.QuestMarkerPayload;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The starting village (M5.2, first part), built once per world around the first player's spawn: a paved plaza
 * with a well, four cottages along dirt paths, Eldrin on the plaza and one NPC from {@code npcs/village/*} in front
 * of each cottage. The notice board (lectern) starts the side quest "Verlorene Vorräte"; its three supply crates
 * lie in the woods around the village and are found by right-clicking them.
 */
public final class StarterVillage {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrosthainMod.MOD_ID);
    private static final ContentId LOST_SUPPLIES = ContentId.of("lost_supplies");
    private static final String FIND_CRATES = "find_crates";
    private static final int RADIUS = 20;
    private static final int HOUSE_DISTANCE = 12;
    private static final List<ResourceKey<VillagerProfession>> PROFESSIONS = List.of(VillagerProfession.ARMORER,
            VillagerProfession.FARMER, VillagerProfession.CLERIC, VillagerProfession.CARTOGRAPHER);

    /** Where the village was built; stored on the overworld. */
    public record Village(BlockPos center, BlockPos board, List<BlockPos> crates) {
        public static final Codec<Village> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("center").forGetter(Village::center),
                BlockPos.CODEC.fieldOf("board").forGetter(Village::board),
                BlockPos.CODEC.listOf().fieldOf("crates").forGetter(Village::crates)
        ).apply(instance, Village::new));
    }

    public static final AttachmentType<Village> VILLAGE = AttachmentRegistry.create(FrosthainMod.id("village"),
            builder -> builder.persistent(Village.CODEC));
    /** Supply crates this player has already found. */
    public static final AttachmentType<List<BlockPos>> FOUND_CRATES = AttachmentRegistry.create(FrosthainMod.id("found_crates"),
            builder -> builder.persistent(BlockPos.CODEC.listOf()).copyOnDeath());

    private static int tickCounter;

    private StarterVillage() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            // Only in new worlds (no character yet): never level the area around an existing player's base.
            if (village(player.level()).isEmpty() && player.level() == server.overworld()
                    && player.getAttached(CharacterService.CHARACTER) == null) {
                build(player.level(), player.blockPosition());
            }
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                player instanceof ServerPlayer serverPlayer && use(serverPlayer, hit.getBlockPos())
                        ? InteractionResult.SUCCESS : InteractionResult.PASS);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter % 20 != 0) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                crateHints(player);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
                Commands.literal("frosthain").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("dorf").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            build(player.level(), player.blockPosition());
                            return 1;
                        }))));
    }

    public static Optional<Village> village(ServerLevel level) {
        return Optional.ofNullable(level.getServer().overworld().getAttached(VILLAGE));
    }

    // ---- interaction ----------------------------------------------------------------------------------------

    /** Right-click on the notice board or a supply crate. Returns true if handled. */
    public static boolean use(ServerPlayer player, BlockPos pos) {
        Optional<Village> village = village(player.level());
        if (village.isEmpty()) {
            return false;
        }
        if (pos.equals(village.get().board())) {
            QuestLog log = QuestService.log(player);
            if (!log.isActive(LOST_SUPPLIES) && !log.isCompleted(LOST_SUPPLIES)) {
                QuestService.start(player, LOST_SUPPLIES);
            } else {
                player.sendOverlayMessage(Component.translatable("gui.frosthain.board.nothing_new"));
            }
            return true;
        }
        if (!village.get().crates().contains(pos)) {
            return false;
        }
        if (!QuestService.log(player).isActive(LOST_SUPPLIES)) {
            player.sendOverlayMessage(Component.translatable("gui.frosthain.crate.unknown"));
            return true;
        }
        List<BlockPos> found = new ArrayList<>(foundCrates(player));
        if (found.contains(pos)) {
            player.sendOverlayMessage(Component.translatable("gui.frosthain.crate.already"));
            return true;
        }
        found.add(pos);
        player.setAttached(FOUND_CRATES, List.copyOf(found));
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.4, 0.3, 0.4, 0.0);
        level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.getInventory().add(new ItemStack(Items.BREAD, 3));
        QuestService.advance(player, LOST_SUPPLIES, FIND_CRATES, 1);
        return true;
    }

    public static List<BlockPos> foundCrates(ServerPlayer player) {
        List<BlockPos> found = player.getAttached(FOUND_CRATES);
        return found == null ? List.of() : found;
    }

    private static List<BlockPos> openCrates(ServerPlayer player) {
        if (!QuestService.log(player).isActive(LOST_SUPPLIES)) {
            return List.of();
        }
        List<BlockPos> found = foundCrates(player);
        return village(player.level()).map(v -> v.crates().stream().filter(c -> !found.contains(c)).toList()).orElse(List.of());
    }

    /** Waypoint to the nearest crate still to find, if the side quest is active. */
    public static Optional<QuestMarkerPayload> marker(ServerPlayer player) {
        return openCrates(player).stream()
                .min(Comparator.comparingDouble(c -> c.distSqr(player.blockPosition())))
                .map(c -> new QuestMarkerPayload(true, "gui.frosthain.marker.crate", c.getX(), c.getY(), c.getZ()));
    }

    private static void crateHints(ServerPlayer player) {
        for (BlockPos crate : openCrates(player)) {
            for (int i = 1; i <= 6; i++) {
                player.level().sendParticles(player, ParticleTypes.END_ROD, true, true,
                        crate.getX() + 0.5, crate.getY() + 1 + i, crate.getZ() + 0.5, 1, 0.05, 0.3, 0.05, 0.0);
            }
        }
    }

    // ---- construction ---------------------------------------------------------------------------------------

    /** Builds the village around {@code around} and remembers it on the overworld. */
    public static Village build(ServerLevel level, BlockPos around) {
        // The player ends up at the north-west edge of the plaza, Eldrin a few steps away, not inside the well.
        int x = around.getX() + 2;
        int z = around.getZ() + 5;
        BlockPos center = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        flatten(level, center);
        plaza(level, center);
        int[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int[] d : directions) {
            for (int i = 5; i < HOUSE_DISTANCE - 3; i++) {
                for (int side = -1; side <= 1; side++) {
                    level.setBlock(center.offset(d[0] * i + d[1] * side, -1, d[1] * i + d[0] * side),
                            Blocks.DIRT_PATH.defaultBlockState(), 3);
                }
            }
            cottage(level, center.offset(d[0] * HOUSE_DISTANCE, 0, d[1] * HOUSE_DISTANCE), d);
        }
        BlockPos board = center.offset(3, 0, 3);
        level.setBlock(board, Blocks.LECTERN.defaultBlockState(), 3);
        List<BlockPos> crates = new ArrayList<>();
        // Woods around the village; north stays free for the old shrine.
        for (double angle : new double[]{55, 160, 250}) {
            double rad = Math.toRadians(angle);
            int cx = center.getX() + (int) Math.round(Math.sin(rad) * 32);
            int cz = center.getZ() - (int) Math.round(Math.cos(rad) * 32);
            int cy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
            BlockPos crate = new BlockPos(cx, cy, cz);
            level.setBlock(crate, Blocks.BARREL.defaultBlockState(), 3);
            level.setBlock(crate.offset(1, 0, 0), Blocks.SPRUCE_SLAB.defaultBlockState(), 3);
            level.setBlock(crate.offset(0, -1, 1), Blocks.COARSE_DIRT.defaultBlockState(), 3);
            crates.add(crate);
        }
        placeNpcs(level, center);
        Village village = new Village(center, board, List.copyOf(crates));
        level.getServer().overworld().setAttached(VILLAGE, village);
        LOGGER.info("Startdorf bei {} errichtet, Vorratskisten {}", center.toShortString(), crates);
        return village;
    }

    /** Levels the village area: grass at y−1, dirt below to fill gaps, air above (removes trees). */
    private static void flatten(ServerLevel level, BlockPos center) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                BlockPos column = center.offset(dx, 0, dz);
                level.setBlock(column.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                for (int depth = 2; depth <= 8; depth++) {
                    BlockPos below = column.below(depth);
                    if (!level.getBlockState(below).canBeReplaced()) {
                        break;
                    }
                    level.setBlock(below, Blocks.DIRT.defaultBlockState(), 3);
                }
                for (int up = 0; up <= 12; up++) {
                    if (!level.getBlockState(column.above(up)).isAir()) {
                        level.setBlock(column.above(up), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /** Round stone plaza with a well and four lantern posts. */
    private static void plaza(ServerLevel level, BlockPos center) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (dx * dx + dz * dz <= 26) {
                    Block stone = (dx * 5 + dz * 3) % 4 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS;
                    level.setBlock(center.offset(dx, -1, dz), stone.defaultBlockState(), 3);
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean middle = dx == 0 && dz == 0;
                level.setBlock(center.offset(dx, -2, dz), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                level.setBlock(center.offset(dx, -1, dz), (middle ? Blocks.WATER : Blocks.STONE_BRICKS).defaultBlockState(), 3);
                if (!middle) {
                    level.setBlock(center.offset(dx, 0, dz), Blocks.STONE_BRICK_WALL.defaultBlockState(), 3);
                }
            }
        }
        for (int[] c : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
            level.setBlock(center.offset(c[0], 0, c[1]), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
            level.setBlock(center.offset(c[0], 1, c[1]), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
            level.setBlock(center.offset(c[0], 2, c[1]), Blocks.LANTERN.defaultBlockState(), 3);
        }
    }

    /** 7×7 cottage: cobblestone base, plank walls with log corners, windows, stepped spruce roof, door towards the plaza. */
    private static void cottage(ServerLevel level, BlockPos center, int[] outward) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.setBlock(center.offset(dx, -1, dz), Blocks.COBBLESTONE.defaultBlockState(), 3);
                boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                boolean corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
                for (int up = 0; up <= 2; up++) {
                    BlockState state = !wall ? Blocks.AIR.defaultBlockState()
                            : corner ? Blocks.SPRUCE_LOG.defaultBlockState()
                            : up == 0 ? Blocks.COBBLESTONE.defaultBlockState()
                            : up == 1 && (dx == 0 || dz == 0) ? Blocks.GLASS_PANE.defaultBlockState()
                            : Blocks.OAK_PLANKS.defaultBlockState();
                    level.setBlock(center.offset(dx, up, dz), state, 3);
                }
            }
        }
        for (int layer = 0; layer <= 3; layer++) {
            int r = 4 - layer;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block roof = layer == 3 ? Blocks.SPRUCE_SLAB : Blocks.SPRUCE_PLANKS;
                    level.setBlock(center.offset(dx, 3 + layer, dz), roof.defaultBlockState(), 3);
                }
            }
        }
        // Door gap facing the plaza (opposite of the outward direction), lantern inside.
        BlockPos door = center.offset(-outward[0] * 3, 0, -outward[1] * 3);
        level.setBlock(door, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(door.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(center.offset(outward[0] * 2, 0, outward[1] * 2), Blocks.LANTERN.defaultBlockState(), 3);
    }

    /** Eldrin on the plaza; village NPCs (sorted by id) in front of the cottages. */
    private static void placeNpcs(ServerLevel level, BlockPos center) {
        DialogueService.spawnNpc(level, ContentId.of("eldrin"), center.offset(0, 0, -3), 0, VillagerProfession.LIBRARIAN);
        List<NpcDefinition> villagers = FrosthainMod.content().all(ContentType.NPCS).values().stream()
                .filter(npc -> npc.id().path().startsWith("village/"))
                .sorted(Comparator.comparing((NpcDefinition npc) -> npc.id().toString()))
                .toList();
        int[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int i = 0; i < Math.min(villagers.size(), directions.length); i++) {
            int[] d = directions[i];
            BlockPos spot = center.offset(d[0] * (HOUSE_DISTANCE - 5), 0, d[1] * (HOUSE_DISTANCE - 5));
            // Facing the plaza.
            float yaw = (float) Math.toDegrees(Math.atan2(d[0], -d[1]));
            DialogueService.spawnNpc(level, villagers.get(i).id(), spot, yaw, PROFESSIONS.get(i));
        }
        if (villagers.size() > directions.length) {
            LOGGER.warn("Nur {} Dorf-NPCs platziert, {} definiert", directions.length, villagers.size());
        }
    }
}
