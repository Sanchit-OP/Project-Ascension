package com.ascension.combat.client;

import com.ascension.combat.internal.growth.GrowthMilestone;
import com.ascension.combat.internal.growth.GrowthNode;
import com.ascension.combat.internal.growth.GrowthRespecPayload;
import com.ascension.combat.internal.growth.GrowthRules;
import com.ascension.combat.internal.growth.GrowthStat;
import com.ascension.combat.internal.growth.GrowthTakePayload;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The skill tree (docs/progression/growth.md section 10): the web of nodes drawn from the synced
 * {@code growth_node} registry, a side panel of points, sources and stats, and the two buttons.
 * Click a node to mark it, click it again to unmark, Confirm spends everything marked in one go. The
 * layout is the approved mockup ({@code design/growth/tree.png}); the textures are
 * {@code tools/generate-growth-art.py}'s.
 *
 * <p>Everything the screen knows about the player comes from {@link ClientGrowth}'s snapshot, replaced
 * whole by the server whenever anything changes, so this never guesses and never holds the player or
 * the level: it reads the registries on opening and the snapshot each frame. The marks are local until
 * Confirm, and the server checks every rule again ({@code PlayerGrowth.takeAll}), so a click that the
 * server refuses is told so and the screen simply shows what the server says.
 *
 * <p>Drawn at a fixed 400 by 224 and shrunk to fit a smaller window, never grown, so it reads the same
 * at every GUI scale.
 */
public final class GrowthScreen extends Screen {

    private static final int W = GrowthSprites.PANEL_W;
    private static final int H = GrowthSprites.PANEL_H;
    private static final int CENTRE_X = 135;
    private static final int CENTRE_Y = 121;
    private static final double UNIT = 21.5;
    private static final long RESPEC_SURE_MS = 3000;
    private static final long STATUS_MS = 2500;
    private static final int MAX_SOURCE_LINES = 6;
    private static final int MAX_STAT_LINES = 6;

    /** A node as the screen draws it. */
    private record View(ResourceLocation id, GrowthNode node, int theme, boolean notable, int glyph, double x, double y) {

        int size() {
            return notable ? GrowthSprites.NOTABLE : GrowthSprites.SMALL;
        }

        int[] ramp() {
            return GrowthDisplay.ramp(node.arm());
        }
    }

    private record Edge(View a, View b) {
    }

    private record Source(Component title, int points) {
    }

    private record StatLine(String number, Component name, int colour) {
    }

    private final List<View> views = new ArrayList<>();
    private final Map<ResourceLocation, View> byId = new HashMap<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<ResourceLocation, Integer> pointsOf = new HashMap<>();
    private final Map<ResourceLocation, List<GrowthStat>> milestoneStats = new HashMap<>();
    private int allMilestonePoints;
    private GrowthSelection selection;

    private ClientGrowth.Snapshot shown;
    private Set<ResourceLocation> taken = Set.of();
    private List<Source> sources = List.of();
    private List<StatLine> stats = List.of();

    private float scale = 1f;
    private int originX;
    private int originY;
    private View hovered;
    private long respecArmedAt = -1;
    private Component status = Component.empty();
    private long statusAt;

    // Buttons, in panel pixels; set each frame from the labels' widths.
    private int confirmX, confirmW, respecX, respecW;
    private static final int BUTTON_Y = 3;
    private static final int BUTTON_H = 12;

    public GrowthScreen() {
        super(Component.translatable("growth.ascension_combat.screen.title"));
    }

    @Override
    protected void init() {
        views.clear();
        byId.clear();
        edges.clear();
        pointsOf.clear();
        milestoneStats.clear();
        allMilestonePoints = 0;
        shown = null;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            onClose();
            return;
        }
        Registry<GrowthNode> nodes = level.registryAccess().registry(GrowthNode.REGISTRY).orElse(null);
        if (nodes != null) {
            nodes.entrySet().forEach(entry -> addView(entry.getKey().location(), entry.getValue()));
        }
        level.registryAccess().registry(GrowthMilestone.REGISTRY).ifPresent(registry -> registry.forEach(milestone -> {
            pointsOf.merge(milestone.advancement(), milestone.points(), Integer::sum);
            milestoneStats.computeIfAbsent(milestone.advancement(), id -> new ArrayList<>()).addAll(milestone.stats());
            allMilestonePoints += milestone.points();
        }));

        Map<ResourceLocation, List<ResourceLocation>> links = new HashMap<>();
        Set<ResourceLocation> starts = new HashSet<>();
        Set<String> drawn = new HashSet<>();
        for (View view : views) {
            links.put(view.id(), view.node().links());
            if (view.node().start()) {
                starts.add(view.id());
            }
        }
        for (View view : views) {
            for (ResourceLocation link : view.node().links()) {
                View other = byId.get(link);
                String key = view.id().compareTo(link) < 0 ? view.id() + "|" + link : link + "|" + view.id();
                if (other != null && drawn.add(key)) {
                    edges.add(new Edge(view, other));
                }
            }
        }
        selection = new GrowthSelection(starts, GrowthRules.neighbours(links));

        scale = Math.min(1f, Math.min((width - 4f) / W, (height - 4f) / H));
        originX = Math.round((width - W * scale) / 2f);
        originY = Math.round((height - H * scale) / 2f);
    }

    private void addView(ResourceLocation id, GrowthNode node) {
        int glyph = node.stats().isEmpty() ? GrowthDisplay.NO_GLYPH : GrowthDisplay.glyph(node.stats().get(0).attribute());
        View view = new View(id, node, node.arm().ordinal(), node.kind() == GrowthNode.Kind.NOTABLE, glyph,
                CENTRE_X + node.position().x() * UNIT, CENTRE_Y + node.position().y() * UNIT);
        views.add(view);
        byId.put(id, view);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (GrowthKeybind.OPEN.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ------------------------------------------------------------------------------------ state

    /** Takes in a new snapshot from the server, if there is one: marks follow what is now true. */
    private void syncState() {
        ClientGrowth.Snapshot now = ClientGrowth.get();
        if (now == shown) {
            return;
        }
        shown = now;
        taken = new HashSet<>(now.taken());
        selection.reconcile(taken, now.available());

        List<Source> list = new ArrayList<>();
        List<GrowthStat> all = new ArrayList<>();
        for (ResourceLocation advancement : now.milestones()) {
            list.add(new Source(titleOf(advancement), pointsOf.getOrDefault(advancement, 0)));
            all.addAll(milestoneStats.getOrDefault(advancement, List.of()));
        }
        sources = list;
        for (ResourceLocation node : now.taken()) {
            View view = byId.get(node);
            if (view != null) {
                all.addAll(view.node().stats());
            }
        }
        List<StatLine> lines = new ArrayList<>();
        for (Map.Entry<GrowthStat.StatKey, Double> total : GrowthRules.sum(all).entrySet()) {
            ResourceLocation attribute = total.getKey().attribute();
            lines.add(new StatLine(GrowthDisplay.number(attribute, total.getKey().operation(), total.getValue()),
                    GrowthDisplay.name(attribute), GrowthDisplay.colour(attribute)));
        }
        stats = lines;
    }

    private Component titleOf(ResourceLocation advancement) {
        var connection = Minecraft.getInstance().getConnection();
        AdvancementHolder holder = connection == null ? null : connection.getAdvancements().get(advancement);
        if (holder != null && holder.value().display().isPresent()) {
            return holder.value().display().map(DisplayInfo::getTitle).get();
        }
        return Component.literal(advancement.getPath());
    }

    private boolean respecArmed() {
        return respecArmedAt >= 0 && Util.getMillis() - respecArmedAt < RESPEC_SURE_MS;
    }

    private boolean creative() {
        var player = Minecraft.getInstance().player;
        return player != null && player.getAbilities().instabuild;
    }

    private int levels() {
        var player = Minecraft.getInstance().player;
        return player == null ? 0 : player.experienceLevel;
    }

    private void say(Component message) {
        status = message;
        statusAt = Util.getMillis();
    }

    private void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    // ----------------------------------------------------------------------------------- clicks

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || selection == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int mx = (int) ((mouseX - originX) / scale);
        int my = (int) ((mouseY - originY) / scale);
        syncState();

        if (inside(mx, my, confirmX, BUTTON_Y, confirmW, BUTTON_H)) {
            confirm();
            return true;
        }
        if (inside(mx, my, respecX, BUTTON_Y, respecW, BUTTON_H)) {
            respec();
            return true;
        }
        View hit = nodeAt(mx, my);
        if (hit != null) {
            clickNode(hit);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickNode(View view) {
        respecArmedAt = -1;
        switch (selection.toggle(view.id(), taken, shown.available())) {
            case MARKED -> play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.3f);
            case UNMARKED -> play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f);
            case ALREADY_TAKEN -> say(Component.translatable("growth.ascension_combat.screen.status.taken"));
            case NOT_CONNECTED -> {
                say(Component.translatable("growth.ascension_combat.screen.status.not_connected"));
                play(SoundEvents.NOTE_BLOCK_BASS.value(), 0.7f);
            }
            case NO_POINTS -> {
                say(Component.translatable("growth.ascension_combat.screen.status.no_points"));
                play(SoundEvents.NOTE_BLOCK_BASS.value(), 0.7f);
            }
        }
    }

    private void confirm() {
        List<ResourceLocation> marked = selection.marked();
        if (marked.isEmpty()) {
            return;
        }
        PacketDistributor.sendToServer(new GrowthTakePayload(marked));
        selection.clear();
        play(SoundEvents.ENCHANTMENT_TABLE_USE, 1.2f);
    }

    private void respec() {
        if (taken.isEmpty()) {
            say(Component.translatable("growth.ascension_combat.respec.nothing"));
        } else if (!creative() && levels() < GrowthRules.RESPEC_LEVELS) {
            say(Component.translatable("growth.ascension_combat.respec.levels", GrowthRules.RESPEC_LEVELS));
        } else if (!respecArmed()) {
            respecArmedAt = Util.getMillis();
            play(SoundEvents.NOTE_BLOCK_BASS.value(), 1.4f);
        } else {
            respecArmedAt = -1;
            PacketDistributor.sendToServer(GrowthRespecPayload.INSTANCE);
            selection.clear();
            play(SoundEvents.GRINDSTONE_USE, 1.0f);
        }
    }

    private static boolean inside(int x, int y, int left, int top, int w, int h) {
        return x >= left && x < left + w && y >= top && y < top + h;
    }

    /** The node under a point, the nearest if several: sockets are small and the web is close. */
    private View nodeAt(int px, int py) {
        View best = null;
        double bestDistance = Double.MAX_VALUE;
        for (View view : views) {
            double reach = view.size() / 2.0 + 1.0;
            double distance = Math.hypot(px - view.x(), py - view.y());
            if (distance <= reach && distance < bestDistance) {
                best = view;
                bestDistance = distance;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------------------------- drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        if (selection == null) {
            return;
        }
        syncState();

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(originX, originY, 0);
        pose.scale(scale, scale, 1f);
        int mx = (int) ((mouseX - originX) / scale);
        int my = (int) ((mouseY - originY) / scale);
        hovered = nodeAt(mx, my);

        g.blit(GrowthSprites.PANEL, 0, 0, 0f, 0f, W, H, W, H);
        if (views.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("growth.ascension_combat.screen.empty"), W / 2, H / 2,
                    GrowthDisplay.TEXT_DIM);
            pose.popPose();
            return;
        }
        drawTitleBar(g, mx, my);
        drawLinks(g);
        g.blit(GrowthSprites.NODES, CENTRE_X - 15, CENTRE_Y - 15, (float) GrowthSprites.CENTRE_U,
                (float) GrowthSprites.CENTRE_V, GrowthSprites.CENTRE_SIZE, GrowthSprites.CENTRE_SIZE,
                GrowthSprites.SHEET_W, GrowthSprites.SHEET_H);
        for (View view : views) {
            drawNode(g, view);
        }
        drawThemeNames(g);
        drawSidePanel(g);
        if (hovered != null) {
            pose.translate(0, 0, 200);
            drawCard(g, hovered);
        }
        pose.popPose();
    }

    private GrowthSelection.State stateOf(View view) {
        return selection.state(view.id(), taken);
    }

    private void drawNode(GuiGraphics g, View view) {
        GrowthSelection.State state = stateOf(view);
        int size = view.size();
        boolean lit = state == GrowthSelection.State.TAKEN || state == GrowthSelection.State.MARKED;
        int u;
        int v;
        if (state == GrowthSelection.State.LOCKED) {
            u = GrowthSprites.lockedU(view.notable());
            v = GrowthSprites.LOCKED_V;
        } else {
            u = GrowthSprites.socketU(view.notable(), lit);
            v = GrowthSprites.themeV(view.theme());
        }
        int left = (int) Math.round(view.x() - size / 2.0);
        int top = (int) Math.round(view.y() - size / 2.0);
        g.blit(GrowthSprites.NODES, left, top, (float) u, (float) v, size, size, GrowthSprites.SHEET_W, GrowthSprites.SHEET_H);

        if (state == GrowthSelection.State.MARKED) {
            drawRing(g, view, GrowthSprites.MARKED_EXTRA, GrowthSprites.markedU(view.notable()));
        }
        if (view == hovered) {
            drawRing(g, view, GrowthSprites.HOVER_EXTRA, GrowthSprites.hoverU(view.notable()));
        }
        if (view.glyph() != GrowthDisplay.NO_GLYPH) {
            int row = lit ? 0 : state == GrowthSelection.State.OPEN ? 1 : 2;
            g.blit(GrowthSprites.NODES, (int) Math.round(view.x() - 3.5), (int) Math.round(view.y() - 3.5),
                    (float) (GrowthSprites.GLYPH_U + GrowthSprites.GLYPH_SIZE * view.glyph()),
                    (float) GrowthSprites.GLYPH_V[row], GrowthSprites.GLYPH_SIZE, GrowthSprites.GLYPH_SIZE,
                    GrowthSprites.SHEET_W, GrowthSprites.SHEET_H);
        }
    }

    private void drawRing(GuiGraphics g, View view, int extra, int u) {
        int box = view.size() + extra;
        g.blit(GrowthSprites.NODES, (int) Math.round(view.x() - box / 2.0), (int) Math.round(view.y() - box / 2.0),
                (float) u, (float) GrowthSprites.LOCKED_V, box, box, GrowthSprites.SHEET_W, GrowthSprites.SHEET_H);
    }

    /** Every link once, under the sockets; the start nodes also join the centre. */
    private void drawLinks(GuiGraphics g) {
        Set<ResourceLocation> marked = new HashSet<>(selection.marked());
        for (Edge edge : edges) {
            boolean a = taken.contains(edge.a().id());
            boolean b = taken.contains(edge.b().id());
            boolean on = a || b || marked.contains(edge.a().id()) || marked.contains(edge.b().id());
            int[] ra = edge.a().ramp();
            int[] rb = edge.b().ramp();
            int colour = a && b ? GrowthDisplay.alpha(GrowthDisplay.mix(ra[3], rb[3], 0.5f), 230)
                    : on ? GrowthDisplay.alpha(GrowthDisplay.mix(ra[1], rb[1], 0.5f), 190)
                    : 0x5A464E60;
            line(g, edge.a().x(), edge.a().y(), edge.b().x(), edge.b().y(), colour, !a && !b && !on);
        }
        for (View view : views) {
            if (view.node().start()) {
                boolean has = taken.contains(view.id());
                int[] ramp = view.ramp();
                int colour = GrowthDisplay.alpha(GrowthDisplay.mix(ramp[has ? 3 : 1], GrowthDisplay.BRASS[has ? 3 : 1], 0.5f),
                        has ? 230 : 190);
                line(g, view.x(), view.y(), CENTRE_X, CENTRE_Y, colour, false);
            }
        }
    }

    /** A one-pixel line in whole GUI pixels, dashed if asked: the way the mockup draws them. */
    private static void line(GuiGraphics g, double x0, double y0, double x1, double y1, int colour, boolean dashed) {
        int ax = (int) Math.round(x0);
        int ay = (int) Math.round(y0);
        int bx = (int) Math.round(x1);
        int by = (int) Math.round(y1);
        int dx = Math.abs(bx - ax);
        int dy = -Math.abs(by - ay);
        int sx = ax < bx ? 1 : -1;
        int sy = ay < by ? 1 : -1;
        int err = dx + dy;
        int step = 0;
        while (true) {
            if (!dashed || (step / 2) % 2 == 0) {
                g.fill(ax, ay, ax + 1, ay + 1, colour);
            }
            step++;
            if (ax == bx && ay == by) {
                return;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                ax += sx;
            }
            if (e2 <= dx) {
                err += dx;
                ay += sy;
            }
        }
    }

    private void drawTitleBar(GuiGraphics g, int mx, int my) {
        g.drawString(font, Component.translatable("growth.ascension_combat.screen.title")
                .withStyle(ChatFormatting.BOLD), 9, 5, GrowthDisplay.BRASS[3], true);

        int marked = selection.marked().size();
        boolean canConfirm = marked > 0;
        Component confirm = Component.translatable("growth.ascension_combat.screen.confirm");
        Component respec = Component.translatable(respecArmed()
                ? "growth.ascension_combat.screen.respec_sure" : "growth.ascension_combat.screen.respec");
        confirmW = font.width(confirm) + 14;
        respecW = font.width(respec) + 14;
        confirmX = W - 8 - confirmW;
        respecX = confirmX - 4 - respecW;

        boolean canRespec = !taken.isEmpty() && (creative() || levels() >= GrowthRules.RESPEC_LEVELS);
        button(g, confirmX, confirmW, confirm, true, canConfirm, mx, my);
        button(g, respecX, respecW, respec, false, canRespec, mx, my);

        Component cost = Component.translatable("growth.ascension_combat.screen.respec_cost", GrowthRules.RESPEC_LEVELS);
        int costX = respecX - 6 - font.width(cost);
        orb(g, costX - 9, 5);
        g.drawString(font, cost, costX, 5, GrowthDisplay.XP_GREEN, true);

        Component marks = Component.translatable("growth.ascension_combat.screen.marked", marked);
        g.drawString(font, marks, costX - 16 - font.width(marks), 5,
                marked > 0 ? GrowthDisplay.TEXT_BODY : GrowthDisplay.TEXT_FAINT, true);
    }

    private void button(GuiGraphics g, int x, int w, Component label, boolean primary, boolean enabled, int mx, int my) {
        boolean hot = enabled && inside(mx, my, x, BUTTON_Y, w, BUTTON_H);
        int border = !enabled ? GrowthDisplay.BRASS[0]
                : hot || primary ? GrowthDisplay.BRASS[2] : GrowthDisplay.BRASS[1];
        g.fill(x, BUTTON_Y, x + w, BUTTON_Y + BUTTON_H, 0xFF0A0E1C);
        g.fill(x, BUTTON_Y, x + w, BUTTON_Y + 1, border);
        g.fill(x, BUTTON_Y + BUTTON_H - 1, x + w, BUTTON_Y + BUTTON_H, border);
        g.fill(x, BUTTON_Y, x + 1, BUTTON_Y + BUTTON_H, border);
        g.fill(x + w - 1, BUTTON_Y, x + w, BUTTON_Y + BUTTON_H, border);
        int colour = !enabled ? GrowthDisplay.TEXT_FAINT : primary || hot ? GrowthDisplay.TEXT : GrowthDisplay.TEXT_BODY;
        g.drawString(font, label, x + (w - font.width(label)) / 2, BUTTON_Y + 2, colour, true);
    }

    /** The XP orb beside the respec cost: a small green disc. */
    private static void orb(GuiGraphics g, int x, int y) {
        int dark = 0xFF3C8C14;
        int light = 0xFF8CE03C;
        g.fill(x + 2, y, x + 5, y + 1, dark);
        g.fill(x + 1, y + 1, x + 6, y + 6, dark);
        g.fill(x + 2, y + 6, x + 5, y + 7, dark);
        g.fill(x + 2, y + 2, x + 5, y + 5, light);
        g.fill(x + 2, y + 2, x + 3, y + 3, 0xFFE8FF9C);
    }

    private void drawThemeNames(GuiGraphics g) {
        theme(g, 0, 8, 22, false);
        theme(g, 1, 264, 22, true);
        theme(g, 2, 264, H - 14, true);
        theme(g, 3, 8, H - 14, false);
    }

    private void theme(GuiGraphics g, int theme, int x, int y, boolean rightAligned) {
        String name = Component.translatable("growth.ascension_combat.theme." + GrowthNode.Arm.values()[theme].getSerializedName())
                .getString().toUpperCase(Locale.ROOT);
        Component text = Component.literal(name).withStyle(ChatFormatting.BOLD);
        int left = rightAligned ? x - font.width(text) : x;
        g.drawString(font, text, left, y, GrowthDisplay.ramp(GrowthNode.Arm.values()[theme])[3], true);
    }

    private void drawSidePanel(GuiGraphics g) {
        int x0 = 272;
        int right = W - 6;
        int available = shown.available();
        int marked = selection.marked().size();

        g.drawString(font, Component.translatable("growth.ascension_combat.screen.points"), x0, 22, GrowthDisplay.TEXT_DIM, false);
        Component big = Component.literal(Integer.toString(available)).withStyle(ChatFormatting.BOLD);
        g.drawString(font, big, x0, 32, GrowthDisplay.TEXT, true);
        g.drawString(font, Component.translatable("growth.ascension_combat.screen.to_spend"), x0 + font.width(big) + 4, 32,
                GrowthDisplay.TEXT_BODY, true);
        g.drawString(font, Component.translatable("growth.ascension_combat.screen.marked_left", marked, available - marked),
                x0, 43, marked > 0 ? GrowthDisplay.TEXT_BODY : GrowthDisplay.TEXT_FAINT, true);

        int y = 58;
        g.drawString(font, Component.translatable("growth.ascension_combat.screen.sources"), x0, y, GrowthDisplay.TEXT_DIM, false);
        y += 10;
        int shownSources = Math.min(sources.size(), sources.size() > MAX_SOURCE_LINES ? MAX_SOURCE_LINES - 1 : MAX_SOURCE_LINES);
        for (int i = 0; i < shownSources; i++) {
            Source source = sources.get(i);
            String points = "+" + source.points();
            int room = right - x0 - font.width(points) - 4;
            g.drawString(font, font.substrByWidth(source.title(), room).getString(), x0, y, GrowthDisplay.TEXT_BODY, true);
            g.drawString(font, points, right - font.width(points), y, GrowthDisplay.BRASS[3], true);
            y += 10;
        }
        if (sources.size() > shownSources) {
            g.drawString(font, Component.translatable("growth.ascension_combat.screen.sources_more", sources.size() - shownSources),
                    x0, y, GrowthDisplay.TEXT_DIM, true);
            y += 10;
        }
        int toFind = Math.max(0, allMilestonePoints - shown.earned());
        g.drawString(font, Component.translatable("growth.ascension_combat.screen.to_find", toFind), x0, y + 1,
                GrowthDisplay.TEXT_FAINT, true);

        y += 16;
        g.drawString(font, Component.translatable("growth.ascension_combat.screen.stats"), x0, y, GrowthDisplay.TEXT_DIM, false);
        y += 10;
        for (int i = 0; i < Math.min(stats.size(), MAX_STAT_LINES); i++) {
            StatLine line = stats.get(i);
            int after = g.drawString(font, line.number() + " ", x0, y, line.colour(), true);
            g.drawString(font, font.substrByWidth(line.name(), right - after).getString(), after, y, GrowthDisplay.TEXT_BODY, true);
            y += 10;
        }

        boolean fresh = Util.getMillis() - statusAt < STATUS_MS && !status.getString().isEmpty();
        if (fresh) {
            int lines = 0;
            for (var part : font.split(status, right - x0)) {
                g.drawString(font, part, x0, H - 24 + 10 * lines++, GrowthDisplay.REFUSED, true);
            }
        } else {
            g.drawString(font, Component.translatable("growth.ascension_combat.screen.hint_mark"), x0, H - 24,
                    GrowthDisplay.TEXT_FAINT, true);
            g.drawString(font, Component.translatable("growth.ascension_combat.screen.hint_confirm"), x0, H - 14,
                    GrowthDisplay.TEXT_FAINT, true);
        }
    }

    /** The hover card beside a node: its name, theme and kind, what it gives, and what taking it needs. */
    private void drawCard(GuiGraphics g, View view) {
        GrowthSelection.State state = stateOf(view);
        int[] ramp = view.ramp();
        Component name = view.node().displayName(view.id()).copy().withStyle(ChatFormatting.BOLD);
        Component tag = Component.translatable("growth.ascension_combat.card.tag",
                Component.translatable("growth.ascension_combat.theme." + view.node().arm().getSerializedName()),
                Component.translatable(view.notable() ? "growth.ascension_combat.kind.notable" : "growth.ascension_combat.kind.small"));
        List<Component> numbers = new ArrayList<>();
        for (GrowthStat stat : view.node().stats()) {
            numbers.add(Component.literal(GrowthDisplay.number(stat.attribute(), stat.operation(), stat.amount()) + " ")
                    .append(GrowthDisplay.name(stat.attribute())));
        }
        String noteKey = switch (state) {
            case TAKEN -> "growth.ascension_combat.card.taken";
            case MARKED -> "growth.ascension_combat.card.marked";
            case OPEN -> "growth.ascension_combat.card.cost";
            case LOCKED -> "growth.ascension_combat.card.locked";
        };

        int inner = Math.max(96, Math.max(font.width(name), font.width(tag)));
        for (Component number : numbers) {
            inner = Math.max(inner, font.width(number));
        }
        inner = Math.min(inner, 150);
        int w = inner + 14;
        var note = font.split(Component.translatable(noteKey), inner);
        int h = 6 + 10 + 12 + numbers.size() * 10 + 4 + note.size() * 10 + 4;

        int x = (int) view.x() + 12;
        if (x + w > W - 6) {
            x = (int) view.x() - 12 - w;
        }
        int y = Math.max(22, Math.min((int) view.y() - 14, H - 6 - h));

        g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x6E000000);
        g.fill(x, y, x + w, y + h, 0xFC060914);
        g.fill(x, y, x + w, y + 1, ramp[3]);
        g.fill(x, y + h - 1, x + w, y + h, ramp[1]);
        g.fill(x, y, x + 1, y + h, ramp[2]);
        g.fill(x + w - 1, y, x + w, y + h, ramp[1]);
        for (int[] corner : new int[][] {{x, y}, {x + w - 1, y}, {x, y + h - 1}, {x + w - 1, y + h - 1}}) {
            g.fill(corner[0], corner[1], corner[0] + 1, corner[1] + 1, GrowthDisplay.BRASS[3]);
        }

        int yy = y + 6;
        g.drawString(font, name, x + 7, yy, GrowthDisplay.TEXT, true);
        g.drawString(font, tag, x + 7, yy + 10, ramp[3], true);
        yy += 22;
        for (int i = 0; i < view.node().stats().size(); i++) {
            ResourceLocation attribute = view.node().stats().get(i).attribute();
            g.drawString(font, numbers.get(i), x + 7, yy, GrowthDisplay.colour(attribute), true);
            yy += 10;
        }
        yy += 4;
        for (var part : note) {
            g.drawString(font, part, x + 7, yy, GrowthDisplay.TEXT_DIM, true);
            yy += 10;
        }
    }
}
