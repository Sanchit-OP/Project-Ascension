package com.ascension.compat.ftbquests.internal;

import com.ascension.compat.ftbquests.AscensionCompatFtbQuests;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * One chat line, once per player, saying where the quest book is (docs/progression/journey.md section 5.9:
 * nothing else tells a new player the book exists, and it is the game's guide).
 *
 * <p>"Once" is a saved flag on the player, kept through death, as {@code ascension-compat-ftbteams} does
 * for its party line. The key is sent as a key binding, so the client shows whatever the player has bound
 * it to rather than our default. Quests' own binding is named by string: this jar has no compile
 * dependency on Quests.
 */
public final class QuestBookWelcome {

    /** Quests' own key binding for the book. A binding component shows the player's current key for it. */
    private static final String QUEST_BOOK_KEY = "key.ftbquests.quests";

    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AscensionCompatFtbQuests.MOD_ID);

    /** Whether this player has been shown the line. */
    private static final Supplier<AttachmentType<Boolean>> TOLD = TYPES.register(
            "told_about_the_book",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .build());

    private QuestBookWelcome() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        TYPES.register(modBus);
        gameBus.addListener(QuestBookWelcome::onLoggedIn);
    }

    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getData(TOLD)) {
            return;
        }
        player.setData(TOLD, true);
        player.sendSystemMessage(Component.translatable("quest.ascension_compat_ftbquests.welcome",
                Component.keybind(QUEST_BOOK_KEY)));
    }
}
