package com.ascension.combat.internal.mechanics;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * The sound and the burst of particles a legendary item makes when its active or big proc fires,
 * so using one is felt and seen by everyone nearby. Server side, sent to the clients in range by
 * vanilla's own packets: nothing new on the wire and nothing stored. Only vanilla's sounds and
 * particles, so there is no audio or art to ship.
 */
public final class Presence {

    private Presence() {
    }

    /** A sound at the player and {@code count} particles around their middle. A client player does nothing. */
    public static void burst(Player player, SoundEvent sound, float volume, float pitch,
                             ParticleOptions particle, int count, double spread) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
        level.sendParticles(particle, player.getX(), player.getY(0.5), player.getZ(), count, spread, spread * 0.8, spread, 0.02);
    }
}
