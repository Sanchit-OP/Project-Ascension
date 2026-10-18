package com.ascension.hud.internal;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Which registered elements the player has switched off in the config. Pure, so a test can decide
 * it. The layer recomputes only when the config list changes, so a frame does no string work.
 */
public final class HudVisibility {

    private HudVisibility() {
    }

    /**
     * @param ids    the registered elements' ids, in registry order
     * @param hidden the config's list of ids to leave out; entries that are not valid ids are ignored
     * @param out    receives true for each element that is hidden; at least ids.size() long
     */
    public static void compute(List<ResourceLocation> ids, List<? extends String> hidden, boolean[] out) {
        for (int i = 0; i < ids.size(); i++) {
            out[i] = false;
        }
        for (String entry : hidden) {
            ResourceLocation id = ResourceLocation.tryParse(entry == null ? "" : entry.trim());
            if (id == null) {
                continue;
            }
            int at = ids.indexOf(id);
            if (at >= 0) {
                out[at] = true;
            }
        }
    }
}
