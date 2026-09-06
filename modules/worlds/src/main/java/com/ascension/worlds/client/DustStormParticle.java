package com.ascension.worlds.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The dust-storm particle's actual rendering, as opposed to {@code WorldsContent.DUST_STORM_PARTICLE}
 * which is just the registry entry.
 *
 * <p><strong>Exists because vanilla's ash particles cannot do this job.</strong> The first version
 * of this effect reused {@code ParticleTypes.WHITE_ASH}, and it looked exactly like snowfall
 * regardless of colour or the velocity passed to {@code addParticle} &mdash; because
 * {@code WhiteAshParticle}'s own factory throws that velocity away and substitutes its own small,
 * near-static, randomised drift (the same family {@code AshParticle} and {@code FallingDustParticle}
 * belong to; all three hijack their velocity parameters for their own ambient-fall look). There is
 * no vanilla particle built for "blown sideways by wind" to reuse, so this is a real particle class
 * rather than a vanilla one reused &mdash; but it needs no new art: {@code particles/dust_storm.json}
 * points at the existing {@code generic_0}-{@code generic_3} sprites vanilla's own smoke/ash
 * particles already use, just tinted tan here instead of grey.
 */
final class DustStormParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    private DustStormParticle(
            ClientLevel level, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;

        // Set directly, not through Particle's (x,y,z,xd,yd,zd) constructor -- that overload
        // renormalises direction and adds heavy random jitter, built for an omnidirectional puff
        // (an explosion, a step). Wind needs to keep the exact direction and speed it was given.
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        this.friction = 0.99f;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.quadSize = 0.12f + this.random.nextFloat() * 0.08f;
        this.lifetime = 30 + this.random.nextInt(20);

        // Neutral gray, matching DustStormEffect's fog tint and screen overlay so every layer of
        // the effect reads as one storm rather than three uncoordinated colours. (Was a warm tan
        // in the first version -- read as an Earth desert sandstorm rather than lunar regolith.)
        this.rCol = 0.55f;
        this.gCol = 0.55f;
        this.bCol = 0.58f;

        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new DustStormParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
