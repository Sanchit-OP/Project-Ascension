package com.ascension.combat.client.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * A development aid, dormant unless a file named {@code keybind-audit.txt} sits in the game
 * directory: once the game has settled on its first menu (every mod has registered its keys and the
 * options are loaded), it writes every key binding the pack has to {@code keybind-audit-out.tsv} and quits.
 *
 * <p>One tab-separated line per binding: the key's name, its category, its default key, the key it
 * is set to now, the conflict context and the modifier. It is how the pack's default controls are
 * reviewed (which keys are taken, which collide, which are never used) without clicking through the
 * Controls screen.
 */
public final class KeybindAudit {

    private static final String TAB = String.valueOf((char) 9);

    private static boolean written;
    private static int settle;

    private KeybindAudit() {
    }

    public static void register(IEventBus gameBus) {
        if (!DevScript.armed("keybind-audit.txt")) {
            return;
        }

        gameBus.addListener((ClientTickEvent.Post event) -> {
            Minecraft mc = Minecraft.getInstance();
            // Any first screen will do: Distant Horizons puts an update prompt in front of the title.
            if (written || mc.screen == null || mc.getOverlay() != null || ++settle < 100) {
                return;
            }

            written = true;
            List<String> lines = new ArrayList<>();
            lines.add(String.join(TAB, "name", "category", "default", "current", "context", "modifier"));
            List<KeyMapping> mappings = new ArrayList<>(List.of(mc.options.keyMappings));
            mappings.sort(Comparator.comparing(KeyMapping::getCategory).thenComparing(KeyMapping::getName));
            for (KeyMapping m : mappings) {
                lines.add(String.join(TAB, m.getName(), m.getCategory(), m.getDefaultKey().getName(), m.getKey().getName(),
                        String.valueOf(m.getKeyConflictContext()), String.valueOf(m.getKeyModifier())));
            }

            try {
                Files.write(FMLPaths.GAMEDIR.get().resolve("keybind-audit-out.tsv"), lines);
            } catch (IOException e) {
                System.out.println("[keybind-audit] could not write: " + e);
            }
            System.out.println("[keybind-audit] wrote " + mappings.size() + " bindings");
            mc.stop();
        });
    }
}
