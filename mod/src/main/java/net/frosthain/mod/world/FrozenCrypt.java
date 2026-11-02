package net.frosthain.mod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.frosthain.core.ContentId;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.character.QuestService;
import net.frosthain.mod.network.QuestMarkerPayload;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The first dungeon (M4.5/M5.3, first part): a ruined entrance south-east of the village, a stairway twelve blocks
 * down, a corridor with two guard rooms (strays, zombies) and the burial chamber of Hrímgar, the Frost Warden.
 * The crypt is built once per world when "Finde die Gruft" becomes a player's current objective.
 *
 * <p>The boss wakes when a player whose objective is to defeat him enters the chamber. Phase one: sword and a
 * telegraphed frost nova every ten seconds. At half health phase two: he switches to the bow, two servants
 * appear and the nova comes every six seconds. Defeating him counts for every nearby player (generic
 * {@link QuestService#onDefeat}); the servants crumble with him.
 */
public final class FrozenCrypt {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrosthainMod.MOD_ID);
    public static final ContentId QUEST = ContentId.of("frozen_crypt");
    private static final String FIND = "find_crypt";
    private static final String DEFEAT = "defeat_warden";
    public static final String BOSS_TAG = "frosthain.boss.crypt_warden";
    public static final String PHASE_TWO_TAG = "frosthain.boss.phase_two";
    public static final String SERVANT_TAG = "frosthain.boss.servant";
    private static final int DISTANCE = 64;
    private static final double ANGLE = 110;
    private static final int DEPTH = 12;
    private static final int DISCOVERY_RADIUS = 6;
    private static final float BOSS_HEALTH = 120;
    private static final int NOVA_TELEGRAPH_TICKS = 30;

    /** Entrance (top of the stairway) and the middle of the chamber floor (feet level). */
    public record Crypt(BlockPos entrance, BlockPos chamber) {
        public static final Codec<Crypt> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("entrance").forGetter(Crypt::entrance),
                BlockPos.CODEC.fieldOf("chamber").forGetter(Crypt::chamber)
        ).apply(instance, Crypt::new));

        /** Inside of the burial chamber. */
        public AABB chamberBox() {
            return new AABB(chamber.getX() - 7, chamber.getY(), chamber.getZ() - 7,
                    chamber.getX() + 8, chamber.getY() + 7, chamber.getZ() + 8);
        }
    }

    public static final AttachmentType<Crypt> CRYPT = AttachmentRegistry.create(FrosthainMod.id("frozen_crypt"),
            builder -> builder.persistent(Crypt.CODEC));

    private static final Map<UUID, ServerBossEvent> BARS = new ConcurrentHashMap<>();
    private static int tickCounter;

    private FrozenCrypt() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter % 5 != 0) {
                return;
            }
            crypt(server.overworld()).ifPresent(crypt -> tick(server.overworld(), crypt));
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.entityTags().contains(BOSS_TAG) && entity.level() instanceof ServerLevel level) {
                onBossDefeated(level, entity);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
                Commands.literal("frosthain").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("gruft").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            build(player.level(), player.getBlockX(), player.getBlockZ() + 3);
                            return 1;
                        }))));
    }

    public static Optional<Crypt> crypt(ServerLevel level) {
        return Optional.ofNullable(level.getServer().overworld().getAttached(CRYPT));
    }

    private static boolean isCurrent(ServerPlayer player, String objective) {
        QuestLog log = QuestService.log(player);
        return log.isActive(QUEST) && QuestService.currentObjectives(log, QUEST).contains(objective);
    }

    /** Builds the crypt once it is needed (south-east of the village, or of the player without a village). */
    public static void onQuestLogChanged(ServerPlayer player) {
        if (crypt(player.level()).isPresent() || !isCurrent(player, FIND)) {
            return;
        }
        BlockPos origin = StarterVillage.village(player.level()).map(StarterVillage.Village::center).orElse(player.blockPosition());
        double rad = Math.toRadians(ANGLE);
        ServerLevel overworld = player.level().getServer().overworld();
        build(overworld, origin.getX() + (int) Math.round(Math.sin(rad) * DISTANCE),
                origin.getZ() - (int) Math.round(Math.cos(rad) * DISTANCE));
    }

    /** Waypoint: the entrance while searching, the chamber while the warden waits. */
    public static Optional<QuestMarkerPayload> marker(ServerPlayer player) {
        Optional<Crypt> crypt = crypt(player.level());
        if (crypt.isEmpty()) {
            return Optional.empty();
        }
        if (isCurrent(player, FIND)) {
            BlockPos e = crypt.get().entrance();
            return Optional.of(new QuestMarkerPayload(true, "gui.frosthain.marker.crypt", e.getX(), e.getY(), e.getZ()));
        }
        if (isCurrent(player, DEFEAT)) {
            BlockPos c = crypt.get().chamber();
            return Optional.of(new QuestMarkerPayload(true, "gui.frosthain.marker.warden", c.getX(), c.getY(), c.getZ()));
        }
        return Optional.empty();
    }

    // ---- encounter ------------------------------------------------------------------------------------------

    private static void tick(ServerLevel level, Crypt crypt) {
        AABB chamber = crypt.chamberBox();
        List<ServerPlayer> near = level.players().stream()
                .filter(p -> p.blockPosition().closerThan(crypt.chamber(), 96))
                .toList();
        BARS.entrySet().removeIf(entry -> {
            if (level.getEntity(entry.getKey()) instanceof LivingEntity boss && boss.isAlive()) {
                return false;
            }
            entry.getValue().removeAllPlayers();
            return true;
        });
        if (near.isEmpty()) {
            return;
        }
        for (ServerPlayer player : near) {
            if (isCurrent(player, FIND) && player.blockPosition().closerThan(crypt.entrance(), DISCOVERY_RADIUS)) {
                QuestService.advance(player, QUEST, FIND, 1);
                level.playSound(null, crypt.entrance(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.6f, 1.4f);
            }
        }
        List<Mob> bosses = level.getEntitiesOfClass(Mob.class, chamber.inflate(6), e -> e.entityTags().contains(BOSS_TAG));
        if (bosses.isEmpty()) {
            boolean challenger = near.stream().anyMatch(p -> chamber.contains(p.position()) && isCurrent(p, DEFEAT));
            if (challenger) {
                spawnBoss(level, crypt);
            }
            return;
        }
        List<ServerPlayer> inside = near.stream().filter(p -> chamber.inflate(4).contains(p.position())).toList();
        for (Mob boss : bosses) {
            bossTick(level, boss, inside);
        }
    }

    private static void bossTick(ServerLevel level, Mob boss, List<ServerPlayer> inside) {
        boolean phaseTwo = boss.entityTags().contains(PHASE_TWO_TAG);
        BossEvent.BossBarColor color = phaseTwo ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.BLUE;
        ServerBossEvent bar = BARS.computeIfAbsent(boss.getUUID(), id -> new ServerBossEvent(UUID.randomUUID(),
                boss.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10));
        bar.setProgress(Math.max(0, boss.getHealth() / boss.getMaxHealth()));
        for (ServerPlayer player : level.players()) {
            if (inside.contains(player)) {
                bar.addPlayer(player);
            } else {
                bar.removePlayer(player);
            }
        }
        if (!phaseTwo && boss.getHealth() <= boss.getMaxHealth() / 2) {
            enterPhaseTwo(level, boss, inside);
            bar.setColor(BossEvent.BossBarColor.PURPLE);
            phaseTwo = true;
        }
        if (inside.isEmpty()) {
            return;
        }
        int interval = phaseTwo ? 120 : 200;
        long phase = level.getGameTime() % interval;
        if (phase >= interval - NOVA_TELEGRAPH_TICKS && phase < interval - NOVA_TELEGRAPH_TICKS + 5) {
            // Telegraph: rising snowflakes and a warning, so players can step out of the ring.
            level.sendParticles(ParticleTypes.SNOWFLAKE, boss.getX(), boss.getY() + 1, boss.getZ(), 80, 2.5, 1.0, 2.5, 0.02);
            level.playSound(null, boss.blockPosition(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.HOSTILE, 1.5f, 0.5f);
            inside.forEach(p -> p.sendOverlayMessage(Component.translatable("gui.frosthain.boss.nova")));
        } else if (phase < 5) {
            frostNova(level, boss, inside, phaseTwo);
        }
    }

    private static void frostNova(ServerLevel level, Mob boss, List<ServerPlayer> inside, boolean phaseTwo) {
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2 * i / 48;
            level.sendParticles(ParticleTypes.SNOWFLAKE, boss.getX() + Math.cos(a) * 5, boss.getY() + 0.3,
                    boss.getZ() + Math.sin(a) * 5, 2, 0.1, 0.1, 0.1, 0.01);
        }
        level.playSound(null, boss.blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.5f, 0.6f);
        for (ServerPlayer player : inside) {
            if (player.distanceToSqr(boss) <= 25) {
                player.hurtServer(level, boss.damageSources().indirectMagic(boss, boss), phaseTwo ? 6 : 4);
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
            }
        }
    }

    private static void spawnBoss(ServerLevel level, Crypt crypt) {
        Mob boss = EntityTypes.STRAY.create(level, EntitySpawnReason.EVENT);
        if (boss == null) {
            return;
        }
        BlockPos spot = crypt.chamber().offset(0, 0, 5);
        boss.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 180, 0);
        boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        boss.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        boss.setDropChance(EquipmentSlot.MAINHAND, 0);
        boss.setDropChance(EquipmentSlot.HEAD, 0);
        base(boss, Attributes.MAX_HEALTH, BOSS_HEALTH);
        base(boss, Attributes.SCALE, 1.6);
        base(boss, Attributes.ARMOR, 6);
        base(boss, Attributes.KNOCKBACK_RESISTANCE, 0.7);
        base(boss, Attributes.ATTACK_DAMAGE, 4);
        base(boss, Attributes.FOLLOW_RANGE, 32);
        boss.setHealth(BOSS_HEALTH);
        boss.setCustomName(Component.translatable("entity.frosthain.crypt_warden"));
        boss.setCustomNameVisible(true);
        boss.setPersistenceRequired();
        boss.addTag(BOSS_TAG);
        boss.addTag(QuestService.TARGET_TAG + "frosthain:boss/crypt_warden");
        level.addFreshEntity(boss);
        level.sendParticles(ParticleTypes.SNOWFLAKE, boss.getX(), boss.getY() + 1.5, boss.getZ(), 120, 1.2, 1.5, 1.2, 0.05);
        level.playSound(null, spot, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0f, 0.7f);
        for (ServerPlayer player : level.players()) {
            if (crypt.chamberBox().contains(player.position())) {
                player.sendOverlayMessage(Component.translatable("gui.frosthain.boss.awakens"));
            }
        }
        LOGGER.info("Frostwächter bei {} erwacht", spot.toShortString());
    }

    private static void enterPhaseTwo(ServerLevel level, Mob boss, List<ServerPlayer> inside) {
        boss.addTag(PHASE_TWO_TAG);
        boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        boss.setDropChance(EquipmentSlot.MAINHAND, 0);
        boss.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60, 0));
        for (int side : new int[]{-1, 1}) {
            Mob servant = EntityTypes.STRAY.create(level, EntitySpawnReason.EVENT);
            if (servant == null) {
                continue;
            }
            servant.snapTo(boss.getX() + side * 3, boss.getY(), boss.getZ() - 2, 180, 0);
            servant.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            servant.setDropChance(EquipmentSlot.MAINHAND, 0);
            servant.setPersistenceRequired();
            servant.addTag(SERVANT_TAG);
            level.addFreshEntity(servant);
            level.sendParticles(ParticleTypes.SNOWFLAKE, servant.getX(), servant.getY() + 1, servant.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
        }
        level.playSound(null, boss.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5f, 0.6f);
        inside.forEach(p -> p.sendOverlayMessage(Component.translatable("gui.frosthain.boss.phase_two")));
    }

    private static void onBossDefeated(ServerLevel level, LivingEntity boss) {
        ServerBossEvent bar = BARS.remove(boss.getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
        }
        for (Mob servant : level.getEntitiesOfClass(Mob.class, boss.getBoundingBox().inflate(32), e -> e.entityTags().contains(SERVANT_TAG))) {
            level.sendParticles(ParticleTypes.POOF, servant.getX(), servant.getY() + 1, servant.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
            servant.discard();
        }
        for (ItemStack loot : List.of(new ItemStack(Items.DIAMOND, 2), new ItemStack(Items.GOLDEN_APPLE), new ItemStack(Items.AMETHYST_SHARD, 6))) {
            level.addFreshEntity(new ItemEntity(level, boss.getX(), boss.getY() + 0.5, boss.getZ(), loot));
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, boss.getX(), boss.getY() + 1.5, boss.getZ(), 200, 1.5, 1.5, 1.5, 0.1);
        level.playSound(null, boss.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 1.0f, 1.0f);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(boss) < 48 * 48) {
                player.sendOverlayMessage(Component.translatable("gui.frosthain.boss.defeated"));
            }
        }
    }

    private static void base(LivingEntity entity, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    // ---- construction ---------------------------------------------------------------------------------------

    /** Builds the crypt with its entrance at (x, surface, z); the dungeon runs south (+z) below ground. */
    public static Crypt build(ServerLevel level, int x, int z) {
        int y0 = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos e = new BlockPos(x, y0, z);
        int floor = y0 - DEPTH;
        ruin(level, e);
        // Corridor, guard rooms and chamber as closed shells first, then openings and the stairway.
        room(level, e.offset(-2, -DEPTH - 1, DEPTH + 1), e.offset(2, -DEPTH + 3, 30), Blocks.POLISHED_DEEPSLATE);
        room(level, e.offset(-9, -DEPTH - 1, 17), e.offset(-3, -DEPTH + 3, 23), Blocks.COBBLED_DEEPSLATE);
        room(level, e.offset(3, -DEPTH - 1, 17), e.offset(9, -DEPTH + 3, 23), Blocks.COBBLED_DEEPSLATE);
        room(level, e.offset(-8, -DEPTH - 1, 31), e.offset(8, -DEPTH + 7, 47), Blocks.DEEPSLATE_TILES);
        stairway(level, e);
        air(level, e.offset(-1, -DEPTH, DEPTH + 1), e.offset(1, -DEPTH + 2, DEPTH + 1));
        air(level, e.offset(-3, -DEPTH, 20), e.offset(-2, -DEPTH + 1, 20));
        air(level, e.offset(2, -DEPTH, 20), e.offset(3, -DEPTH + 1, 20));
        air(level, e.offset(-1, -DEPTH, 30), e.offset(1, -DEPTH + 2, 31));
        BlockPos chamber = new BlockPos(x, floor, z + 39);
        decorate(level, e, chamber);
        guards(level, e);
        Crypt crypt = new Crypt(e, chamber);
        level.getServer().overworld().setAttached(CRYPT, crypt);
        LOGGER.info("Gefrorene Gruft bei {} errichtet, Grabkammer {}", e.toShortString(), chamber.toShortString());
        return crypt;
    }

    /** Ruined platform with broken pillars and snow around the stairway entrance. */
    private static void ruin(ServerLevel level, BlockPos e) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -4; dz <= 0; dz++) {
                level.setBlock(e.offset(dx, -1, dz), mossy(dx, dz).defaultBlockState(), 3);
                for (int up = 0; up <= 5; up++) {
                    level.setBlock(e.offset(dx, up, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        int[][] pillars = {{-3, -4, 3}, {3, -4, 1}, {-3, 0, 4}, {3, 0, 2}};
        for (int[] p : pillars) {
            for (int up = 0; up < p[2]; up++) {
                level.setBlock(e.offset(p[0], up, p[1]), Blocks.STONE_BRICK_WALL.defaultBlockState(), 3);
            }
        }
        level.setBlock(e.offset(-3, 4, 0), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        for (int[] s : new int[][]{{-2, -3}, {1, -2}, {2, -4}, {-1, -1}}) {
            level.setBlock(e.offset(s[0], 0, s[1]), Blocks.SNOW.defaultBlockState(), 3);
        }
    }

    /** Twelve steps down towards +z, enclosed on all sides. */
    private static void stairway(ServerLevel level, BlockPos e) {
        BlockState stair = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        for (int i = 0; i < DEPTH; i++) {
            int z = 1 + i;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2 - i; dy <= 3 - i; dy++) {
                    level.setBlock(e.offset(dx, dy, z), mossy(dx, dy + z).defaultBlockState(), 3);
                }
            }
        }
        for (int i = 0; i < DEPTH; i++) {
            int z = 1 + i;
            air(level, e.offset(-1, -i, z), e.offset(1, 2 - i, z));
            for (int dx = -1; dx <= 1; dx++) {
                level.setBlock(e.offset(dx, -1 - i, z), stair, 3);
            }
        }
    }

    private static void decorate(ServerLevel level, BlockPos e, BlockPos chamber) {
        BlockState hanging = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        int floor = chamber.getY();
        for (int z : new int[]{16, 22, 28}) {
            level.setBlock(new BlockPos(e.getX(), floor + 2, e.getZ() + z), hanging, 3);
        }
        for (int dz = DEPTH + 1; dz <= 30; dz += 3) {
            level.setBlock(new BlockPos(e.getX() + (dz % 2 == 0 ? 1 : -1), floor - 1, e.getZ() + dz), Blocks.PACKED_ICE.defaultBlockState(), 3);
        }
        for (int side : new int[]{-6, 6}) {
            level.setBlock(new BlockPos(e.getX() + side + Integer.signum(side) * 2, floor, e.getZ() + 18), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
            BlockPos chest = new BlockPos(e.getX() + side, floor, e.getZ() + 22);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
            if (level.getBlockEntity(chest) instanceof Container container) {
                container.setItem(0, new ItemStack(Items.BREAD, 4));
                container.setItem(1, side < 0 ? new ItemStack(Items.ARROW, 16) : new ItemStack(Items.IRON_INGOT, 3));
            }
        }
        // Chamber: ice ring on the tiles, four pillars, hanging lanterns, throne of ice at the back.
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                int r = dx * dx + dz * dz;
                if (r >= 16 && r <= 25) {
                    level.setBlock(chamber.offset(dx, -1, dz), Blocks.PACKED_ICE.defaultBlockState(), 3);
                }
            }
        }
        for (int[] p : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
            for (int up = 0; up <= 6; up++) {
                level.setBlock(chamber.offset(p[0], up, p[1]), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
            }
            level.setBlock(chamber.offset(p[0] / 2, 6, p[1] / 2), hanging, 3);
        }
        for (int[] c : new int[][]{{-7, -7, 2}, {7, -7, 1}, {-7, 7, 3}, {7, 7, 2}}) {
            for (int up = 0; up < c[2]; up++) {
                level.setBlock(chamber.offset(c[0], up, c[1]), Blocks.PACKED_ICE.defaultBlockState(), 3);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            level.setBlock(chamber.offset(dx, 0, 7), Blocks.BLUE_ICE.defaultBlockState(), 3);
        }
        level.setBlock(chamber.offset(0, 1, 7), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
        level.setBlock(chamber.offset(0, 2, 7), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
    }

    /** Two strays with bows in the west room, three zombies in the east room. */
    private static void guards(ServerLevel level, BlockPos e) {
        int floor = e.getY() - DEPTH;
        spawnGuard(level, EntityTypes.STRAY, e.getX() - 5, floor, e.getZ() + 19, true);
        spawnGuard(level, EntityTypes.STRAY, e.getX() - 7, floor, e.getZ() + 21, true);
        for (int i = 0; i < 3; i++) {
            spawnGuard(level, EntityTypes.ZOMBIE, e.getX() + 5 + i, floor, e.getZ() + 18 + i * 2, false);
        }
    }

    private static void spawnGuard(ServerLevel level, EntityType<? extends Mob> type, int x, int y, int z, boolean bow) {
        Mob mob = type.create(level, EntitySpawnReason.EVENT);
        if (mob == null) {
            return;
        }
        mob.snapTo(x + 0.5, y, z + 0.5, 0, 0);
        if (bow) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
    }

    /** Closed box: walls, ceiling and the given floor; inside air. */
    private static void room(ServerLevel level, BlockPos from, BlockPos to, Block floor) {
        for (int x = from.getX(); x <= to.getX(); x++) {
            for (int y = from.getY(); y <= to.getY(); y++) {
                for (int z = from.getZ(); z <= to.getZ(); z++) {
                    boolean shell = x == from.getX() || x == to.getX() || y == to.getY() || z == from.getZ() || z == to.getZ();
                    Block block = y == from.getY() ? floor : shell ? mossy(x, y + z) : Blocks.AIR;
                    level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void air(ServerLevel level, BlockPos from, BlockPos to) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static Block mossy(int a, int b) {
        int h = Math.floorMod(a * 7 + b * 13, 9);
        return h == 0 ? Blocks.MOSSY_STONE_BRICKS : h == 1 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS;
    }
}
