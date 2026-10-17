package com.ascension.combat.client.dev;

import com.ascension.combat.api.rune.RangedHits;
import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.internal.rune.RuneAttachments;
import com.ascension.combat.internal.rune.RuneContent;
import com.ascension.combat.internal.rune.RuneHits;
import com.ascension.combat.internal.rune.RunePage;
import com.ascension.combat.internal.rune.Runes;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/**
 * A development aid, dormant unless a file named {@code rune-effects.txt} sits in the game
 * directory: it opens the singleplayer test world, builds a stone platform in the sky, and plays
 * real fights on it through the real server (zombies hunting the player, a kill with the real
 * damage pipeline, arrows shot from a distance), printing what happened as {@code [rune-effects]}
 * lines in the log, then quits. It exists because a rune's effect cannot be seen from its
 * arithmetic (ADR-0008): it is the way to watch Vanish and Deadeye work without a person
 * fighting zombies for ten minutes.
 */
public final class RuneEffectChecks {

    private static final DevScript SCRIPT = new DevScript("rune-effects");

    /** Zombies stand on the platform at fixed places; these are their ids, resolved when needed. */
    private static UUID victim;
    private static UUID hunterA;
    private static UUID hunterB;
    private static UUID target;
    private static BlockPos platform = BlockPos.ZERO;
    private static long killTick;
    private static double arrowBaseline;
    private static double thrownBaseline;

    private RuneEffectChecks() {
    }

    public static void register(IEventBus gameBus) {
        if (!DevScript.armed("rune-effects.txt")) {
            return;
        }

        plan();
        gameBus.addListener((ClientTickEvent.Post event) -> SCRIPT.tick(Minecraft.getInstance()));
        gameBus.addListener(EventPriority.LOWEST, (ProjectileImpactEvent event) -> {
            if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity victim
                    && event.getProjectile().getOwner() instanceof ServerPlayer) {
                SCRIPT.log("   impact at height " + String.format("%.2f", RuneHits.contactHeight(event.getProjectile(), victim) - victim.getY()) + " above the feet; eyes at "
                        + String.format("%.2f", victim.getEyeHeight()) + "; cancelled=" + event.isCanceled());
            }
        });
    }

    private static void plan() {
        add(200, RuneEffectChecks::prepare);

        // ---- Vanish: a page with it, three zombies hunting the player, one dies
        add(20, p -> save(p, page(RunePath.REAVER, "ambush", List.of("vanish", "finishing_blow"))));
        add(20, RuneEffectChecks::spawnHunters);
        add(2, p -> report(p, "before the kill"));
        add(2, RuneEffectChecks::killVictim);
        add(2, p -> report(p, "kill +2"));
        for (int i = 1; i <= 9; i++) {
            int at = i * 10;
            add(10, p -> report(p, "kill +" + at));
        }
        add(2, p -> report(p, "after the window"));
        add(60, p -> report(p, "60 ticks after the window"));

        // ---- Vanish must not fire for a cow, and must come back after its cooldown
        add(200, RuneEffectChecks::clearEffects);
        add(10, RuneEffectChecks::killCow);
        add(4, p -> report(p, "after a cow's death"));
        add(10, RuneEffectChecks::respawnVictim);
        add(10, RuneEffectChecks::killVictim);
        add(4, p -> report(p, "second kill, after the cooldown"));

        // ---- Deadeye: arrows from 22 blocks and from 5, a gun-like projectile with and without a headshot mark
        add(400, p -> save(p, page(RunePath.REAVER, "ambush", List.of("vanish", "finishing_blow"))));
        add(10, RuneEffectChecks::clearHunters);
        add(10, p -> placeTarget(p, 22));
        add(10, p -> shoot(p, 0.9, "no Deadeye: body @22"));
        add(25, p -> arrowBaseline = dealt(p, "BASELINE arrow, no Deadeye"));
        add(10, p -> thrown(p, false, "no Deadeye: gun-like hit @22"));
        add(220, p -> thrownBaseline = dealt(p, "BASELINE gun-like hit, no Deadeye"));
        add(20, p -> save(p, page(RunePath.REAVER, "deadeye", List.of("vanish", "finishing_blow"))));
        add(20, p -> shoot(p, 0.9, "Deadeye: body @22"));
        add(25, p -> ratio(p, arrowBaseline));
        add(10, p -> shoot(p, 1.74, "Deadeye: head @22"));
        add(25, p -> ratio(p, arrowBaseline));
        add(10, p -> placeTarget(p, 5));
        add(10, p -> shoot(p, 1.74, "Deadeye: head @5"));
        add(25, p -> ratio(p, arrowBaseline));
        add(10, p -> shoot(p, 0.9, "Deadeye: body @5"));
        add(25, p -> ratio(p, arrowBaseline));
        add(10, p -> placeTarget(p, 22));
        add(10, p -> thrown(p, false, "Deadeye: gun-like hit @22, no headshot mark"));
        add(5, p -> ratio(p, thrownBaseline));
        add(10, p -> thrown(p, true, "Deadeye: gun-like hit @22, marked headshot"));
        add(5, p -> ratio(p, thrownBaseline));
        add(30, p -> RangedHits.markHeadshot(p, targetZombie(p.serverLevel())));
        add(20, p -> thrown(p, false, "gun-like hit @22, 20 ticks after a headshot mark that nothing used"));
        add(5, p -> ratio(p, thrownBaseline));
        SCRIPT.add(10, () -> Minecraft.getInstance().stop());
    }

    // ------------------------------------------------------------------------------------------
    // Steps
    // ------------------------------------------------------------------------------------------

    /** A step that runs on the server thread with the singleplayer player. */
    private static void add(int waitAfter, Consumer<ServerPlayer> action) {
        SCRIPT.add(waitAfter, () -> {
            Minecraft mc = Minecraft.getInstance();
            MinecraftServer server = mc.getSingleplayerServer();
            if (server == null || mc.player == null) {
                return;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
            server.execute(() -> action.accept(player));
        });
    }

    private static void prepare(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        player.setGameMode(GameType.SURVIVAL);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000.0);
        player.setHealth(2000.0f);
        level.setDayTime(18000L);
        int y = Math.min(level.getMaxBuildHeight() - 10, (int) player.getY() + 40);
        platform = player.blockPosition().atY(y);
        for (int dx = -30; dx <= 30; dx++) {
            for (int dz = -30; dz <= 30; dz++) {
                level.setBlock(platform.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 2);
            }
        }
        player.teleportTo(platform.getX() + 0.5, y + 1.0, platform.getZ() + 0.5);
        player.getInventory().add(new ItemStack(RuneContent.RUNE_CHALK.get(), 8));
        player.giveExperienceLevels(30);
        SCRIPT.log("platform at " + platform);
    }

    private static RunePage page(RunePath primary, String keystone, List<String> minors) {
        return new RunePage(Optional.of(primary), Optional.of(Runes.id(keystone)), minors.stream().map(Runes::id).toList(),
                Optional.empty(), List.of(), List.of());
    }

    private static void save(ServerPlayer player, RunePage page) {
        Optional<?> refusal = Runes.save(player, page);
        SCRIPT.log("saved a page with " + page.keystone().orElseThrow().getPath() + " + " + page.primaryMinors().size()
                + " minors: " + (refusal.isPresent() ? "REFUSED " + refusal.get() : "accepted"));
    }

    private static Zombie zombie(ServerLevel level, double dx, double dz) {
        BlockPos pos = platform.offset((int) dx, 1, (int) dz);
        Zombie zombie = EntityType.ZOMBIE.spawn(level, pos, MobSpawnType.COMMAND);
        zombie.setBaby(false);
        zombie.setPersistenceRequired();
        // a hit zombie may call other zombies to its side, which would wander into the checks
        zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0.0);
        return zombie;
    }

    private static void spawnHunters(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Zombie a = zombie(level, 4, 0);
        Zombie b = zombie(level, -5, 3);
        Zombie c = zombie(level, 0, -6);
        victim = a.getUUID();
        hunterA = b.getUUID();
        hunterB = c.getUUID();
        for (Zombie zombie : List.of(a, b, c)) {
            zombie.setTarget(player);
        }
        SCRIPT.log("three zombies hunting the player");
    }

    private static void respawnVictim(ServerPlayer player) {
        Zombie fresh = zombie(player.serverLevel(), 4, 0);
        victim = fresh.getUUID();
        fresh.setTarget(player);
    }

    private static void killVictim(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (level.getEntity(victim) instanceof Zombie zombie) {
            killTick = level.getGameTime();
            zombie.hurt(level.damageSources().playerAttack(player), 1000.0f);
            SCRIPT.log("killed the victim zombie with a player attack");
        }
    }

    private static void killCow(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Cow cow = EntityType.COW.spawn(level, platform.offset(3, 1, 3), MobSpawnType.COMMAND);
        cow.hurt(level.damageSources().playerAttack(player), 1000.0f);
        SCRIPT.log("killed a cow");
    }

    private static void clearEffects(ServerPlayer player) {
        player.removeAllEffects();
    }

    private static void clearHunters(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (Zombie zombie : level.getEntitiesOfClass(Zombie.class, new AABB(platform).inflate(40.0, 20.0, 40.0))) {
            zombie.discard();
        }
    }

    private static void report(ServerPlayer player, String when) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        boolean window = player.getData(RuneAttachments.VANISH_WINDOW.get()).isActive(now);
        SCRIPT.log(when + " (game tick +" + (now - killTick) + "): invisible=" + player.isInvisible() + " effect=" + player.hasEffect(MobEffects.INVISIBILITY)
                + " window=" + window + " | zombie A targets " + targetOf(level, hunterA, player)
                + ", zombie B targets " + targetOf(level, hunterB, player));
    }

    private static String targetOf(ServerLevel level, UUID id, ServerPlayer player) {
        if (id == null || !(level.getEntity(id) instanceof Mob mob)) {
            return "(gone)";
        }

        return mob.getTarget() == player ? "THE PLAYER" : mob.getTarget() == null ? "nobody" : "someone else";
    }

    // ------------------------------------------------------------------------------------------
    // Deadeye
    // ------------------------------------------------------------------------------------------

    /** Puts one still zombie {@code distance} blocks east of the player, on the platform, at full health. */
    private static void placeTarget(ServerPlayer player, int distance) {
        ServerLevel level = player.serverLevel();
        for (Zombie other : level.getEntitiesOfClass(Zombie.class, new AABB(platform).inflate(40.0, 20.0, 40.0))) {
            other.discard();
        }

        Zombie zombie = zombie(level, distance, 0);
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
        zombie.setHealth(100.0f);
        target = zombie.getUUID();
        player.teleportTo(platform.getX() + 0.5, platform.getY() + 1.0, platform.getZ() + 0.5);
    }

    /** Shoots an arrow at a height on the target, from where the player stands, flat out so drop is small. */
    private static void shoot(ServerPlayer player, double aimHeight, String label) {
        ServerLevel level = player.serverLevel();
        if (!(level.getEntity(target) instanceof Zombie zombie)) {
            return;
        }

        zombie.setHealth(zombie.getMaxHealth());
        zombie.invulnerableTime = 0;
        Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), null);
        arrow.setBaseDamage(0.75);
        arrow.setCritArrow(false);
        Vec3 from = arrow.position();
        Vec3 aim = new Vec3(zombie.getX(), zombie.getY() + aimHeight, zombie.getZ());
        Vec3 direction = aimAt(from, aim, 8.0);
        arrow.shoot(direction.x, direction.y, direction.z, 8.0f, 0.0f);
        level.addFreshEntity(arrow);
        SCRIPT.log("SHOT " + label + " (distance " + String.format("%.1f", player.distanceTo(zombie)) + ")");
    }

    /** The direction that makes an arrow flying at {@code speed} land on {@code to}, by trying lifts until it does. */
    private static Vec3 aimAt(Vec3 from, Vec3 to, double speed) {
        double reach = Math.hypot(to.x - from.x, to.z - from.z);
        for (double lift = 0.0; lift < 6.0; lift += 0.02) {
            Vec3 direction = new Vec3(to.x - from.x, to.y - from.y + lift, to.z - from.z).normalize().scale(speed);
            Vec3 position = from;
            Vec3 motion = direction;
            for (int tick = 0; tick < 100 && Math.hypot(position.x - from.x, position.z - from.z) < reach; tick++) {
                position = position.add(motion);
                motion = new Vec3(motion.x * 0.99, motion.y * 0.99 - 0.05, motion.z * 0.99);
            }
            if (position.y >= to.y) {
                return direction;
            }
        }
        return to.subtract(from);
    }

    private static Zombie targetZombie(ServerLevel level) {
        return (Zombie) level.getEntity(target);
    }

    /** How much health the target has lost since it was last healed; logged under {@code label}. */
    private static double dealt(ServerPlayer player, String label) {
        if (player.serverLevel().getEntity(target) instanceof Zombie zombie) {
            double lost = zombie.getMaxHealth() - zombie.getHealth();
            SCRIPT.log(label + " = " + String.format("%.2f", lost));
            return lost;
        }
        return 0.0;
    }

    private static void ratio(ServerPlayer player, double reference) {
        if (player.serverLevel().getEntity(target) instanceof Zombie zombie) {
            double lost = zombie.getMaxHealth() - zombie.getHealth();
            SCRIPT.log("   damage dealt = " + String.format("%.2f", lost) + "  (x" + String.format("%.2f", lost / reference) + " of the baseline)");
        }
    }

    /** A hit like a gun's: damage from a projectile that never went through the arrow's impact. */
    private static void thrown(ServerPlayer player, boolean marked, String label) {
        ServerLevel level = player.serverLevel();
        if (!(level.getEntity(target) instanceof Zombie zombie)) {
            return;
        }

        zombie.setHealth(zombie.getMaxHealth());
        zombie.invulnerableTime = 0;
        Snowball ball = new Snowball(level, player);
        if (marked) {
            RangedHits.markHeadshot(player, zombie);
        }
        zombie.hurt(level.damageSources().thrown(ball, player), 6.0f);
        SCRIPT.log("HIT " + label + " (distance " + String.format("%.1f", player.distanceTo(zombie)) + ")");
    }
}
