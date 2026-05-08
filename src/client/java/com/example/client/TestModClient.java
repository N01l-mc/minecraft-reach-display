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
import java.util.Iterator;
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

            applyForcedOtherPlayerMainHand(client);
            updateJumpReset(client);
        });

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            if (!CONFIG.overlayEnabled) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            drawOverlay(graphics, minecraft);
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
                        minecraft.player.isSprinting() &&
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

        if (diffTicks == 0) {
            jumpResetText = Component.translatable("overlay.pvp-overlay.jump_reset.perfect").getString();
            jumpResetColor = parseColorHex(CONFIG.jumpResetPerfectColor, 0xFF55FF55);
        } else if (diffTicks < 0) {
            jumpResetText = Component.translatable("overlay.pvp-overlay.jump_reset.early").getString()
                    + " "
                    + formatJumpResetOffset(diffTicks);

            jumpResetColor = parseColorHex(CONFIG.jumpResetEarlyColor, 0xFFFFFF55);
        } else {
            jumpResetText = Component.translatable("overlay.pvp-overlay.jump_reset.late").getString()
                    + " "
                    + formatJumpResetOffset(diffTicks);

            jumpResetColor = parseColorHex(CONFIG.jumpResetLateColor, 0xFFFF5555);
        }
    }

    private static String formatJumpResetOffset(long ticks) {
        String sign = ticks > 0 ? "+" : "-";
        long absTicks = Math.abs(ticks);

        return sign + absTicks + getJumpResetUnitSuffix();
    }

    private static String getJumpResetUnitSuffix() {
        return switch (CONFIG.jumpResetUnitMode) {
            case 1 -> "ticks";
            case 2 -> "Ticks";
            case 3 -> "";
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
        CONFIG.save();
    }

    public static void toggleShowHit() {
        CONFIG.showHit = !CONFIG.showHit;
        CONFIG.save();
    }

    public static void toggleShowTaken() {
        CONFIG.showTaken = !CONFIG.showTaken;
        CONFIG.save();
    }

    public static void toggleShowJumpReset() {
        CONFIG.showJumpReset = !CONFIG.showJumpReset;
        CONFIG.save();
    }

    public static void setJumpResetPairWindowTicks(int ticks) {
        CONFIG.jumpResetPairWindowTicks = clampInt(
                ticks,
                PvPOverlayConfig.MIN_JUMP_RESET_PAIR_WINDOW_TICKS,
                PvPOverlayConfig.MAX_JUMP_RESET_PAIR_WINDOW_TICKS
        );
        CONFIG.save();
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
        CONFIG.save();
    }

    public static void resetJumpResetDisplayTicks() {
        setJumpResetDisplayTicks(PvPOverlayConfig.DEFAULT_JUMP_RESET_DISPLAY_TICKS);
    }

    public static void cycleJumpResetUnitMode() {
        CONFIG.jumpResetUnitMode++;

        if (CONFIG.jumpResetUnitMode > 3) {
            CONFIG.jumpResetUnitMode = 0;
        }

        CONFIG.save();
    }

    public static void setJumpResetPerfectColorHex(String value) {
        CONFIG.jumpResetPerfectColor = PvPOverlayConfig.normalizeColorHex(
                value,
                PvPOverlayConfig.DEFAULT_JUMP_RESET_PERFECT_COLOR
        );
        CONFIG.save();
    }

    public static void setJumpResetEarlyColorHex(String value) {
        CONFIG.jumpResetEarlyColor = PvPOverlayConfig.normalizeColorHex(
                value,
                PvPOverlayConfig.DEFAULT_JUMP_RESET_EARLY_COLOR
        );
        CONFIG.save();
    }

    public static void setJumpResetLateColorHex(String value) {
        CONFIG.jumpResetLateColor = PvPOverlayConfig.normalizeColorHex(
                value,
                PvPOverlayConfig.DEFAULT_JUMP_RESET_LATE_COLOR
        );
        CONFIG.save();
    }

    public static void resetJumpResetPerfectColor() {
        CONFIG.jumpResetPerfectColor = PvPOverlayConfig.DEFAULT_JUMP_RESET_PERFECT_COLOR;
        CONFIG.save();
    }

    public static void resetJumpResetEarlyColor() {
        CONFIG.jumpResetEarlyColor = PvPOverlayConfig.DEFAULT_JUMP_RESET_EARLY_COLOR;
        CONFIG.save();
    }

    public static void resetJumpResetLateColor() {
        CONFIG.jumpResetLateColor = PvPOverlayConfig.DEFAULT_JUMP_RESET_LATE_COLOR;
        CONFIG.save();
    }

    public static int getForcedOtherPlayerMainHand() {
        return CONFIG.forcedOtherPlayerMainHand;
    }

    public static void cycleForcedOtherPlayerMainHand() {
        CONFIG.forcedOtherPlayerMainHand++;

        if (CONFIG.forcedOtherPlayerMainHand > 2) {
            CONFIG.forcedOtherPlayerMainHand = 0;
        }

        CONFIG.save();
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
        CONFIG.save();
    }

    public static int getConfigMenuBackgroundColor() {
        int alpha = Math.round(CONFIG.configMenuOpacityPercent * 255.0f / 100.0f);
        return (alpha << 24);
    }

    public static void drawConfigMenuBackdrop(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, getConfigMenuBackgroundColor());
    }

    public static void setOverlayPosition(int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();

        int boxWidth = getOverlayBoxWidth(minecraft);
        int boxHeight = getOverlayBoxHeight(minecraft);

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        CONFIG.customPosition = true;
        CONFIG.overlayX = clampInt(x, 0, Math.max(0, screenWidth - boxWidth));
        CONFIG.overlayY = clampInt(y, 0, Math.max(0, screenHeight - boxHeight));
        CONFIG.save();
    }

    public static void resetOverlayPosition() {
        CONFIG.customPosition = false;
        CONFIG.overlayX = 0;
        CONFIG.overlayY = 10;
        CONFIG.save();
    }

    public static int getOverlayX(Minecraft minecraft) {
        int boxWidth = getOverlayBoxWidth(minecraft);
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();

        if (CONFIG.customPosition) {
            return clampInt(CONFIG.overlayX, 0, Math.max(0, screenWidth - boxWidth));
        }

        return (screenWidth - boxWidth) / 2;
    }

    public static int getOverlayY(Minecraft minecraft) {
        int boxHeight = getOverlayBoxHeight(minecraft);
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        if (CONFIG.customPosition) {
            return clampInt(CONFIG.overlayY, 0, Math.max(0, screenHeight - boxHeight));
        }

        return 10;
    }

    public static int getOverlayBoxWidth(Minecraft minecraft) {
        int paddingX = 8;
        int textWidth = 0;

        if (CONFIG.showHit) {
            textWidth = Math.max(textWidth, minecraft.font.width(hitFullText()));
        }

        if (CONFIG.showTaken) {
            textWidth = Math.max(textWidth, minecraft.font.width(takenFullText()));
        }

        if (CONFIG.showJumpReset) {
            textWidth = Math.max(textWidth, minecraft.font.width(jumpResetFullText()));
        }

        if (textWidth == 0) {
            textWidth = minecraft.font.width(Component.translatable("overlay.pvp-overlay.title").getString());
        }

        return textWidth + paddingX * 2;
    }

    public static int getOverlayBoxHeight(Minecraft minecraft) {
        int paddingY = 8;
        int lineGap = 3;
        int fontHeight = minecraft.font.lineHeight;

        int visibleLines = getVisibleLineCount();

        if (visibleLines == 0) {
            visibleLines = 1;
        }

        int contentHeight = visibleLines * fontHeight + Math.max(0, visibleLines - 1) * lineGap;

        return contentHeight + paddingY * 2;
    }

    public static void drawOverlay(GuiGraphics graphics, Minecraft minecraft) {
        String hitPrefix = Component.translatable("overlay.pvp-overlay.hit").getString() + ": ";
        String takenPrefix = Component.translatable("overlay.pvp-overlay.taken").getString() + ": ";
        String jumpResetPrefix = Component.translatable("overlay.pvp-overlay.jump_reset").getString() + ": ";
        String suffix = " " + Component.translatable("overlay.pvp-overlay.blocks").getString();

        int paddingX = 8;
        int paddingY = 8;
        int lineGap = 3;
        int fontHeight = minecraft.font.lineHeight;

        int visibleLines = getVisibleLineCount();

        if (visibleLines == 0) {
            return;
        }

        int contentHeight = visibleLines * fontHeight + Math.max(0, visibleLines - 1) * lineGap;
        int boxWidth = getOverlayBoxWidth(minecraft);
        int boxHeight = contentHeight + paddingY * 2;

        int x = getOverlayX(minecraft);
        int y = getOverlayY(minecraft);

        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xAA000000);
        graphics.renderOutline(x, y, boxWidth, boxHeight, 0xFFFFFFFF);

        int textX = x + paddingX;
        int textY = y + (boxHeight - contentHeight) / 2;

        if (CONFIG.showHit) {
            drawDistanceLine(
                    graphics,
                    minecraft,
                    textX,
                    textY,
                    hitPrefix,
                    hitMainText,
                    hitRoundedDigit,
                    suffix
            );

            textY += fontHeight + lineGap;
        }

        if (CONFIG.showTaken) {
            drawDistanceLine(
                    graphics,
                    minecraft,
                    textX,
                    textY,
                    takenPrefix,
                    takenMainText,
                    takenRoundedDigit,
                    suffix
            );

            textY += fontHeight + lineGap;
        }

        if (CONFIG.showJumpReset) {
            drawSimpleLine(
                    graphics,
                    minecraft,
                    textX,
                    textY,
                    jumpResetPrefix,
                    jumpResetText,
                    jumpResetColor
            );
        }
    }

    private static int getVisibleLineCount() {
        int visibleLines = 0;

        if (CONFIG.showHit) {
            visibleLines++;
        }

        if (CONFIG.showTaken) {
            visibleLines++;
        }

        if (CONFIG.showJumpReset) {
            visibleLines++;
        }

        return visibleLines;
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

        graphics.drawString(
                minecraft.font,
                prefix,
                textX,
                y,
                0xFFFFFFFF,
                false
        );

        textX += minecraft.font.width(prefix);

        graphics.drawString(
                minecraft.font,
                mainText,
                textX,
                y,
                0xFFFFFFFF,
                false
        );

        textX += minecraft.font.width(mainText);

        graphics.drawString(
                minecraft.font,
                roundedDigit,
                textX,
                y,
                0xFF888888,
                false
        );

        textX += minecraft.font.width(roundedDigit);

        graphics.drawString(
                minecraft.font,
                suffix,
                textX,
                y,
                0xFFFFFFFF,
                false
        );
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

        graphics.drawString(
                minecraft.font,
                prefix,
                textX,
                y,
                0xFFFFFFFF,
                false
        );

        textX += minecraft.font.width(prefix);

        graphics.drawString(
                minecraft.font,
                value,
                textX,
                y,
                valueColor,
                false
        );
    }
}