package com.ascension.combat;

import com.ascension.combat.internal.CombatAttachments;
import com.ascension.combat.internal.CombatAttributes;
import com.ascension.combat.internal.CombatContent;
import com.ascension.combat.internal.armor.EdgeOfNightEffects;
import com.ascension.combat.internal.armor.CollectorsPantsEffects;
import com.ascension.combat.internal.armor.GuardianWingEffects;
import com.ascension.combat.internal.armor.ProteanMaskEffects;
import com.ascension.combat.internal.armor.RocketbeltEffects;
import com.ascension.combat.internal.armor.SterakHarnessEffects;
import com.ascension.combat.internal.armor.UnendingDespairEffects;
import com.ascension.combat.internal.armor.ZoniasHourglassEffects;
import com.ascension.combat.internal.growth.Growth;
import com.ascension.combat.internal.mechanics.CombatMobEffects;
import com.ascension.combat.internal.mechanics.CombatStateTracking;
import com.ascension.combat.internal.mechanics.GrievousWoundsHealing;
import com.ascension.combat.internal.mechanics.OmnivampHealing;
import com.ascension.combat.internal.mechanics.PhysicalDamage;
import com.ascension.combat.internal.net.CombatNetwork;
import com.ascension.combat.internal.rune.RuneCommands;
import com.ascension.combat.internal.rune.RuneConfig;
import com.ascension.combat.internal.rune.RuneContent;
import com.ascension.combat.internal.rune.RuneRegistries;
import com.ascension.combat.internal.rune.RuneSystem;
import com.ascension.combat.internal.stat.StatSystem;
import com.ascension.combat.internal.weapons.DarkenBladeEffects;
import com.ascension.combat.internal.weapons.DariusAxeEffects;
import com.ascension.combat.internal.weapons.DivineSundererEffects;
import com.ascension.combat.internal.weapons.EndlessHungerEffects;
import com.ascension.combat.internal.weapons.GaleforceEffects;
import com.ascension.combat.internal.weapons.HeartwoodOfPallasEffects;
import com.ascension.combat.internal.weapons.MortalReminderEffects;
import com.ascension.combat.internal.weapons.RagefangEffects;
import com.ascension.combat.internal.weapons.SpearOfShojinEffects;
import com.ascension.combat.internal.weapons.TestMagicBladeEffects;
import com.ascension.combat.internal.weapons.VeigosBladeEffects;
import com.ascension.combat.internal.weapons.WraithbladeEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for {@code ascension_combat}.
 *
 * <p>Tier 1 in ADR-0003: must load and work with no other Ascension module present beyond
 * {@code core}. Anything needing a third-party mod belongs in a Tier 2 {@code ascension-compat-*}
 * jar, never here; anything another Tier 1 module needs to read from this one &mdash; combat
 * state, for {@code ascension-atmosphere}'s combat-drain rune &mdash; is published through
 * {@code ascension_core} rather than a direct dependency (ADR-0011, ADR-0012).
 *
 * <p>The combat-state event listener that populates {@code CombatStateRegistry} is
 * {@link CombatStateTracking}, stood up 2026-09-08 once The Protean Mask became the first real
 * consumer (the Bulwark rune, combat-api.md section 7, will be a second once it exists).
 *
 * <p>Design record: {@code docs/technical/combat-api.md}.
 */
@Mod(AscensionCombat.MOD_ID)
public final class AscensionCombat {

    public static final String MOD_ID = "ascension_combat";

    private static final Logger LOGGER = LoggerFactory.getLogger(AscensionCombat.class);

    public AscensionCombat(IEventBus modBus, ModContainer container) {
        CombatContent.register(modBus);
        com.ascension.combat.internal.BuildPathContent.register(modBus);
        CombatAttachments.register(modBus);
        CombatAttributes.register(modBus);
        CombatMobEffects.register(modBus);
        // Every item's stats, from data (docs/technical/item-stats.md, ADR-0019).
        StatSystem.register(modBus, NeoForge.EVENT_BUS);
        modBus.addListener(CombatNetwork::register);
        CombatNetwork.registerLoginSync(NeoForge.EVENT_BUS);

        // Runes (docs/technical/rune-system.md): the rune registry, Rune Chalk, settings, command.
        RuneRegistries.register(modBus);
        RuneContent.register(modBus);
        com.ascension.combat.internal.enchanting.EnchantingContent.register(modBus, NeoForge.EVENT_BUS);
        container.registerConfig(ModConfig.Type.SERVER, RuneConfig.SPEC);
        container.registerConfig(ModConfig.Type.SERVER, com.ascension.combat.internal.mechanics.TeamConfig.SPEC, "ascension_combat-teams-server.toml");
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> RuneCommands.register(event.getDispatcher()));

        // The one place non-client code names a client class (same isolation AtmosphereNetwork
        // uses for its own client-only handler reference) -- gated so the keybind never loads on
        // a dedicated server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.ascension.combat.client.CombatKeybinds.register(modBus);
            com.ascension.combat.client.AbilityCooldownDecorator.register(modBus);
            com.ascension.combat.client.stat.StatTooltips.register(NeoForge.EVENT_BUS);
            com.ascension.combat.client.stat.EnchantmentTooltips.register(NeoForge.EVENT_BUS);
            com.ascension.combat.client.stat.MechanicTooltips.register(NeoForge.EVENT_BUS);
            com.ascension.combat.client.LegendaryBeams.register(NeoForge.EVENT_BUS);
        }

        // Shared mitigation formula (docs/technical/combat-api.md section 3) -- registered before
        // any per-weapon lethality/penetration effect that also touches the ARMOR reduction
        // category, per PhysicalDamage's own javadoc on EventPriority ordering.
        // Teammates cannot hurt each other: first of all, so a cancelled hit does nothing else.
        com.ascension.combat.internal.mechanics.TeamFire.register(NeoForge.EVENT_BUS);
        PhysicalDamage.register(NeoForge.EVENT_BUS);
        com.ascension.combat.internal.dev.DamageChecks.register(NeoForge.EVENT_BUS);
        OmnivampHealing.register(NeoForge.EVENT_BUS);

        VeigosBladeEffects.register(NeoForge.EVENT_BUS);
        DariusAxeEffects.register(NeoForge.EVENT_BUS);
        SpearOfShojinEffects.register(NeoForge.EVENT_BUS);
        MortalReminderEffects.register(NeoForge.EVENT_BUS);
        GrievousWoundsHealing.register(NeoForge.EVENT_BUS);
        DivineSundererEffects.register(NeoForge.EVENT_BUS);
        DarkenBladeEffects.register(NeoForge.EVENT_BUS);
        RagefangEffects.register(NeoForge.EVENT_BUS);
        EndlessHungerEffects.register(NeoForge.EVENT_BUS);
        WraithbladeEffects.register(NeoForge.EVENT_BUS);
        HeartwoodOfPallasEffects.register(NeoForge.EVENT_BUS);
        GaleforceEffects.register(NeoForge.EVENT_BUS);
        CombatStateTracking.register(NeoForge.EVENT_BUS);
        ProteanMaskEffects.register(NeoForge.EVENT_BUS);
        EdgeOfNightEffects.register(NeoForge.EVENT_BUS);
        GuardianWingEffects.register(NeoForge.EVENT_BUS);
        RocketbeltEffects.register(NeoForge.EVENT_BUS);
        CollectorsPantsEffects.register(NeoForge.EVENT_BUS);
        UnendingDespairEffects.register(NeoForge.EVENT_BUS);
        ZoniasHourglassEffects.register(NeoForge.EVENT_BUS);
        SterakHarnessEffects.register(NeoForge.EVENT_BUS);
        TestMagicBladeEffects.register(NeoForge.EVENT_BUS);

        // The runes (docs/technical/rune-system.md); each path's behaviours register inside.
        RuneSystem.register(NeoForge.EVENT_BUS);

        // Player growth: skill points from milestones, a four-arm tree (docs/progression/growth.md).
        Growth.register(modBus, NeoForge.EVENT_BUS);

        LOGGER.info("Ascension Combat loaded ({})", container.getModInfo().getVersion());
    }
}
