package com.ascension.core.internal.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ascension.core.api.client.HudAnchor;
import com.ascension.core.api.client.HudElement;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * The HUD registry is a map, a sort and a flag, so it is decidable without a running game
 * (ADR-0008). What it draws is checked in a client, not here.
 */
class HudElementsTest {

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    private static HudElement element(int order) {
        return new HudElement() {
            @Override
            public HudAnchor anchor() {
                return HudAnchor.ABOVE_HOTBAR;
            }

            @Override
            public int width() {
                return 10;
            }

            @Override
            public int height() {
                return 10;
            }

            @Override
            public boolean visible() {
                return true;
            }

            @Override
            public void render(GuiGraphics graphics, float partialTick, float opacity) {
            }

            @Override
            public int order() {
                return order;
            }
        };
    }

    @Test
    void emptyBeforeFreeze() {
        HudElements registry = new HudElements();
        registry.add(id("a"), element(0));
        assertTrue(registry.elements().isEmpty());
        assertTrue(registry.ids().isEmpty());
    }

    @Test
    void sortsByOrderThenId() {
        HudElements registry = new HudElements();
        registry.add(id("zeta"), element(0));
        registry.add(id("alpha"), element(5));
        registry.add(id("beta"), element(0));
        registry.freeze();
        assertEquals(List.of(id("beta"), id("zeta"), id("alpha")), registry.ids());
    }

    @Test
    void sameListEveryCallAfterFreeze() {
        HudElements registry = new HudElements();
        registry.add(id("a"), element(0));
        registry.freeze();
        assertSame(registry.elements(), registry.elements());
    }

    @Test
    void duplicateIdIsRejected() {
        HudElements registry = new HudElements();
        registry.add(id("a"), element(0));
        assertThrows(IllegalArgumentException.class, () -> registry.add(id("a"), element(1)));
    }

    @Test
    void registrationClosesAtFreeze() {
        HudElements registry = new HudElements();
        registry.freeze();
        assertThrows(IllegalStateException.class, () -> registry.add(id("late"), element(0)));
    }

    @Test
    void freezeIsIdempotent() {
        HudElements registry = new HudElements();
        registry.add(id("a"), element(0));
        registry.freeze();
        registry.freeze();
        assertEquals(1, registry.elements().size());
    }

    @Test
    void hostFlag() {
        HudElements registry = new HudElements();
        assertFalse(registry.hostPresent());
        registry.declareHost(id("host"));
        assertTrue(registry.hostPresent());
    }
}
