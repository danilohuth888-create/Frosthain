package net.frosthain.mod.character;

import net.frosthain.core.content.RuneDefinition;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * What a released rune seal does. Numbers come from the rune's {@code values}; area runes only hit hostile
 * creatures and other non-player targets that powers may hit (never NPCs or players).
 */
final class RuneEffects {

    private RuneEffects() {
    }

    static void release(ServerPlayer player, RuneDefinition rune) {
        ServerLevel level = player.level();
        int duration = (int) rune.value("duration_ticks", 100);
        int amplifier = (int) rune.value("amplifier", 0);
        switch (rune.effect()) {
            case FIRE_BURST -> {
                for (LivingEntity target : targets(player, rune.value("radius", 5))) {
                    target.hurtServer(level, player.damageSources().indirectMagic(player, player), damage(player, rune));
                    target.igniteForSeconds(duration / 20f);
                }
                ring(level, player, ParticleTypes.FLAME, rune.value("radius", 5));
                sound(player, SoundEvents.FIRECHARGE_USE, 0.8f);
            }
            case FROST_BURST -> {
                for (LivingEntity target : targets(player, rune.value("radius", 5))) {
                    target.hurtServer(level, player.damageSources().indirectMagic(player, player), damage(player, rune));
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, amplifier));
                }
                ring(level, player, ParticleTypes.SNOWFLAKE, rune.value("radius", 5));
                sound(player, SoundEvents.PLAYER_HURT_FREEZE, 0.9f);
            }
            case WARD -> {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, amplifier));
                level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 60, 0.6, 0.9, 0.6, 0.4);
                sound(player, SoundEvents.BEACON_ACTIVATE, 1.4f);
            }
            case HEAL -> {
                player.heal((float) (player.getMaxHealth() * rune.value("heal_fraction", 0.3)));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier));
                level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 8, 0.5, 0.4, 0.5, 0.1);
                sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f);
            }
            case SWIFTNESS -> {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, amplifier));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, duration, 0));
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 20, 0.4, 0.1, 0.4, 0.05);
                sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.9f);
            }
        }
    }

    private static List<LivingEntity> targets(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                entity -> !(entity instanceof Player) && entity.distanceToSqr(player) <= radius * radius && PowerEffects.isTarget(entity));
    }

    private static float damage(ServerPlayer player, RuneDefinition rune) {
        double min = rune.value("damage_min", 0);
        double max = rune.value("damage_max", min);
        return (float) (min + player.getRandom().nextDouble() * (max - min));
    }

    private static void ring(ServerLevel level, ServerPlayer player, ParticleOptions particle, double radius) {
        for (int i = 0; i < 40; i++) {
            double a = Math.PI * 2 * i / 40;
            level.sendParticles(particle, player.getX() + Math.cos(a) * radius, player.getY() + 0.4,
                    player.getZ() + Math.sin(a) * radius, 2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    private static void sound(ServerPlayer player, SoundEvent sound, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0f, pitch);
    }
}
