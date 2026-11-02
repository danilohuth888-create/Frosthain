package net.frosthain.mod.character;

import net.frosthain.core.content.PowerDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * In-game effects of the powers in the base content (first version of the effect pipeline, M4.1). The numbers come
 * from the power data ({@code values}: range, damage, knockback, duration …), e.g. Runenstoß 12 blocks, 8–12 magic
 * damage as in the reference "Fähigkeitsdetails". Effects are server-side; clients see particles, sounds and the result.
 */
public final class PowerEffects {

    private PowerEffects() {
    }

    /** @return false if the power has no in-game effect yet (cost and cooldown are then not spent) */
    public static boolean cast(ServerPlayer player, PowerDefinition power) {
        ServerLevel level = player.level();
        int duration = (int) power.value("duration_ticks", 100);
        int amplifier = (int) power.value("amplifier", 0);
        switch (power.id().path()) {
            case "rune_strike" -> {
                beam(player, power.value("range", 12), damage(player, power), power.value("knockback", 0.9), ParticleTypes.END_ROD);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.8f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.4f);
            }
            case "arcane_bolt" -> {
                beam(player, power.value("range", 16), damage(player, power), power.value("knockback", 0.2), ParticleTypes.WITCH);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.8f);
            }
            case "second_wind" -> {
                player.heal(player.getMaxHealth() * (float) power.value("heal_fraction", 0.3));
                burst(level, player, ParticleTypes.HEART, 6);
            }
            case "action_surge" -> {
                effect(player, MobEffects.SPEED, duration, amplifier);
                effect(player, MobEffects.HASTE, duration, amplifier);
                burst(level, player, ParticleTypes.CRIT, 12);
            }
            case "battle_cry" -> {
                effect(player, MobEffects.STRENGTH, duration, amplifier);
                double radius = power.value("radius", 8);
                for (Entity nearby : level.getEntities(player, player.getBoundingBox().inflate(radius), e -> e instanceof ServerPlayer)) {
                    effect((ServerPlayer) nearby, MobEffects.STRENGTH, duration, amplifier);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 0.6f);
            }
            case "dodge_roll" -> {
                Vec3 look = player.getLookAngle();
                Vec3 flat = new Vec3(look.x, 0, look.z);
                Vec3 dash = (flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize()).scale(power.value("dash", 1.1)).add(0, 0.2, 0);
                player.setDeltaMovement(dash);
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
                effect(player, MobEffects.RESISTANCE, duration, 4);
                burst(level, player, ParticleTypes.CLOUD, 8);
            }
            case "shield_bash" -> {
                Optional<LivingEntity> target = firstTarget(player, power.value("range", 3.5));
                target.ifPresent(t -> {
                    t.hurtServer(level, player.damageSources().playerAttack(player), damage(player, power));
                    push(t, player.getLookAngle(), power.value("knockback", 1.2));
                    t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 1));
                });
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 0.9f);
            }
            case "rune_ward" -> {
                effect(player, MobEffects.RESISTANCE, duration, amplifier);
                burst(level, player, ParticleTypes.ENCHANT, 40);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Random damage between {@code damage_min} and {@code damage_max} (inclusive, whole points). */
    private static float damage(ServerPlayer player, PowerDefinition power) {
        int min = (int) power.value("damage_min", 1);
        int max = Math.max(min, (int) power.value("damage_max", min));
        return min + player.getRandom().nextInt(max - min + 1);
    }

    /** Straight magic beam from the eyes; hits the first living creature (not NPCs) before a solid block. */
    private static void beam(ServerPlayer player, double range, float damage, double knockback, ParticleOptions particle) {
        ServerLevel level = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(range));
        BlockHitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        Optional<LivingEntity> target = firstTarget(player, start, end);
        if (target.isPresent()) {
            end = target.get().getBoundingBox().inflate(0.3).clip(start, end).orElse(end);
            target.get().hurtServer(level, player.damageSources().indirectMagic(player, player), damage);
            push(target.get(), direction, knockback);
        }
        double length = start.distanceTo(end);
        for (double t = 0.8; t <= length; t += 0.4) {
            Vec3 point = start.add(direction.scale(t));
            level.sendParticles(particle, point.x, point.y - 0.15, point.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        level.sendParticles(particle, end.x, end.y, end.z, 12, 0.2, 0.2, 0.2, 0.05);
    }

    private static Optional<LivingEntity> firstTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        return firstTarget(player, start, start.add(player.getLookAngle().scale(range)));
    }

    private static Optional<LivingEntity> firstTarget(ServerPlayer player, Vec3 start, Vec3 end) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : player.level().getEntities(player, new AABB(start, end).inflate(1.0), PowerEffects::isTarget)) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.3).clip(start, end);
            if (hit.isPresent() && start.distanceToSqr(hit.get()) < bestDistance) {
                best = (LivingEntity) entity;
                bestDistance = start.distanceToSqr(hit.get());
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean isTarget(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !entity.isInvulnerable()
                && entity.entityTags().stream().noneMatch(tag -> tag.startsWith("frosthain.npc."));
    }

    private static void push(LivingEntity target, Vec3 direction, double strength) {
        Vec3 flat = new Vec3(direction.x, 0, direction.z);
        if (flat.lengthSqr() > 1e-4) {
            target.addDeltaMovement(flat.normalize().scale(strength).add(0, 0.25, 0));
            target.needsSync = true;
        }
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int ticks, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, ticks, amplifier));
    }

    private static void burst(ServerLevel level, ServerPlayer player, ParticleOptions particle, int count) {
        level.sendParticles(particle, player.getX(), player.getY() + 1.0, player.getZ(), count, 0.5, 0.5, 0.5, 0.05);
    }
}
