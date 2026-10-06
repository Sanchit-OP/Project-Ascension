package com.ascension.worlds.internal.archive;

import com.ascension.worlds.AscensionWorlds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * One inscription in the builders' record: a title and its pages. Lore is data, in the datapack
 * registry {@code ascension_worlds:archive_entry}
 * ({@code data/<namespace>/ascension_worlds/archive_entry/<path>.json}), so a planet's story is
 * authored, extended or replaced without Java, and a datapack or another mod can add entries of
 * its own. Synced to clients, which draw the text.
 *
 * <p>Text is {@link Component}s, so the shipped entries are translation keys ({@code archive.*}
 * in the lang file) and a datapack can use literal text if it wants.
 *
 * <p>Read through an {@link ArchivePlinthBlock} (fixed in a structure) or an
 * {@link ArchiveTabletItem} (a portable copy); both name an entry by id. See
 * {@code docs/technical/lunar-archive-format.md}.
 *
 * @param title shown at the head of the first page
 * @param pages one book page each; keep each under about 240 characters
 */
public record ArchiveEntry(Component title, List<Component> pages) {

    public static final ResourceKey<Registry<ArchiveEntry>> REGISTRY = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(AscensionWorlds.MOD_ID, "archive_entry"));

    public static final Codec<ArchiveEntry> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    ComponentSerialization.CODEC.fieldOf("title").forGetter(ArchiveEntry::title),
                    ComponentSerialization.CODEC.listOf(1, 50).fieldOf("pages").forGetter(ArchiveEntry::pages))
            .apply(instance, ArchiveEntry::new));

    /** What a plinth or tablet with no entry, or an entry no datapack defines, shows. */
    public static final Component WORN_AWAY = Component.translatable("archive.ascension_worlds.worn_away");

    /**
     * The entry with this id, if the loaded datapacks define one. The one lookup the reader, the
     * tablet's tooltip and the debug commands all use, on either side ({@code RegistryAccess} is a
     * {@link HolderLookup.Provider}, and so is a tooltip's context).
     */
    public static Optional<ArchiveEntry> find(@Nullable HolderLookup.Provider registries, @Nullable ResourceLocation id) {
        if (registries == null || id == null) {
            return Optional.empty();
        }
        return registries.lookup(REGISTRY)
                .flatMap(entries -> entries.get(ResourceKey.create(REGISTRY, id)))
                .map(Holder::value);
    }
}
