package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class TestModClient implements ClientModInitializer {
    private static final PvPOverlayConfig CONFIG = PvPOverlayConfig.load();

    private static String hitMainText = "-";
    private static String hitRoundedDigit = "";

    private static String takenMainText = "-";
    private static String takenRoundedDigit = "";

    private static String jumpResetText = "-";
    private static int jumpResetColor = 0xFFFFFFFF;

    private static boolean previousJumpDown = false;
    private static int previousHurtTime = 0;

    private static long clientTickCounter = 0L;
    private static long lastJumpResetUpdateTick = -1L;

    private static long lastEntityDamageTick = -1L;

    private static final int ENTITY_DAMAGE_VALID_TICKS = 6;

    private static final ArrayDeque<Long> recentJumpTicks = new ArrayDeque<>();
    private static final ArrayDeque<Long> recentHurt9Ticks = new ArrayDeque<>();

    private static KeyMapping openConfigKey;

    @Override
    public void onInitializeClient() {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pvp-overlay.open_config",
                GLFW.GLFW_KEY_O,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            clientTickCounter++;

            while (openConfigKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new PvPOverlayConfigScreen());
                }
            }

            CONFIG.ensureEnabledModulesHaveGroups();

            applyForcedOtherPlayerMainHand(client);
            updateJumpReset(client);
        });

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            if (!CONFIG.overlayEnabled) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            drawOverlay(graphics, minecraft, false);
        });
    }

    private static void applyForcedOtherPlayerMainHand(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        HumanoidArm forcedArm = getForcedOtherPlayerMainHandArm();

        if (forcedArm == null) {
            return;
        }

        for (Player player : minecraft.level.players()) {
            if (player == minecraft.player) {
                continue;
            }

            player.setMainArm(forcedArm);
        }
    }

    private static void updateJumpReset(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            previousJumpDown = false;
            previousHurtTime = 0;
            lastEntityDamageTick = -1L;
            recentJumpTicks.clear();
            recentHurt9Ticks.clear();
            return;
        }

        boolean jumpDown = minecraft.options.keyJump.isDown();

        if (jumpDown && !previousJumpDown) {
            recentJumpTicks.addLast(clientTickCounter);
            tryPairJumpReset();
        }

        previousJumpDown = jumpDown;

        int hurtTime = minecraft.player.hurtTime;

        if (
                hurtTime == 9 &&
                        previousHurtTime != 9 &&
                        isJumpResetSprintRequirementMet(minecraft.player) &&
                        wasRecentlyDamagedByEntity()
        ) {
            recentHurt9Ticks.addLast(clientTickCounter);
            tryPairJumpReset();
        }

        previousHurtTime = hurtTime;

        pruneOldJumpResetTicks();

        if (lastJumpResetUpdateTick > 0 && clientTickCounter - lastJumpResetUpdateTick > CONFIG.jumpResetDisplayTicks) {
            jumpResetText = "-";
            jumpResetColor = 0xFFFFFFFF;
        }
    }

    private static boolean isJumpResetSprintRequirementMet(Player player) {
        return !CONFIG.jumpResetRequireSprint || player.isSprinting();
    }

    private static boolean wasRecentlyDamagedByEntity() {
        return lastEntityDamageTick > 0 &&
                clientTickCounter - lastEntityDamageTick <= ENTITY_DAMAGE_VALID_TICKS;
    }

    private static void tryPairJumpReset() {
        if (recentJumpTicks.isEmpty() || recentHurt9Ticks.isEmpty()) {
            return;
        }

        int maxWindowTicks = CONFIG.jumpResetPairWindowTicks;

        Long bestJumpTick = null;
        Long bestHurtTick = null;
        long bestAbsDiff = Long.MAX_VALUE;

        for (Long jumpTick : recentJumpTicks) {
            for (Long hurtTick : recentHurt9Ticks) {
                long diff = jumpTick - hurtTick;
                long absDiff = Math.abs(diff);

                if (absDiff <= maxWindowTicks && absDiff < bestAbsDiff) {
                    bestAbsDiff = absDiff;
                    bestJumpTick = jumpTick;
                    bestHurtTick = hurtTick;
                }
            }
        }

        if (bestJumpTick == null || bestHurtTick == null) {
            return;
        }

        recentJumpTicks.remove(bestJumpTick);
        recentHurt9Ticks.remove(bestHurtTick);

        long diffTicks = bestJumpTick - bestHurtTick;
        setJumpResetResult(diffTicks);
    }

    private static void setJumpResetResult(long diffTicks) {
        lastJumpResetUpdateTick = clientTickCounter;

        jumpResetText = getJumpResetDisplayText(diffTicks);

        if (diffTicks == 0) {
            jumpResetColor = parseColorHex(CONFIG.jumpResetPerfectColor, 0xFF55FF55);
        } else if (diffTicks < 0) {
            jumpResetColor = parseColorHex(CONFIG.jumpResetEarlyColor, 0xFFFFFF55);
        } else {
            jumpResetColor = parseColorHex(CONFIG.jumpResetLateColor, 0xFFFF5555);
        }
    }

    private static String getJumpResetDisplayText(long diffTicks) {
        if (diffTicks == 0) {
            return Component.translatable("overlay.pvp-overlay.jump_reset.perfect").getString();
        }

        String offset = formatJumpResetOffset(diffTicks);

        if (!CONFIG.jumpResetShowTimingLabels) {
            return offset;
        }

        if (diffTicks < 0) {
            return Component.translatable("overlay.pvp-overlay.jump_reset.early").getString()
                    + " "
                    + offset;
        }

        return Component.translatable("overlay.pvp-overlay.jump_reset.late").getString()
                + " "
                + offset;
    }

    private static String formatJumpResetOffset(long ticks) {
        String sign = ticks > 0 ? "+" : "-";
        long absTicks = Math.abs(ticks);
        String unit = getJumpResetUnitSuffix(absTicks);

        if (unit.isEmpty()) {
            return sign + absTicks;
        }

        return sign + absTicks + " " + unit;
    }

    private static String getJumpResetUnitSuffix(long absTicks) {
        return switch (CONFIG.jumpResetUnitMode) {
            case 1 -> absTicks == 1 ? "tick" : "ticks";
            case 2 -> absTicks == 1 ? "Tick" : "Ticks";
            case 3 -> "";
            case 4 -> "t";
            default -> "T";
        };
    }

    private static void pruneOldJumpResetTicks() {
        long maxAge = Math.max(
                CONFIG.jumpResetPairWindowTicks,
                CONFIG.jumpResetDisplayTicks
        ) + 5L;

        pruneDequeOlderThan(recentJumpTicks, clientTickCounter - maxAge);
        pruneDequeOlderThan(recentHurt9Ticks, clientTickCounter - maxAge);
    }

    private static void pruneDequeOlderThan(ArrayDeque<Long> deque, long minAllowedTick) {
        Iterator<Long> iterator = deque.iterator();

        while (iterator.hasNext()) {
            long tick = iterator.next();

            if (tick < minAllowedTick) {
                iterator.remove();
            }
        }
    }

    public static boolean isOpenConfigKey(KeyEvent input) {
        return openConfigKey != null && openConfigKey.matches(input);
    }

    public static void recordOutgoingHit(Player player, Entity target) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || player != minecraft.player) {
            return;
        }

        if (!(minecraft.hitResult instanceof EntityHitResult entityHitResult)) {
            return;
        }

        if (entityHitResult.getEntity() != target) {
            return;
        }

        Vec3 eyePosition = player.getEyePosition();
        Vec3 hitPosition = entityHitResult.getLocation();

        double distance = eyePosition.distanceTo(hitPosition);

        setHitDistance(distance);
    }

    public static void recordIncomingHit(Player hurtEntity, DamageSource damageSource) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || hurtEntity != minecraft.player) {
            return;
        }

        Entity attacker = damageSource.getEntity();

        if (attacker == null) {
            return;
        }

        lastEntityDamageTick = clientTickCounter;

        Vec3 attackerEyes = attacker.getEyePosition();
        AABB myHitbox = minecraft.player.getBoundingBox();

        Vec3 closestPointOnMyHitbox = getClosestPointOnBox(attackerEyes, myHitbox);

        double distance = attackerEyes.distanceTo(closestPointOnMyHitbox);

        setTakenDistance(distance);
    }

    public static boolean isOverlayEnabled() {
        return CONFIG.overlayEnabled;
    }

    public static boolean isShowHit() {
        return CONFIG.showHit;
    }

    public static boolean isShowTaken() {
        return CONFIG.showTaken;
    }

    public static boolean isShowJumpReset() {
        return CONFIG.showJumpReset;
    }

    public static boolean isJumpResetRequireSprint() {
        return CONFIG.jumpResetRequireSprint;
    }

    public static boolean isJumpResetShowTimingLabels() {
        return CONFIG.jumpResetShowTimingLabels;
    }

    public static int getJumpResetPairWindowTicks() {
        return CONFIG.jumpResetPairWindowTicks;
    }

    public static int getJumpResetDisplayTicks() {
        return CONFIG.jumpResetDisplayTicks;
    }

    public static Component getJumpResetUnitText() {
        return switch (CONFIG.jumpResetUnitMode) {
            case 1 -> Component.literal("ticks");
            case 2 -> Component.literal("Ticks");
            case 3 -> Component.translatable("state.pvp-overlay.none");
            case 4 -> Component.literal("t");
            default -> Component.literal("T");
        };
    }

    public static String getJumpResetPerfectColorHex() {
        return CONFIG.jumpResetPerfectColor;
    }

    public static String getJumpResetEarlyColorHex() {
        return CONFIG.jumpResetEarlyColor;
    }

    public static String getJumpResetLateColorHex() {
        return CONFIG.jumpResetLateColor;
    }

    public static void toggleOverlayEnabled() {
        CONFIG.overlayEnabled = !CONFIG.overlayEnabled;
        saveConfig();
    }

    public static void toggleShowHit() {
        CONFIG.showHit = !CONFIG.showHit;
        CONFIG.ensureEnabledModulesHaveGroups();
        saveConfig();
    }

    public static void toggleShowTaken() {
        CONFIG.showTaken = !CONFIG.showTaken;
        CONFIG.ensureEnabledModulesHaveGroups();
        saveConfig();
    }

    public static void toggleShowJumpReset() {
        CONFIG.showJumpReset = !CONFIG.showJumpReset;
        CONFIG.ensureEnabledModulesHaveGroups();
        saveConfig();
    }

    public static void toggleJumpResetRequireSprint() {
        CONFIG.jumpResetRequireSprint = !CONFIG.jumpResetRequireSprint;
        saveConfig();
    }

    public static void toggleJumpResetShowTimingLabels() {
        CONFIG.jumpResetShowTimingLabels = !CONFIG.jumpResetShowTimingLabels;
        saveConfig();
    }

    public static void setJumpResetPairWindowTicks(int ticks) {
        CONFIG.jumpResetPairWindowTicks = clampInt(
                ticks,
                PvPOverlayConfig.MIN_JUMP_RESET_PAIR_WINDOW_TICKS,
                PvPOverlayConfig.MAX_JUMP_RESET_PAIR_WINDOW_TICKS
        );
        saveConfig();
    }

    public static void resetJumpResetPairWindowTicks() {
        setJumpResetPairWindowTicks(PvPOverlayConfig.DEFAULT_JUMP_RESET_PAIR_WINDOW_TICKS);
    }

    public static void setJumpResetDisplayTicks(int ticks) {
        CONFIG.jumpResetDisplayTicks = clampInt(
                ticks,
                PvPOverlayConfig.MIN_JUMP_RESET_DISPLAY_TICKS,
                PvPOverlayConfig.MAX_JUMP_RESET_DISPLAY_TICKS
        );
        saveConfig();
    }

    public static void resetJumpResetDisplayTicks() {
        setJumpResetDisplayTicks(PvPOverlayConfig.DEFAULT_JUMP_RESET_DISPLAY_TICKS);
    }

    public static void cycleJumpResetUnitMode() {
        CONFIG.jumpResetUnitMode = switch (CONFIG.jumpResetUnitMode) {
            case 3 -> 0;
            case 0 -> 4;
            case 4 -> 2;
            case 2 -> 1;
            case 1 -> 3;
            default -> 0;
        };

        saveConfig();
    }

    public static void setJumpResetPerfectColorHex(String value) {
        CONFIG.jumpResetPerfectColor = PvPOverlayConfig.normalizeColorHex(
                value,
                PvPOverlayConfig.DEFAULT_JUMP_RESET_PERFECT_COLOR
        );
        saveConfig();
    }

    public static void setJumpResetEarlyColorHex(String value) {
        CONFIG.jumpResetEarlyColor = PvPOverlayConfig.normalizeColorHex(
                value,
                PvPOverlayConfig.DEFAULT_JUMP_RESET_EARLY_COLOR
        );
        saveConfig();
    }

    public static void setJumpResetLateColorHex(String value) {
        CONFIG.jumpResetLateColor = PvPOverlayConfig.normalizeColorHex(
                value,
                PvPOverlayConfig.DEFAULT_JUMP_RESET_LATE_COLOR
        );
        saveConfig();
    }

    public static void resetJumpResetPerfectColor() {
        CONFIG.jumpResetPerfectColor = PvPOverlayConfig.DEFAULT_JUMP_RESET_PERFECT_COLOR;
        saveConfig();
    }

    public static void resetJumpResetEarlyColor() {
        CONFIG.jumpResetEarlyColor = PvPOverlayConfig.DEFAULT_JUMP_RESET_EARLY_COLOR;
        saveConfig();
    }

    public static void resetJumpResetLateColor() {
        CONFIG.jumpResetLateColor = PvPOverlayConfig.DEFAULT_JUMP_RESET_LATE_COLOR;
        saveConfig();
    }

    public static int getForcedOtherPlayerMainHand() {
        return CONFIG.forcedOtherPlayerMainHand;
    }

    public static void cycleForcedOtherPlayerMainHand() {
        CONFIG.forcedOtherPlayerMainHand++;

        if (CONFIG.forcedOtherPlayerMainHand > 2) {
            CONFIG.forcedOtherPlayerMainHand = 0;
        }

        saveConfig();
    }

    public static Component getForcedOtherPlayerMainHandText() {
        return switch (CONFIG.forcedOtherPlayerMainHand) {
            case 1 -> Component.translatable("state.pvp-overlay.left");
            case 2 -> Component.translatable("state.pvp-overlay.right");
            default -> Component.translatable("state.pvp-overlay.off");
        };
    }

    public static HumanoidArm getForcedOtherPlayerMainHandArm() {
        return switch (CONFIG.forcedOtherPlayerMainHand) {
            case 1 -> HumanoidArm.LEFT;
            case 2 -> HumanoidArm.RIGHT;
            default -> null;
        };
    }

    public static int getConfigMenuOpacityPercent() {
        return CONFIG.configMenuOpacityPercent;
    }

    public static void setConfigMenuOpacityPercent(int percent) {
        CONFIG.configMenuOpacityPercent = clampInt(percent, 0, 100);
        saveConfig();
    }

    public static int getConfigMenuBackgroundColor() {
        int alpha = Math.round(CONFIG.configMenuOpacityPercent * 255.0f / 100.0f);
        return (alpha << 24);
    }

    public static void drawConfigMenuBackdrop(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, getConfigMenuBackgroundColor());
    }

    public static ArrayList<PvPOverlayConfig.OverlayGroupConfig> getOverlayGroups() {
        CONFIG.ensureEnabledModulesHaveGroups();
        return CONFIG.overlayGroups;
    }

    public static PvPOverlayConfig.OverlayGroupConfig findOverlayGroup(String groupId) {
        return CONFIG.findGroup(groupId);
    }

    public static void saveConfig() {
        CONFIG.clampAndRepair();
        CONFIG.ensureEnabledModulesHaveGroups();
        CONFIG.save();
    }

    public static String createLayoutSnapshot() {
        return new com.google.gson.Gson().toJson(CONFIG.overlayGroups);
    }

    public static void restoreLayoutSnapshot(String snapshot) {
        PvPOverlayConfig.OverlayGroupConfig[] groups = new com.google.gson.Gson().fromJson(
                snapshot,
                PvPOverlayConfig.OverlayGroupConfig[].class
        );

        CONFIG.overlayGroups.clear();

        if (groups != null) {
            for (PvPOverlayConfig.OverlayGroupConfig group : groups) {
                CONFIG.overlayGroups.add(group);
            }
        }

        saveConfig();
    }

    public static void resetOverlayLayout() {
        CONFIG.overlayGroups.clear();

        PvPOverlayConfig.OverlayGroupConfig group = PvPOverlayConfig.OverlayGroupConfig.createDefault(
                "group-main",
                0,
                10
        );

        group.showBox = true;
        group.modules.add(PvPOverlayConfig.MODULE_HIT);
        group.modules.add(PvPOverlayConfig.MODULE_TAKEN);
        group.modules.add(PvPOverlayConfig.MODULE_JUMP_RESET);

        CONFIG.overlayGroups.add(group);

        saveConfig();
    }

    public static void setOverlayPosition(int x, int y) {
        ArrayList<PvPOverlayConfig.OverlayGroupConfig> groups = getOverlayGroups();

        if (groups.isEmpty()) {
            resetOverlayLayout();
            groups = getOverlayGroups();
        }

        PvPOverlayConfig.OverlayGroupConfig group = groups.get(0);
        group.x = x;
        group.y = y;

        saveConfig();
    }

    public static void resetOverlayPosition() {
        resetOverlayLayout();
    }

    public static int getOverlayX(Minecraft minecraft) {
        ArrayList<PvPOverlayConfig.OverlayGroupConfig> groups = getOverlayGroups();

        if (groups.isEmpty()) {
            return 0;
        }

        return groups.get(0).x;
    }

    public static int getOverlayY(Minecraft minecraft) {
        ArrayList<PvPOverlayConfig.OverlayGroupConfig> groups = getOverlayGroups();

        if (groups.isEmpty()) {
            return 10;
        }

        return groups.get(0).y;
    }

    public static int getOverlayBoxWidth(Minecraft minecraft) {
        ArrayList<PvPOverlayConfig.OverlayGroupConfig> groups = getOverlayGroups();

        if (groups.isEmpty()) {
            return 0;
        }

        return getGroupBounds(minecraft, groups.get(0)).width;
    }

    public static int getOverlayBoxHeight(Minecraft minecraft) {
        ArrayList<PvPOverlayConfig.OverlayGroupConfig> groups = getOverlayGroups();

        if (groups.isEmpty()) {
            return 0;
        }

        return getGroupBounds(minecraft, groups.get(0)).height;
    }

    public static void drawOverlay(GuiGraphics graphics, Minecraft minecraft, boolean editorMode) {
        CONFIG.ensureEnabledModulesHaveGroups();

        boolean playerListOpen = minecraft.options.keyPlayerList.isDown();

        for (PvPOverlayConfig.OverlayGroupConfig group : CONFIG.overlayGroups) {
            if (!editorMode && group.hideWhenPlayerListOpen && playerListOpen) {
                continue;
            }

            drawGroup(graphics, minecraft, group, editorMode, false, null);
        }
    }

    public static void drawGroupEditorPreview(
            GuiGraphics graphics,
            Minecraft minecraft,
            PvPOverlayConfig.OverlayGroupConfig group,
            boolean hovered,
            String hoveredModule
    ) {
        drawGroup(graphics, minecraft, group, true, hovered, hoveredModule);
    }

    private static void drawGroup(
            GuiGraphics graphics,
            Minecraft minecraft,
            PvPOverlayConfig.OverlayGroupConfig group,
            boolean editorMode,
            boolean hovered,
            String hoveredModule
    ) {
        OverlayGroupBounds bounds = getGroupBounds(minecraft, group);

        if (bounds.visibleModules.isEmpty()) {
            return;
        }

        if (group.showBox) {
            int bgColor = PvPOverlayConfig.colorWithOpacity(
                    group.backgroundColor,
                    group.backgroundOpacityPercent,
                    0xFF000000
            );

            graphics.fill(
                    bounds.x,
                    bounds.y,
                    bounds.x + bounds.width,
                    bounds.y + bounds.height,
                    bgColor
            );
        }

        if (group.showBorder || editorMode) {
            int borderColor;

            if (editorMode && hovered) {
                borderColor = 0xFF55AAFF;
            } else {
                borderColor = PvPOverlayConfig.colorWithOpacity(
                        group.borderColor,
                        group.borderOpacityPercent,
                        0xFFFFFFFF
                );
            }

            graphics.renderOutline(bounds.x, bounds.y, bounds.width, bounds.height, borderColor);
        }

        for (OverlayModuleRow row : bounds.rows) {
            if (editorMode && row.moduleId.equals(hoveredModule)) {
                graphics.renderOutline(
                        row.x - 2,
                        row.y - 2,
                        row.width + 4,
                        row.height + 4,
                        0xFFFFFF55
                );
            }

            drawModuleText(graphics, minecraft, row.moduleId, row.x, row.y);
        }
    }

    private static void drawModuleText(GuiGraphics graphics, Minecraft minecraft, String moduleId, int x, int y) {
        switch (moduleId) {
            case PvPOverlayConfig.MODULE_HIT -> drawDistanceLine(
                    graphics,
                    minecraft,
                    x,
                    y,
                    Component.translatable("overlay.pvp-overlay.hit").getString() + ": ",
                    hitMainText,
                    hitRoundedDigit,
                    " " + Component.translatable("overlay.pvp-overlay.blocks").getString()
            );
            case PvPOverlayConfig.MODULE_TAKEN -> drawDistanceLine(
                    graphics,
                    minecraft,
                    x,
                    y,
                    Component.translatable("overlay.pvp-overlay.taken").getString() + ": ",
                    takenMainText,
                    takenRoundedDigit,
                    " " + Component.translatable("overlay.pvp-overlay.blocks").getString()
            );
            case PvPOverlayConfig.MODULE_JUMP_RESET -> drawSimpleLine(
                    graphics,
                    minecraft,
                    x,
                    y,
                    Component.translatable("overlay.pvp-overlay.jump_reset").getString() + ": ",
                    jumpResetText,
                    jumpResetColor
            );
        }
    }

    public static OverlayGroupBounds getGroupBounds(Minecraft minecraft, PvPOverlayConfig.OverlayGroupConfig group) {
        group.repair();

        ArrayList<String> visibleModules = getVisibleModules(group);

        int fontHeight = minecraft.font.lineHeight;
        int maxWidth = 0;

        ArrayList<OverlayModuleRow> rows = new ArrayList<>();

        int contentX = group.x + group.paddingX;
        int contentY = group.y + group.paddingY;

        int y = contentY;

        for (String moduleId : visibleModules) {
            int width = getModuleTextWidth(minecraft, moduleId);

            rows.add(new OverlayModuleRow(
                    moduleId,
                    contentX,
                    y,
                    width,
                    fontHeight
            ));

            maxWidth = Math.max(maxWidth, width);
            y += fontHeight + group.lineGap;
        }

        int contentHeight = visibleModules.isEmpty()
                ? 0
                : visibleModules.size() * fontHeight + Math.max(0, visibleModules.size() - 1) * group.lineGap;

        int width = maxWidth + group.paddingX * 2;
        int height = contentHeight + group.paddingY * 2;

        return new OverlayGroupBounds(
                group.id,
                group.x,
                group.y,
                width,
                height,
                visibleModules,
                rows
        );
    }

    private static ArrayList<String> getVisibleModules(PvPOverlayConfig.OverlayGroupConfig group) {
        ArrayList<String> visibleModules = new ArrayList<>();

        if (group.modules == null) {
            return visibleModules;
        }

        for (String moduleId : group.modules) {
            if (isModuleVisible(moduleId)) {
                visibleModules.add(moduleId);
            }
        }

        return visibleModules;
    }

    private static boolean isModuleVisible(String moduleId) {
        return switch (moduleId) {
            case PvPOverlayConfig.MODULE_HIT -> CONFIG.showHit;
            case PvPOverlayConfig.MODULE_TAKEN -> CONFIG.showTaken;
            case PvPOverlayConfig.MODULE_JUMP_RESET -> CONFIG.showJumpReset;
            default -> false;
        };
    }

    public static String getModuleDisplayName(String moduleId) {
        return switch (moduleId) {
            case PvPOverlayConfig.MODULE_HIT -> Component.translatable("overlay.pvp-overlay.hit").getString();
            case PvPOverlayConfig.MODULE_TAKEN -> Component.translatable("overlay.pvp-overlay.taken").getString();
            case PvPOverlayConfig.MODULE_JUMP_RESET -> Component.translatable("overlay.pvp-overlay.jump_reset").getString();
            default -> moduleId;
        };
    }

    private static int getModuleTextWidth(Minecraft minecraft, String moduleId) {
        return switch (moduleId) {
            case PvPOverlayConfig.MODULE_HIT -> minecraft.font.width(hitFullText());
            case PvPOverlayConfig.MODULE_TAKEN -> minecraft.font.width(takenFullText());
            case PvPOverlayConfig.MODULE_JUMP_RESET -> Math.max(
                    minecraft.font.width(jumpResetFullText()),
                    getJumpResetStableTextWidth(minecraft)
            );
            default -> 0;
        };
    }

    private static int getJumpResetStableTextWidth(Minecraft minecraft) {
        String prefix = Component.translatable("overlay.pvp-overlay.jump_reset").getString() + ": ";

        int width = minecraft.font.width(prefix + Component.translatable("overlay.pvp-overlay.jump_reset.perfect").getString());

        int maxOffset = CONFIG.jumpResetPairWindowTicks;

        if (maxOffset <= 0) {
            return width;
        }

        width = Math.max(width, minecraft.font.width(prefix + getJumpResetDisplayText(-maxOffset)));
        width = Math.max(width, minecraft.font.width(prefix + getJumpResetDisplayText(maxOffset)));

        return width;
    }

    public static void moveModuleToNewGroup(String moduleId, int x, int y) {
        removeModuleFromAllGroups(moduleId);

        PvPOverlayConfig.OverlayGroupConfig group = PvPOverlayConfig.OverlayGroupConfig.createDefault(
                "group-" + moduleId + "-" + System.currentTimeMillis(),
                x,
                y
        );

        group.showBox = false;
        group.modules.add(moduleId);

        CONFIG.overlayGroups.add(group);

        saveConfig();
    }

    public static void moveModuleToGroup(String moduleId, String targetGroupId, int insertIndex) {
        PvPOverlayConfig.OverlayGroupConfig target = CONFIG.findGroup(targetGroupId);

        if (target == null) {
            return;
        }

        removeModuleFromAllGroups(moduleId);

        insertIndex = clampInt(insertIndex, 0, target.modules.size());

        target.modules.add(insertIndex, moduleId);

        saveConfig();
    }

    public static void removeModuleFromGroupToNewGroup(String moduleId, String groupId) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group == null || group.modules == null || !group.modules.contains(moduleId)) {
            return;
        }

        int x = group.x + 16;
        int y = group.y + 16;

        group.modules.remove(moduleId);
        CONFIG.removeEmptyGroups();

        PvPOverlayConfig.OverlayGroupConfig newGroup = PvPOverlayConfig.OverlayGroupConfig.createDefault(
                "group-" + moduleId + "-" + System.currentTimeMillis(),
                x,
                y
        );

        newGroup.showBox = false;
        newGroup.modules.add(moduleId);
        CONFIG.overlayGroups.add(newGroup);

        saveConfig();
    }

    private static void removeModuleFromAllGroups(String moduleId) {
        for (PvPOverlayConfig.OverlayGroupConfig group : CONFIG.overlayGroups) {
            if (group.modules != null) {
                group.modules.remove(moduleId);
            }
        }

        CONFIG.removeEmptyGroups();
    }

    public static void moveModuleUp(String groupId, String moduleId) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group == null || group.modules == null) {
            return;
        }

        int index = group.modules.indexOf(moduleId);

        if (index <= 0) {
            return;
        }

        group.modules.remove(index);
        group.modules.add(index - 1, moduleId);

        saveConfig();
    }

    public static void moveModuleDown(String groupId, String moduleId) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group == null || group.modules == null) {
            return;
        }

        int index = group.modules.indexOf(moduleId);

        if (index < 0 || index >= group.modules.size() - 1) {
            return;
        }

        group.modules.remove(index);
        group.modules.add(index + 1, moduleId);

        saveConfig();
    }

    public static void ungroupAll(String groupId) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group == null || group.modules == null || group.modules.size() <= 1) {
            return;
        }

        ArrayList<String> modules = new ArrayList<>(group.modules);
        int startX = group.x;
        int startY = group.y;

        CONFIG.overlayGroups.remove(group);

        for (int i = 0; i < modules.size(); i++) {
            PvPOverlayConfig.OverlayGroupConfig newGroup = PvPOverlayConfig.OverlayGroupConfig.createDefault(
                    "group-" + modules.get(i) + "-" + System.currentTimeMillis() + "-" + i,
                    startX,
                    startY + i * 24
            );

            newGroup.showBox = false;
            newGroup.modules.add(modules.get(i));
            CONFIG.overlayGroups.add(newGroup);
        }

        saveConfig();
    }

    public static void setGroupShowBox(String groupId, boolean value) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group != null) {
            group.showBox = value;
            saveConfig();
        }
    }

    public static void setGroupShowBorder(String groupId, boolean value) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group != null) {
            group.showBorder = value;
            saveConfig();
        }
    }

    public static void setGroupHideWhenPlayerListOpen(String groupId, boolean value) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group != null) {
            group.hideWhenPlayerListOpen = value;
            saveConfig();
        }
    }

    public static void setGroupBackgroundColor(String groupId, String value) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group != null) {
            group.backgroundColor = PvPOverlayConfig.normalizeColorHex(value, PvPOverlayConfig.DEFAULT_GROUP_BACKGROUND_COLOR);
            saveConfig();
        }
    }

    public static void setGroupBorderColor(String groupId, String value) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group != null) {
            group.borderColor = PvPOverlayConfig.normalizeColorHex(value, PvPOverlayConfig.DEFAULT_GROUP_BORDER_COLOR);
            saveConfig();
        }
    }

    public static void setGroupBackgroundOpacityPercent(String groupId, int percent) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group != null) {
            group.backgroundOpacityPercent = clampInt(percent, 0, 100);
            saveConfig();
        }
    }

    public static void setGroupBorderOpacityPercent(String groupId, int percent) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group != null) {
            group.borderOpacityPercent = clampInt(percent, 0, 100);
            saveConfig();
        }
    }

    public static void setGroupPaddingX(String groupId, int value) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group != null) {
            group.paddingX = clampInt(value, 0, 32);
            saveConfig();
        }
    }

    public static void setGroupPaddingY(String groupId, int value) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group != null) {
            group.paddingY = clampInt(value, 0, 32);
            saveConfig();
        }
    }

    public static void setGroupLineGap(String groupId, int value) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group != null) {
            group.lineGap = clampInt(value, 0, 20);
            saveConfig();
        }
    }

    public static void setGroupScalePercent(String groupId, int value) {
        PvPOverlayConfig.OverlayGroupConfig group = CONFIG.findGroup(groupId);

        if (group != null) {
            group.scalePercent = clampInt(value, 50, 200);
            saveConfig();
        }
    }

    private static String hitFullText() {
        return Component.translatable("overlay.pvp-overlay.hit").getString()
                + ": "
                + hitMainText
                + hitRoundedDigit
                + " "
                + Component.translatable("overlay.pvp-overlay.blocks").getString();
    }

    private static String takenFullText() {
        return Component.translatable("overlay.pvp-overlay.taken").getString()
                + ": "
                + takenMainText
                + takenRoundedDigit
                + " "
                + Component.translatable("overlay.pvp-overlay.blocks").getString();
    }

    private static String jumpResetFullText() {
        return Component.translatable("overlay.pvp-overlay.jump_reset").getString()
                + ": "
                + jumpResetText;
    }

    private static void setHitDistance(double distance) {
        String[] parts = formatDistance(distance);
        hitMainText = parts[0];
        hitRoundedDigit = parts[1];
    }

    private static void setTakenDistance(double distance) {
        String[] parts = formatDistance(distance);
        takenMainText = parts[0];
        takenRoundedDigit = parts[1];
    }

    private static String[] formatDistance(double distance) {
        String formattedDistance = String.format(Locale.US, "%.4f", distance);

        if (formattedDistance.contains(".")) {
            int dotIndex = formattedDistance.indexOf(".");
            int splitIndex = Math.min(dotIndex + 4, formattedDistance.length());

            String mainText = formattedDistance.substring(0, splitIndex);
            String roundedDigit = formattedDistance.substring(splitIndex);

            return new String[]{mainText, roundedDigit};
        }

        return new String[]{formattedDistance, ""};
    }

    private static Vec3 getClosestPointOnBox(Vec3 point, AABB box) {
        double x = clamp(point.x, box.minX, box.maxX);
        double y = clamp(point.y, box.minY, box.maxY);
        double z = clamp(point.z, box.minZ, box.maxZ);

        return new Vec3(x, y, z);
    }

    private static int parseColorHex(String value, int fallback) {
        if (!PvPOverlayConfig.isValidColorHex(value)) {
            return fallback;
        }

        String normalized = value.trim();

        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        return 0xFF000000 | Integer.parseInt(normalized, 16);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void drawDistanceLine(
            GuiGraphics graphics,
            Minecraft minecraft,
            int x,
            int y,
            String prefix,
            String mainText,
            String roundedDigit,
            String suffix
    ) {
        int textX = x;

        graphics.drawString(minecraft.font, prefix, textX, y, 0xFFFFFFFF, false);
        textX += minecraft.font.width(prefix);

        graphics.drawString(minecraft.font, mainText, textX, y, 0xFFFFFFFF, false);
        textX += minecraft.font.width(mainText);

        graphics.drawString(minecraft.font, roundedDigit, textX, y, 0xFF888888, false);
        textX += minecraft.font.width(roundedDigit);

        graphics.drawString(minecraft.font, suffix, textX, y, 0xFFFFFFFF, false);
    }

    private static void drawSimpleLine(
            GuiGraphics graphics,
            Minecraft minecraft,
            int x,
            int y,
            String prefix,
            String value,
            int valueColor
    ) {
        int textX = x;

        graphics.drawString(minecraft.font, prefix, textX, y, 0xFFFFFFFF, false);
        textX += minecraft.font.width(prefix);

        graphics.drawString(minecraft.font, value, textX, y, valueColor, false);
    }

    public record OverlayGroupBounds(
            String groupId,
            int x,
            int y,
            int width,
            int height,
            ArrayList<String> visibleModules,
            ArrayList<OverlayModuleRow> rows
    ) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x &&
                    mouseX <= x + width &&
                    mouseY >= y &&
                    mouseY <= y + height;
        }
    }

    public record OverlayModuleRow(
            String moduleId,
            int x,
            int y,
            int width,
            int height
    ) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x &&
                    mouseX <= x + width &&
                    mouseY >= y &&
                    mouseY <= y + height;
        }
    }
}