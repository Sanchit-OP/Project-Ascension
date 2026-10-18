package com.ascension.combat.internal.stat;

import com.ascension.combat.AscensionCombat;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.FloatTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps a player's health across logging out and back in, and across a respawn, now that armour
 * gives Health (docs/technical/item-stats.md section 5).
 *
 * <p>The vanilla trap: a player's saved health is read back with {@code setHealth}, which caps it
 * at max health. Worn gear's modifiers are never saved and only return on the player's first tick,
 * so at that moment max health is the bare 20. Someone in diamond at 35 of 40 would log back in at
 * 20.
 *
 * <p>So the real health is saved beside vanilla's, in {@link #SAVED_HEALTH}, written from the
 * player at save time. At login their gear is re-applied first ({@link WornGear#reapply}), which
 * restores max health, and then the saved health with it. Vanilla's own first-tick equipment check
 * afterwards removes and re-adds the same modifiers, and caps health only after both, so nothing is
 * lost.
 *
 * <p>Mobs have the same trap when their chunk reloads, and are left to it: a zombie losing a few
 * points of a diamond chestplate's bonus is not worth an attachment on every mob.
 */
public final class HealthAcrossLogin {

    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AscensionCombat.MOD_ID);

    /**
     * Copied on death, so a player who respawns still has it to save. Without that, their next
     * logout would save nothing and the login after it would lose health again.
     */
    private static final Supplier<AttachmentType<SavedHealth>> SAVED_HEALTH = TYPES.register(
            "saved_health",
            () -> AttachmentType.builder(SavedHealth::new).serialize(new Serializer()).copyOnDeath().build());

    private HealthAcrossLogin() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        TYPES.register(modBus);
        gameBus.addListener(HealthAcrossLogin::onLoggedIn);
        gameBus.addListener(EventPriority.LOWEST, HealthAcrossLogin::onRespawn);
    }

    /**
     * A respawn is the same trap in a smaller shape. A new body starts at the bare 20 Health, and
     * the modifiers that raise it (worn gear that survived death, growth, runes) come back one at a
     * time: gear on the new body's first tick, the others in their own respawn handlers. Healing
     * to full before they were all back left a player in netherite at 23 of 63.
     *
     * <p>So this runs last (lowest priority, after growth and the runes have put theirs back),
     * puts the worn gear's modifiers on now rather than a tick later, and only then heals. Coming
     * back from the End is not a death: health stays as it was.
     */
    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        WornGear.reapply(player);
        if (!event.isEndConquered()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        WornGear.reapply(player);

        SavedHealth saved = player.getData(SAVED_HEALTH);
        if (saved.atLastSave > player.getHealth()) {
            player.setHealth(saved.atLastSave);
        }
        saved.atLastSave = SavedHealth.NONE;
    }

    /**
     * Holds its owner only to read its health when saved. It lives on that same player and goes
     * with it, so it is not the long-lived reference ADR-0007 rule 1 forbids.
     */
    private static final class SavedHealth {
        static final float NONE = -1.0f;

        private final IAttachmentHolder owner;
        private float atLastSave;

        SavedHealth(IAttachmentHolder owner) {
            this(owner, NONE);
        }

        SavedHealth(IAttachmentHolder owner, float atLastSave) {
            this.owner = owner;
            this.atLastSave = atLastSave;
        }
    }

    private static final class Serializer implements IAttachmentSerializer<FloatTag, SavedHealth> {
        @Override
        public SavedHealth read(IAttachmentHolder holder, FloatTag tag, HolderLookup.Provider provider) {
            return new SavedHealth(holder, tag.getAsFloat());
        }

        @Override
        public @Nullable FloatTag write(SavedHealth attachment, HolderLookup.Provider provider) {
            return attachment.owner instanceof LivingEntity living ? FloatTag.valueOf(living.getHealth()) : null;
        }
    }
}
