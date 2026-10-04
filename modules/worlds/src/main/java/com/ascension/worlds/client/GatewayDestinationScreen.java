package com.ascension.worlds.client;

import com.ascension.worlds.internal.net.GatewayDestinationsPayload.Destination;
import com.ascension.worlds.internal.net.GatewayTravelPayload;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The gateway destination picker: one button per planet the network can reach from here.
 *
 * <p>A plain {@link Screen}, not a container menu &mdash; there is no inventory to sync, just a
 * list in and one choice out. The list is the server's ({@code GatewayDestinations.sendPicker});
 * a click only <em>asks</em> to travel ({@link GatewayTravelPayload}), and the server re-checks
 * everything before moving anyone, so nothing drawn here is trusted.
 *
 * <p>Does not pause single-player: the picker session it rides on expires in game time, and a
 * paused world would freeze that clock while the player reads.
 */
public final class GatewayDestinationScreen extends Screen {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_SPACING = 24;

    private final List<Destination> destinations;

    public GatewayDestinationScreen(List<Destination> destinations) {
        super(Component.translatable("block.ascension_worlds.gateway_portal.pick_destination"));
        this.destinations = destinations;
    }

    @Override
    protected void init() {
        int rows = destinations.size() + 1;
        int x = (width - BUTTON_WIDTH) / 2;
        int y = (height - rows * ROW_SPACING) / 2;

        for (Destination destination : destinations) {
            addRenderableWidget(Button.builder(planetName(destination.planet()), button -> travel(destination))
                    .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
            y += ROW_SPACING;
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(x, y + ROW_SPACING / 2, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int firstRowY = (height - (destinations.size() + 1) * ROW_SPACING) / 2;
        graphics.drawCenteredString(font, title, width / 2, firstRowY - 20, 0x55FFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void travel(Destination destination) {
        PacketDistributor.sendToServer(new GatewayTravelPayload(destination.surface()));
        onClose();
    }

    private static Component planetName(ResourceLocation planet) {
        return Component.translatable("planet." + planet.getNamespace() + "." + planet.getPath());
    }
}
