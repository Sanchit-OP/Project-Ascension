package com.ascension.combat.client;

import static com.ascension.combat.client.rune.OrreryLayout.BLURB_X0;
import static com.ascension.combat.client.rune.OrreryLayout.BLURB_X1;
import static com.ascension.combat.client.rune.OrreryLayout.BLURB_Y0;
import static com.ascension.combat.client.rune.OrreryLayout.BLURB_Y1;
import static com.ascension.combat.client.rune.OrreryLayout.PANEL_H;
import static com.ascension.combat.client.rune.OrreryLayout.PANEL_W;
import static com.ascension.combat.client.rune.OrreryLayout.P_CX;
import static com.ascension.combat.client.rune.OrreryLayout.P_CY;
import static com.ascension.combat.client.rune.OrreryLayout.S_CX;
import static com.ascension.combat.client.rune.OrreryLayout.S_CY;

import com.ascension.combat.api.rune.RunePath;
import com.ascension.combat.client.rune.OrreryLayout;
import com.ascension.combat.client.rune.OrreryLayout.Point;
import com.ascension.combat.client.rune.PixelDraw;
import com.ascension.combat.client.rune.RuneCardPainter;
import com.ascension.combat.client.rune.RuneCards;
import com.ascension.combat.client.rune.RuneKit;
import com.ascension.combat.client.rune.RunePageEdit;
import com.ascension.combat.client.rune.RuneSounds;
import com.ascension.combat.internal.net.SetRunePagePayload;
import com.ascension.combat.internal.rune.RuneContent;
import com.ascension.combat.internal.rune.RuneDefinition;
import com.ascension.combat.internal.rune.RunePage;
import com.ascension.combat.internal.rune.RunePageRules;
import com.ascension.combat.internal.rune.RuneRegistries;
import com.ascension.combat.internal.rune.RuneSlot;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The rune screen: the orrery (docs/technical/rune-system.md section 10, Sanchit's pick of
 * 2026-10-17). The primary path is a brass dial with its keystones round the hub and its minors on
 * the outer ring; the secondary path a smaller dial of minors; the shards three rows of moons.
 * Everything is drawn from the art kit's sprites ({@link RuneKit}) at one texel to a GUI pixel on
 * a 400 x 224 panel, and everything it offers is read from the synced rune registry, so it shows
 * exactly what the server will accept.
 *
 * <p>Choices are local until Save ({@link RunePageEdit}); the server answers saved or refused, and
 * the reason shows in the keystone box ({@link #onResult}) without losing the unsaved choices.
 * Choosing a primary path turns the dial a quarter round to it; a chosen rune's spoke draws out
 * from the hub; the chosen keystone's halo breathes. All of it is whole-pixel art and plays no
 * part in the page itself.
 *
 * <p>If a very small window can't fit the panel at the GUI scale, the whole panel is scaled down
 * to fit rather than cut off.
 */
public final class RuneScreen extends Screen {

    private static final long TURN_MS = 250L;
    private static final long SPOKE_MS = 150L;
    private static final long STATUS_MS = 4500L;
    private static final long LONG_AGO = Long.MIN_VALUE / 4;

    private enum Kind { PRIMARY_TAB, SECONDARY_TAB, KEYSTONE, MINOR, SHARD }

    /** Chosen: in the unsaved page. Open: can be chosen. Barred: not allowed. Locked: not opened yet. */
    private enum State { CHOSEN, OPEN, BARRED, LOCKED }

    /** One round socket on the panel: where, what, and what it says when hovered. */
    private record Node(Kind kind, int cx, int cy, State state, RunePath path, ResourceLocation id, RuneDefinition rune,
            boolean primarySide, int shardRow, Component hint) {

        int size() {
            return switch (kind) {
                case KEYSTONE -> OrreryLayout.KEY_SOCKET;
                case SHARD -> OrreryLayout.SHARD_SOCKET;
                default -> OrreryLayout.MINOR_SOCKET;
            };
        }

        boolean contains(double x, double y) {
            return Math.hypot(x - cx, y - cy) <= size() / 2.0 + 1.0;
        }

        boolean chosen() {
            return state == State.CHOSEN;
        }
    }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    /** A rune's id and data, for sorting and laying out. */
    private record Entry(ResourceLocation id, RuneDefinition rune) {
    }

    private final RunePageEdit edit;

    private double scale = 1.0;
    private int originX;
    private int originY;

    private long turnStartedAt = LONG_AGO;
    private double turnFrom;
    private final Map<ResourceLocation, Long> chosenAt = new HashMap<>();

    private Node hovered;
    /** Where the screenshot harness pretends the pointer is, in panel pixels; null in play. */
    private double[] forcedMouse;
    private Node tickedFor;
    private Rect saveRect = new Rect(0, 0, 0, 0);
    private Rect resetRect = new Rect(0, 0, 0, 0);
    private Rect yesRect = new Rect(0, 0, 0, 0);
    private Rect noRect = new Rect(0, 0, 0, 0);

    private Component status = Component.empty();
    private boolean statusBad;
    private long statusUntil;

    /** Set while the player is asked whether to throw away a growing rune's growth. */
    private List<Component> confirm;

    public RuneScreen() {
        super(Component.translatable("screen.ascension_combat.rune_page"));
        Registry<RuneDefinition> runes = runes();
        edit = new RunePageEdit(ClientRunePageCache.page(), runes::get);
        for (ResourceLocation id : ClientRunePageCache.page().runes()) {
            chosenAt.put(id, LONG_AGO);
        }
    }

    private static Registry<RuneDefinition> runes() {
        return Minecraft.getInstance().level.registryAccess().registryOrThrow(RuneRegistries.RUNE);
    }

    @Override
    protected void init() {
        scale = Math.min(1.0, Math.min((this.width - 6.0) / PANEL_W, (this.height - 6.0) / PANEL_H));
        originX = (int) ((this.width - PANEL_W * scale) / 2);
        originY = (int) ((this.height - PANEL_H * scale) / 2);
    }

    /**
     * For {@code client.dev.RuneScreenshots} only: draws the screen as if the pointer were at this
     * panel position, so a hover card can be photographed without moving anyone's mouse.
     */
    public void devHover(double panelX, double panelY) {
        forcedMouse = new double[] {panelX, panelY};
    }

    /** For {@code client.dev.RuneScreenshots} only: a left click at a panel position. */
    public void devClick(double panelX, double panelY) {
        mouseClicked(originX + panelX * scale, originY + panelY * scale, 0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    // What is on the panel
    // ---------------------------------------------------------------------------------------------

    private static List<Entry> of(Registry<RuneDefinition> runes, Predicate<RuneDefinition> keep) {
        List<Entry> out = new ArrayList<>();
        for (var e : runes.entrySet()) {
            if (keep.test(e.getValue())) {
                out.add(new Entry(e.getKey().location(), e.getValue()));
            }
        }
        out.sort(Comparator.comparingInt((Entry e) -> e.rune().position()).thenComparing(e -> e.id().toString()));
        return out;
    }

    private static boolean ofPath(RuneDefinition rune, RuneSlot slot, RunePath path) {
        return rune.slot() == slot && rune.path().orElse(null) == path;
    }

    private Component milestone(int number) {
        return Component.translatable("gui.ascension_combat.opens_at", Component.translatable("gui.ascension_combat.ordinal." + number));
    }

    /** Every socket on the panel for this frame, in the order they are drawn. */
    private List<Node> buildNodes(Registry<RuneDefinition> runes, double turn) {
        List<Node> nodes = new ArrayList<>();
        RunePath primary = edit.primary().orElse(null);
        RunePath secondary = edit.secondary().orElse(null);
        RunePageRules.OpenSlots open = ClientRunePageCache.open();

        RunePath[] paths = RunePath.values();
        for (int i = 0; i < paths.length; i++) {
            Point p = OrreryLayout.primaryTab(i);
            nodes.add(new Node(Kind.PRIMARY_TAB, p.x(), p.y(), paths[i] == primary ? State.CHOSEN : State.OPEN, paths[i],
                    null, null, true, 0, null));
        }

        if (primary != null) {
            int combat = 0;
            for (Entry e : of(runes, r -> ofPath(r, RuneSlot.KEYSTONE, primary))) {
                if (!e.rune().world() && combat > 1) {
                    continue;
                }

                Point p = OrreryLayout.primaryKeystone(e.rune().world() ? 0 : combat, e.rune().world(), turn);
                combat += e.rune().world() ? 0 : 1;
                nodes.add(new Node(Kind.KEYSTONE, p.x(), p.y(), chosen(edit.keystone(), e.id()), primary, e.id(), e.rune(), true, 0, null));
            }
            for (Entry e : of(runes, r -> ofPath(r, RuneSlot.MINOR, primary))) {
                Point p = OrreryLayout.primaryMinor(e.rune().row(), e.rune().position(), turn);
                nodes.add(new Node(Kind.MINOR, p.x(), p.y(), chosen(edit.minor(true, e.rune().row()), e.id()), primary, e.id(), e.rune(), true, 0, null));
            }
        }

        int tab = 0;
        for (RunePath path : paths) {
            if (path == primary || primary == null) {
                continue;
            }

            Point p = OrreryLayout.secondaryTab(tab++);
            State state = !open.secondary() ? State.BARRED : path == secondary ? State.CHOSEN : State.OPEN;
            nodes.add(new Node(Kind.SECONDARY_TAB, p.x(), p.y(), state, path, null, null, false, 0,
                    open.secondary() ? null : milestone(1)));
        }

        if (!open.secondary()) {
            for (int row = 0; row < 2; row++) {
                for (int place = 0; place < 3; place++) {
                    Point p = OrreryLayout.secondaryMinor(row, place);
                    nodes.add(new Node(Kind.MINOR, p.x(), p.y(), State.LOCKED, null, null, null, false, 0, milestone(1)));
                }
            }
        } else if (secondary != null) {
            for (Entry e : of(runes, r -> ofPath(r, RuneSlot.MINOR, secondary))) {
                Point p = OrreryLayout.secondaryMinor(e.rune().row(), e.rune().position());
                nodes.add(new Node(Kind.MINOR, p.x(), p.y(), chosen(edit.minor(false, e.rune().row()), e.id()), secondary, e.id(), e.rune(), false, 0, null));
            }
        }

        for (int row = 0; row < RuneDefinition.SHARD_ROWS; row++) {
            int shardRow = row;
            if (!open.shardRow(row)) {
                for (int place = 0; place < 3; place++) {
                    Point p = OrreryLayout.shardMoon(row, place);
                    nodes.add(new Node(Kind.SHARD, p.x(), p.y(), State.LOCKED, null, null, null, true, row, milestone(row + 2)));
                }
                continue;
            }

            for (Entry e : of(runes, r -> r.slot() == RuneSlot.SHARD && r.row() == shardRow)) {
                Point p = OrreryLayout.shardMoon(row, e.rune().position());
                nodes.add(new Node(Kind.SHARD, p.x(), p.y(), chosen(edit.shard(row), e.id()), null, e.id(), e.rune(), true, row, null));
            }
        }
        return nodes;
    }

    private static State chosen(Optional<ResourceLocation> pick, ResourceLocation id) {
        return pick.filter(id::equals).isPresent() ? State.CHOSEN : State.OPEN;
    }

    private Node nodeAt(List<Node> nodes, double x, double y) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            if (nodes.get(i).contains(x, y)) {
                return nodes.get(i);
            }
        }
        return null;
    }

    private boolean dirty() {
        return !edit.toPage().sameAs(ClientRunePageCache.page());
    }

    private double turnNow(long now) {
        double t = (now - turnStartedAt) / (double) TURN_MS;
        if (t >= 1.0) {
            return 0.0;
        }

        double eased = 1.0 - Math.pow(1.0 - Math.max(0.0, t), 3);
        return turnFrom * (1.0 - eased);
    }

    // ---------------------------------------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        Registry<RuneDefinition> runes = runes();
        long now = Util.getMillis();
        double turn = turnNow(now);
        List<Node> nodes = buildNodes(runes, turn);
        double lx = forcedMouse != null ? forcedMouse[0] : (mouseX - originX) / scale;
        double ly = forcedMouse != null ? forcedMouse[1] : (mouseY - originY) / scale;
        hovered = confirm != null ? null : nodeAt(nodes, lx, ly);
        if (hovered != null && forcedMouse == null && !sameNode(hovered, tickedFor)) {
            RuneSounds.hover();
        }
        tickedFor = hovered;

        graphics.pose().pushPose();
        graphics.pose().translate(originX, originY, 0);
        graphics.pose().scale((float) scale, (float) scale, 1.0f);
        RuneKit.blit(graphics, RuneKit.sprite("panel"), 0, 0, PANEL_W, PANEL_H);
        drawTitleBar(graphics, lx, ly);
        drawPrimary(graphics, nodes, now, turn);
        drawSecondary(graphics, nodes, now);
        drawShards(graphics, nodes);
        drawKeystoneBox(graphics, runes, now);
        for (Node node : nodes) {
            if (node == hovered) {
                drawHoverRing(graphics, node);
            }
        }
        graphics.pose().popPose();

        if (confirm != null) {
            drawConfirm(graphics, lx, ly);
        } else if (hovered != null) {
            drawCard(graphics, runes, hovered);
        }
    }

    private static boolean sameNode(Node a, Node b) {
        return b != null && a.kind() == b.kind() && a.cx() == b.cx() && a.cy() == b.cy() && java.util.Objects.equals(a.id(), b.id());
    }

    private void drawTitleBar(GuiGraphics g, double lx, double ly) {
        g.drawString(this.font, Component.translatable("screen.ascension_combat.rune_page").withStyle(ChatFormatting.BOLD),
                9, 7, RuneKit.BRASS_HIGH, true);
        Component save = Component.translatable("gui.ascension_combat.rune_save");
        Component reset = Component.translatable("gui.ascension_combat.rune_reset");
        boolean dirty = dirty();
        int saveW = this.font.width(save) + 14;
        int resetW = this.font.width(reset) + 14;
        saveRect = new Rect(PANEL_W - 8 - saveW, 5, saveW, 12);
        resetRect = new Rect(saveRect.x() - 4 - resetW, 5, resetW, 12);
        drawButton(g, saveRect, save, dirty, true, lx, ly);
        drawButton(g, resetRect, reset, dirty, false, lx, ly);
        if (dirty) {
            drawCost(g, resetRect.x() - 4);
        }
    }

    private void drawButton(GuiGraphics g, Rect r, Component label, boolean enabled, boolean primary, double lx, double ly) {
        boolean over = enabled && confirm == null && r.contains(lx, ly);
        String sprite = !enabled ? "button/disabled" : primary ? (over ? "button/primary_hover" : "button/primary")
                : (over ? "button/hover" : "button/normal");
        RuneKit.blit(g, RuneKit.sprite(sprite), r.x(), r.y(), r.w(), r.h());
        int colour = !enabled ? RuneKit.TEXT_FAINT : primary ? RuneKit.TEXT : RuneKit.TEXT_BODY;
        g.drawString(this.font, label, r.x() + (r.w() - this.font.width(label)) / 2, r.y() + 2, colour, true);
    }

    /** What this save costs, left of the buttons: Rune Chalk and levels (red if short), or "free". */
    private void drawCost(GuiGraphics g, int rightEdge) {
        if (!RunePageRules.costsToChange(ClientRunePageCache.page(), edit.toPage())) {
            Component free = Component.translatable("gui.ascension_combat.free");
            g.drawString(this.font, free, rightEdge - this.font.width(free) - 4, 7, RuneKit.TEXT_DIM, true);
            return;
        }

        var player = Minecraft.getInstance().player;
        boolean creative = player != null && player.getAbilities().instabuild;
        int haveChalk = player == null ? 0 : player.getInventory().countItem(RuneContent.RUNE_CHALK.get());
        int haveLevels = player == null ? 0 : player.experienceLevel;
        int cx = rightEdge - 64;
        g.pose().pushPose();
        g.pose().translate(cx, 6, 0);
        g.pose().scale(0.5f, 0.5f, 1.0f);
        g.renderItem(new ItemStack(RuneContent.RUNE_CHALK.get()), 0, 0);
        g.pose().popPose();
        g.drawString(this.font, "×" + ClientRunePageCache.chalk(), cx + 10, 7,
                creative || haveChalk >= ClientRunePageCache.chalk() ? RuneKit.TEXT_BODY : RuneKit.BAD, true);
        RuneKit.blit(g, RuneKit.sprite("icon/xp_orb"), cx + 26, 7, 8, 8);
        g.drawString(this.font, Component.translatable("gui.ascension_combat.levels_short", ClientRunePageCache.levels()),
                cx + 36, 7, creative || haveLevels >= ClientRunePageCache.levels() ? RuneKit.XP : RuneKit.BAD, true);
    }

    private void drawPrimary(GuiGraphics g, List<Node> nodes, long now, double turn) {
        RunePath primary = edit.primary().orElse(null);
        if (primary != null) {
            RuneKit.blit(g, RuneKit.sprite("nebula/" + primary.getSerializedName()), P_CX - 90, P_CY - 90, 180, 180);
        }

        g.pose().pushPose();
        g.pose().translate(P_CX, P_CY, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees((float) turn));
        RuneKit.blit(g, RuneKit.dial(primary, true), -90, -90, 180, 180);
        g.pose().popPose();

        float arrival = turn == 0.0 ? 1.0f : (float) (0.25 + 0.75 * (1.0 - Math.abs(turn) / 90.0));
        drawSpokes(g, nodes, now, P_CX, P_CY, true, primary);
        RuneKit.blit(g, RuneKit.sprite(primary == null ? "hub/none" : "hub/" + primary.getSerializedName()),
                P_CX - 14, P_CY - 14, 28, 28);
        g.setColor(1.0f, 1.0f, 1.0f, arrival);
        for (Node node : nodes) {
            if (node.kind() == Kind.KEYSTONE || (node.kind() == Kind.MINOR && node.primarySide())) {
                drawNode(g, node, now);
            }
        }
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        for (Node node : nodes) {
            if (node.kind() == Kind.PRIMARY_TAB) {
                drawNode(g, node, now);
            }
        }

        Component name = primary == null
                ? Component.translatable("gui.ascension_combat.choose_primary_prompt")
                : Component.literal(I18n.get("rune_path.ascension_combat." + primary.getSerializedName())
                        .toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD);
        g.drawString(this.font, name, P_CX - this.font.width(name) / 2, 210,
                primary == null ? RuneKit.TEXT_FAINT : RuneKit.ramp(primary, 3), true);
    }

    private void drawSecondary(GuiGraphics g, List<Node> nodes, long now) {
        RunePath secondary = edit.secondary().orElse(null);
        boolean unlocked = ClientRunePageCache.open().secondary();
        int tx = OrreryLayout.SECONDARY_TAB_X;
        g.drawString(this.font, Component.translatable("gui.ascension_combat.secondary_path_label"), tx, 26, RuneKit.TEXT_DIM, true);

        if (unlocked && secondary != null) {
            RuneKit.blit(g, RuneKit.sprite("nebula/" + secondary.getSerializedName() + "_small"), S_CX - 55, S_CY - 55, 110, 110);
            RuneKit.blit(g, RuneKit.dial(secondary, false), S_CX - 54, S_CY - 54, 108, 108);
            drawSpokes(g, nodes, now, S_CX, S_CY, false, secondary);
            RuneKit.blit(g, RuneKit.sprite("hub/" + secondary.getSerializedName() + "_small"), S_CX - 11, S_CY - 11, 22, 22);
        } else {
            RuneKit.blit(g, RuneKit.sprite("dial/secondary_locked"), S_CX - 54, S_CY - 54, 108, 108);
            RuneKit.blit(g, RuneKit.sprite(unlocked ? "hub/none_small" : "hub/locked"), S_CX - 11, S_CY - 11, 22, 22);
        }
        for (Node node : nodes) {
            if (node.kind() == Kind.MINOR && !node.primarySide() || node.kind() == Kind.SECONDARY_TAB) {
                drawNode(g, node, now);
            }
        }

        if (!unlocked) {
            g.drawString(this.font, Component.literal(I18n.get("gui.ascension_combat.locked").toUpperCase(Locale.ROOT))
                    .withStyle(ChatFormatting.BOLD), tx, 36, RuneKit.TEXT_FAINT, true);
            paragraph(g, milestone(1), tx, 78, PANEL_W - 8 - tx, RuneKit.TEXT_DIM, 120);
        } else if (secondary != null) {
            g.drawString(this.font, Component.literal(I18n.get("rune_path.ascension_combat." + secondary.getSerializedName())
                    .toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD), tx, 36, RuneKit.ramp(secondary, 3), true);
            int y = paragraph(g, Component.translatable("rune_path.ascension_combat." + secondary.getSerializedName() + ".promise"),
                    tx, 76, PANEL_W - 8 - tx, RuneKit.TEXT_BODY, 130);
            paragraph(g, Component.translatable("gui.ascension_combat.one_from_each_half"), tx, y + 4, PANEL_W - 8 - tx,
                    RuneKit.TEXT_FAINT, 150);
        } else {
            paragraph(g, Component.translatable(edit.primary().isPresent()
                    ? "gui.ascension_combat.choose_secondary" : "gui.ascension_combat.choose_primary_first"),
                    tx, 78, PANEL_W - 8 - tx, RuneKit.TEXT_DIM, 120);
        }
    }

    private void drawShards(GuiGraphics g, List<Node> nodes) {
        g.drawString(this.font, Component.translatable("gui.ascension_combat.rune_shards_label"),
                OrreryLayout.SHARD_LABEL_X, OrreryLayout.SHARD_LABEL_Y, RuneKit.TEXT_DIM, true);
        for (Node node : nodes) {
            if (node.kind() == Kind.SHARD) {
                drawNode(g, node, 0L);
            }
        }
        for (int row = 0; row < RuneDefinition.SHARD_ROWS; row++) {
            Component label = Component.translatable("gui.ascension_combat.shard_row." + row);
            Point c = OrreryLayout.shardRowLabel(row);
            g.drawString(this.font, label, c.x() - this.font.width(label) / 2, c.y(), RuneKit.TEXT_FAINT, true);
        }
    }

    private void drawKeystoneBox(GuiGraphics g, Registry<RuneDefinition> runes, long now) {
        g.fill(BLURB_X0, BLURB_Y0, BLURB_X1, BLURB_Y0 + 1, RuneKit.withAlpha(RuneKit.BRASS_LOW, 160));
        g.fill(BLURB_X0, BLURB_Y0 + 1, BLURB_X1, BLURB_Y1, 0x78040610);
        int x = BLURB_X0 + 5;
        int width = BLURB_X1 - BLURB_X0 - 10;
        if (now < statusUntil) {
            paragraph(g, status, x, BLURB_Y0 + 5, width, statusBad ? RuneKit.BAD : RuneKit.GOOD, BLURB_Y1);
            return;
        }

        Optional<ResourceLocation> keystone = edit.keystone();
        RuneDefinition rune = keystone.map(runes::get).orElse(null);
        if (rune == null) {
            paragraph(g, Component.translatable(edit.primary().isPresent()
                    ? "gui.ascension_combat.pick_keystone" : "gui.ascension_combat.pick_path_first"),
                    x, BLURB_Y0 + 5, width, RuneKit.TEXT_DIM, BLURB_Y1);
            return;
        }

        RunePath path = rune.path().orElse(null);
        Component name = Component.translatable(RuneCards.nameKey(keystone.get())).withStyle(ChatFormatting.BOLD);
        g.drawString(this.font, name, x, BLURB_Y0 + 5, RuneKit.ramp(path, 4), true);
        g.drawString(this.font, Component.translatable(rune.world() ? "gui.ascension_combat.blurb_world" : "gui.ascension_combat.blurb_keystone"),
                x + 4 + this.font.width(name), BLURB_Y0 + 5, RuneKit.TEXT_FAINT, true);
        String blurb = RuneCards.nameKey(keystone.get()) + ".blurb";
        paragraph(g, Component.translatable(I18n.exists(blurb) ? blurb : RuneCards.nameKey(keystone.get()) + ".desc"),
                x, BLURB_Y0 + 17, width, RuneKit.TEXT_BODY, BLURB_Y1);
    }

    private int paragraph(GuiGraphics g, Component text, int x, int y, int width, int colour, int maxY) {
        for (FormattedCharSequence line : this.font.split(text, width)) {
            if (y + 8 > maxY) {
                break;
            }

            g.drawString(this.font, line, x, y, colour, true);
            y += 10;
        }
        return y;
    }

    private void drawSpokes(GuiGraphics g, List<Node> nodes, long now, int hubX, int hubY, boolean primarySide, RunePath path) {
        Set<ResourceLocation> live = new HashSet<>();
        for (Node node : nodes) {
            if (node.chosen() && node.id() != null && node.primarySide() == primarySide
                    && (node.kind() == Kind.KEYSTONE || node.kind() == Kind.MINOR)) {
                live.add(node.id());
                long since = chosenAt.computeIfAbsent(node.id(), k -> now);
                double reach = Math.min(1.0, (now - since) / (double) SPOKE_MS);
                int tx = hubX + (int) Math.round((node.cx() - hubX) * reach);
                int ty = hubY + (int) Math.round((node.cy() - hubY) * reach);
                PixelDraw.line(g, hubX, hubY, tx, ty, RuneKit.withAlpha(RuneKit.ramp(path, 3), 200));
                PixelDraw.line(g, hubX + 1, hubY, tx + 1, ty, RuneKit.withAlpha(RuneKit.ramp(path, 2), 90));
            }
        }
        if (primarySide) {
            chosenAt.keySet().removeIf(id -> !live.contains(id) && !chosenElsewhere(nodes, id));
        }
    }

    private static boolean chosenElsewhere(List<Node> nodes, ResourceLocation id) {
        for (Node node : nodes) {
            if (id.equals(node.id()) && node.chosen()) {
                return true;
            }
        }
        return false;
    }

    private void drawNode(GuiGraphics g, Node node, long now) {
        int size = node.size();
        int x = node.cx() - size / 2;
        int y = node.cy() - size / 2;
        switch (node.kind()) {
            case PRIMARY_TAB, SECONDARY_TAB -> {
                boolean barred = node.state() == State.BARRED;
                RuneKit.blit(g, barred ? RuneKit.sprite("socket/minor_locked") : RuneKit.minorSocket(node.path(), node.chosen()),
                        x, y, size, size);
                String emblem = node.chosen() ? "chosen" : barred ? "barred" : "open";
                RuneKit.blit(g, RuneKit.tabEmblem(node.path(), emblem), x + 4, y + 4, 14, 14);
            }
            case KEYSTONE -> {
                if (node.chosen()) {
                    double breath = 0.45 + 0.25 * Math.sin(now / 700.0 * Math.PI);
                    int colour = RuneKit.ramp(node.path(), 3);
                    g.setColor(((colour >> 16) & 0xFF) / 255f, ((colour >> 8) & 0xFF) / 255f, (colour & 0xFF) / 255f, (float) breath);
                    RuneKit.blit(g, RuneKit.sprite("socket/halo_key"), node.cx() - 19, node.cy() - 19, 38, 38);
                    g.setColor(1f, 1f, 1f, 1f);
                }
                RuneKit.blit(g, RuneKit.keystoneSocket(node.path(), node.rune().world(), node.chosen()), x, y, size, size);
                RuneKit.blit(g, RuneKit.glyph(node.rune().icon(), node.chosen()), node.cx() - 10, node.cy() - 10, 20, 20);
            }
            case MINOR -> {
                if (node.state() == State.LOCKED) {
                    RuneKit.blit(g, RuneKit.sprite("socket/minor_locked"), x, y, size, size);
                    return;
                }
                RuneKit.blit(g, RuneKit.minorSocket(node.path(), node.chosen()), x, y, size, size);
                RuneKit.blit(g, RuneKit.glyph(node.rune().icon(), node.chosen()), node.cx() - 7, node.cy() - 7, 14, 14);
                if (node.chosen() && node.rune().grows()) {
                    drawGrowthRing(g, node);
                }
            }
            case SHARD -> {
                if (node.state() == State.LOCKED) {
                    RuneKit.blit(g, RuneKit.sprite("socket/shard_locked"), x, y, size, size);
                    RuneKit.blit(g, RuneKit.sprite("icon/padlock"), x + 4, y + 3, 7, 9);
                    return;
                }
                String stat = node.rune().icon().getPath().substring(node.rune().icon().getPath().lastIndexOf('/') + 1);
                RuneKit.blit(g, RuneKit.shardSocket(stat, node.chosen()), x, y, size, size);
                RuneKit.blit(g, RuneKit.shardGlyph(stat, node.chosen()), x + 4, y + 4, 8, 8);
            }
        }
    }

    /** The ring round a growing rune's socket, filled clockwise from the top as it grows. */
    private void drawGrowthRing(GuiGraphics g, Node node) {
        int cap = node.rune().growthCap().orElse(1);
        double filled = Math.min(1.0, ClientRunePageCache.growth(node.id()) / (double) cap) * 360.0;
        int on = RuneKit.ramp(node.path(), 4);
        int off = RuneKit.withAlpha(RuneKit.ramp(node.path(), 0), 200);
        for (OrreryLayout.RingPixel pixel : OrreryLayout.growthRing()) {
            int px = node.cx() + pixel.dx();
            int py = node.cy() + pixel.dy();
            g.fill(px, py, px + 1, py + 1, pixel.angle() <= filled ? on : off);
        }
    }

    private void drawHoverRing(GuiGraphics g, Node node) {
        String name = switch (node.kind()) {
            case KEYSTONE -> "socket/hover_key";
            case SHARD -> "socket/hover_shard";
            default -> "socket/hover_minor";
        };
        int size = node.size() + 6;
        RuneKit.blit(g, RuneKit.sprite(name), node.cx() - size / 2, node.cy() - size / 2, size, size);
    }

    // ---------------------------------------------------------------------------------------------
    // The hover card, and the question before a swap throws growth away
    // ---------------------------------------------------------------------------------------------

    private void drawCard(GuiGraphics g, Registry<RuneDefinition> runes, Node node) {
        List<List<FormattedCharSequence>> blocks = new ArrayList<>();
        Component title;
        Component tag = Component.empty();
        RunePath path = node.path();
        int inner = RuneCardPainter.TEXT_WIDTH;
        if (node.rune() == null && node.kind() != Kind.PRIMARY_TAB && node.kind() != Kind.SECONDARY_TAB) {
            title = Component.translatable("gui.ascension_combat.locked");
            blocks.add(this.font.split(node.hint() == null ? Component.empty() : node.hint(), inner));
        } else if (node.kind() == Kind.PRIMARY_TAB || node.kind() == Kind.SECONDARY_TAB) {
            String key = "rune_path.ascension_combat." + node.path().getSerializedName();
            title = Component.translatable(key);
            tag = Component.translatable(node.kind() == Kind.PRIMARY_TAB
                    ? "gui.ascension_combat.primary_path" : "gui.ascension_combat.secondary_path");
            blocks.add(this.font.split(Component.translatable(key + ".promise"), inner));
            if (node.hint() != null) {
                blocks.add(this.font.split(node.hint(), inner));
            }
        } else {
            RuneCards.Card card = RuneCards.of(node.id(), node.rune(), ClientRunePageCache.page(), node.primarySide(),
                    ClientRunePageCache.growth(node.id()), ClientRunePageCache.chalk(), ClientRunePageCache.levels(), runes::get);
            title = card.title();
            tag = card.tag();
            blocks.add(this.font.split(card.body(), inner));
            List<FormattedCharSequence> numbers = new ArrayList<>();
            card.numbers().forEach(n -> numbers.addAll(this.font.split(n, inner)));
            card.growth().ifPresent(n -> numbers.addAll(this.font.split(n, inner)));
            if (!numbers.isEmpty()) {
                blocks.add(numbers);
            }
            card.fine().ifPresent(n -> blocks.add(this.font.split(n, inner)));
        }

        RuneCardPainter.paint(g, this.font, this.width, this.height, originX + node.cx() * scale, originY + node.cy() * scale,
                scale, path, title, tag, blocks);
    }

    private void drawConfirm(GuiGraphics g, double lx, double ly) {
        g.pose().pushPose();
        g.pose().translate(originX, originY, 300);
        g.pose().scale((float) scale, (float) scale, 1.0f);
        g.fill(0, 0, PANEL_W, PANEL_H, 0xB0000000);
        int w = 230;
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component line : confirm) {
            lines.addAll(this.font.split(line, w - 16));
        }
        int h = 14 + lines.size() * 10 + 8 + 14 + 8;
        int x = (PANEL_W - w) / 2;
        int y = (PANEL_H - h) / 2;
        RuneKit.blit(g, RuneKit.card(edit.primary().orElse(null)), x, y, w, h);
        int yy = y + 8;
        for (FormattedCharSequence line : lines) {
            g.drawString(this.font, line, x + 8, yy, RuneKit.TEXT_BODY, true);
            yy += 10;
        }
        Component keep = Component.translatable("gui.ascension_combat.keep_it");
        Component swap = Component.translatable("gui.ascension_combat.swap_anyway");
        int keepW = this.font.width(keep) + 14;
        int swapW = this.font.width(swap) + 14;
        noRect = new Rect(x + w - 8 - swapW - 6 - keepW, yy + 4, keepW, 12);
        yesRect = new Rect(x + w - 8 - swapW, yy + 4, swapW, 12);
        drawButton(g, noRect, keep, true, true, lx, ly);
        drawButton(g, yesRect, swap, true, false, lx, ly);
        g.pose().popPose();
    }

    // ---------------------------------------------------------------------------------------------
    // Input
    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        double lx = (mouseX - originX) / scale;
        double ly = (mouseY - originY) / scale;
        if (confirm != null) {
            if (yesRect.contains(lx, ly)) {
                confirm = null;
                send();
            } else if (noRect.contains(lx, ly)) {
                confirm = null;
            }
            return true;
        }

        if (saveRect.contains(lx, ly)) {
            if (dirty()) {
                onSave();
            }
            return true;
        }

        if (resetRect.contains(lx, ly)) {
            if (dirty()) {
                edit.load(ClientRunePageCache.page(), runes()::get);
                status = Component.empty();
                statusUntil = 0L;
            }
            return true;
        }

        Registry<RuneDefinition> runes = runes();
        Node node = nodeAt(buildNodes(runes, turnNow(Util.getMillis())), lx, ly);
        if (node == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        click(node);
        return true;
    }

    private void click(Node node) {
        switch (node.kind()) {
            case PRIMARY_TAB -> {
                RunePath before = edit.primary().orElse(null);
                if (edit.choosePrimary(node.path())) {
                    turnFrom = before == null || node.path().ordinal() > before.ordinal() ? 90.0 : -90.0;
                    turnStartedAt = Util.getMillis();
                    RuneSounds.choose(node.path(), false);
                }
            }
            case SECONDARY_TAB -> {
                if (node.state() == State.BARRED) {
                    refuse(node.hint());
                } else if (edit.chooseSecondary(node.path())) {
                    RuneSounds.choose(node.path(), false);
                }
            }
            case KEYSTONE -> {
                edit.toggleKeystone(node.id());
                RuneSounds.choose(node.path(), true);
            }
            case MINOR -> {
                if (node.state() == State.LOCKED) {
                    refuse(node.hint());
                } else {
                    edit.toggleMinor(node.primarySide(), node.rune().row(), node.id());
                    RuneSounds.choose(node.path(), false);
                }
            }
            case SHARD -> {
                if (node.state() == State.LOCKED) {
                    refuse(node.hint());
                } else {
                    edit.toggleShard(node.shardRow(), node.id());
                    RuneSounds.choose(RunePath.VANGUARD, false);
                }
            }
        }
        status = Component.empty();
        statusUntil = 0L;
    }

    private void refuse(Component why) {
        RuneSounds.refused();
        if (why != null) {
            say(why, true);
        }
    }

    private void say(Component text, boolean bad) {
        status = text;
        statusBad = bad;
        statusUntil = Util.getMillis() + STATUS_MS;
    }

    /** Save: first asks whether to throw away the growth of a growing rune this would take off the page. */
    private void onSave() {
        RunePage saved = ClientRunePageCache.page();
        RunePage pending = edit.toPage();
        List<Component> lost = new ArrayList<>();
        Registry<RuneDefinition> runes = runes();
        for (ResourceLocation id : saved.runes()) {
            RuneDefinition rune = runes.get(id);
            int grown = ClientRunePageCache.growth(id);
            if (rune != null && rune.grows() && grown > 0 && !pending.runes().contains(id)) {
                lost.add(Component.translatable("gui.ascension_combat.lose_growth",
                        Component.translatable(RuneCards.nameKey(id)), grown, rune.growthCap().orElse(0)));
            }
        }
        if (lost.isEmpty()) {
            send();
        } else {
            confirm = lost;
        }
    }

    private void send() {
        PacketDistributor.sendToServer(new SetRunePagePayload(edit.toPage()));
    }

    /** The server's answer to a save. */
    public void onResult(Optional<RunePageRules.Refusal> refusal) {
        if (refusal.isPresent()) {
            RuneSounds.refused();
            say(Component.translatable(refusal.get().langKey()), true);
        } else {
            RuneSounds.saved();
            say(Component.translatable("gui.ascension_combat.rune_saved"), false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (confirm != null) {
            if (keyCode == 256) {
                confirm = null;
            }
            return true;
        }

        if (CombatKeybinds.isOpenRunePageKey(keyCode, scanCode)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
