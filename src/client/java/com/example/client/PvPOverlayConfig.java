package com.example.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PvPOverlayConfig {
    public static final int DEFAULT_JUMP_RESET_PAIR_WINDOW_TICKS = 6;
    public static final int DEFAULT_JUMP_RESET_DISPLAY_TICKS = 10;

    public static final int MIN_JUMP_RESET_PAIR_WINDOW_TICKS = 0;
    public static final int MAX_JUMP_RESET_PAIR_WINDOW_TICKS = 10;

    public static final int MIN_JUMP_RESET_DISPLAY_TICKS = 1;
    public static final int MAX_JUMP_RESET_DISPLAY_TICKS = 20;

    public static final int DEFAULT_JUMP_RESET_UNIT_MODE = 0;
    public static final boolean DEFAULT_JUMP_RESET_REQUIRE_SPRINT = true;
    public static final boolean DEFAULT_JUMP_RESET_SHOW_TIMING_LABELS = true;

    public static final String DEFAULT_JUMP_RESET_PERFECT_COLOR = "#55FF55";
    public static final String DEFAULT_JUMP_RESET_EARLY_COLOR = "#FFFF55";
    public static final String DEFAULT_JUMP_RESET_LATE_COLOR = "#FF5555";

    public static final String MODULE_HIT = "hit";
    public static final String MODULE_TAKEN = "taken";
    public static final String MODULE_JUMP_RESET = "jump_reset";

    public static final String DEFAULT_GROUP_BACKGROUND_COLOR = "#000000";
    public static final String DEFAULT_GROUP_BORDER_COLOR = "#FFFFFF";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("pvp-overlay.json");

    public boolean overlayEnabled = true;
    public boolean showHit = true;
    public boolean showTaken = true;
    public boolean showJumpReset = true;

    public int jumpResetPairWindowTicks = DEFAULT_JUMP_RESET_PAIR_WINDOW_TICKS;
    public int jumpResetDisplayTicks = DEFAULT_JUMP_RESET_DISPLAY_TICKS;

    // 0 = T, 1 = ticks, 2 = Ticks, 3 = none, 4 = t
    public int jumpResetUnitMode = DEFAULT_JUMP_RESET_UNIT_MODE;

    public boolean jumpResetRequireSprint = DEFAULT_JUMP_RESET_REQUIRE_SPRINT;
    public boolean jumpResetShowTimingLabels = DEFAULT_JUMP_RESET_SHOW_TIMING_LABELS;

    public String jumpResetPerfectColor = DEFAULT_JUMP_RESET_PERFECT_COLOR;
    public String jumpResetEarlyColor = DEFAULT_JUMP_RESET_EARLY_COLOR;
    public String jumpResetLateColor = DEFAULT_JUMP_RESET_LATE_COLOR;

    public boolean customPosition = false;
    public int overlayX = 0;
    public int overlayY = 10;

    public int configMenuOpacityPercent = 25;

    // 0 = off, 1 = force left, 2 = force right
    public int forcedOtherPlayerMainHand = 0;

    public ArrayList<OverlayGroupConfig> overlayGroups = new ArrayList<>();

    public static PvPOverlayConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            PvPOverlayConfig config = new PvPOverlayConfig();
            config.ensureLayoutGroups();
            config.save();
            return config;
        }

        try {
            String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            PvPOverlayConfig config = GSON.fromJson(json, PvPOverlayConfig.class);

            if (config == null) {
                config = new PvPOverlayConfig();
            }

            config.clampAndRepair();
            config.ensureLayoutGroups();

            return config;
        } catch (Exception exception) {
            System.err.println("[PvP Overlay] Failed to load config. Using defaults.");
            exception.printStackTrace();

            PvPOverlayConfig config = new PvPOverlayConfig();
            config.ensureLayoutGroups();
            return config;
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            String json = GSON.toJson(this);
            Files.writeString(CONFIG_PATH, json, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            System.err.println("[PvP Overlay] Failed to save config.");
            exception.printStackTrace();
        }
    }

    public void clampAndRepair() {
        configMenuOpacityPercent = clampInt(configMenuOpacityPercent, 0, 100);
        forcedOtherPlayerMainHand = clampInt(forcedOtherPlayerMainHand, 0, 2);

        jumpResetPairWindowTicks = clampInt(
                jumpResetPairWindowTicks,
                MIN_JUMP_RESET_PAIR_WINDOW_TICKS,
                MAX_JUMP_RESET_PAIR_WINDOW_TICKS
        );

        jumpResetDisplayTicks = clampInt(
                jumpResetDisplayTicks,
                MIN_JUMP_RESET_DISPLAY_TICKS,
                MAX_JUMP_RESET_DISPLAY_TICKS
        );

        jumpResetUnitMode = clampInt(jumpResetUnitMode, 0, 4);

        if (!isValidColorHex(jumpResetPerfectColor)) {
            jumpResetPerfectColor = DEFAULT_JUMP_RESET_PERFECT_COLOR;
        }

        if (!isValidColorHex(jumpResetEarlyColor)) {
            jumpResetEarlyColor = DEFAULT_JUMP_RESET_EARLY_COLOR;
        }

        if (!isValidColorHex(jumpResetLateColor)) {
            jumpResetLateColor = DEFAULT_JUMP_RESET_LATE_COLOR;
        }

        if (overlayGroups == null) {
            overlayGroups = new ArrayList<>();
        }

        Set<String> usedModules = new HashSet<>();
        ArrayList<OverlayGroupConfig> repairedGroups = new ArrayList<>();

        for (OverlayGroupConfig group : overlayGroups) {
            if (group == null) {
                continue;
            }

            group.repair();

            ArrayList<String> repairedModules = new ArrayList<>();

            if (group.modules != null) {
                for (String moduleId : group.modules) {
                    if (!isKnownModule(moduleId)) {
                        continue;
                    }

                    if (usedModules.contains(moduleId)) {
                        continue;
                    }

                    usedModules.add(moduleId);
                    repairedModules.add(moduleId);
                }
            }

            group.modules = repairedModules;

            if (!group.modules.isEmpty()) {
                repairedGroups.add(group);
            }
        }

        overlayGroups = repairedGroups;
    }

    public void ensureLayoutGroups() {
        if (overlayGroups == null) {
            overlayGroups = new ArrayList<>();
        }

        if (overlayGroups.isEmpty()) {
            OverlayGroupConfig group = OverlayGroupConfig.createDefault(
                    "group-main",
                    customPosition ? overlayX : 0,
                    customPosition ? overlayY : 10
            );

            group.showBox = false;
            group.showBorder = false;
            group.modules.add(MODULE_HIT);
            group.modules.add(MODULE_TAKEN);
            group.modules.add(MODULE_JUMP_RESET);

            overlayGroups.add(group);
        }

        ensureEnabledModulesHaveGroups();
    }

    public void ensureEnabledModulesHaveGroups() {
        ensureModuleHasGroupIfEnabled(MODULE_HIT, showHit);
        ensureModuleHasGroupIfEnabled(MODULE_TAKEN, showTaken);
        ensureModuleHasGroupIfEnabled(MODULE_JUMP_RESET, showJumpReset);
    }

    private void ensureModuleHasGroupIfEnabled(String moduleId, boolean enabled) {
        if (!enabled) {
            return;
        }

        if (isModuleInAnyGroup(moduleId)) {
            return;
        }

        OverlayGroupConfig group = OverlayGroupConfig.createDefault(
                "group-" + moduleId + "-" + System.currentTimeMillis(),
                10,
                10 + overlayGroups.size() * 24
        );

        group.showBox = false;
        group.showBorder = false;
        group.modules.add(moduleId);

        overlayGroups.add(group);
    }

    public boolean isModuleInAnyGroup(String moduleId) {
        if (overlayGroups == null) {
            return false;
        }

        for (OverlayGroupConfig group : overlayGroups) {
            if (group.modules != null && group.modules.contains(moduleId)) {
                return true;
            }
        }

        return false;
    }

    public OverlayGroupConfig findGroup(String groupId) {
        if (overlayGroups == null || groupId == null) {
            return null;
        }

        for (OverlayGroupConfig group : overlayGroups) {
            if (groupId.equals(group.id)) {
                return group;
            }
        }

        return null;
    }

    public void removeEmptyGroups() {
        if (overlayGroups == null) {
            overlayGroups = new ArrayList<>();
            return;
        }

        overlayGroups.removeIf(group -> group == null || group.modules == null || group.modules.isEmpty());
    }

    public static boolean isKnownModule(String moduleId) {
        return MODULE_HIT.equals(moduleId) ||
                MODULE_TAKEN.equals(moduleId) ||
                MODULE_JUMP_RESET.equals(moduleId);
    }

    public static boolean isValidColorHex(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim();

        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        if (normalized.length() != 6) {
            return false;
        }

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);

            boolean isDigit = c >= '0' && c <= '9';
            boolean isLowerHex = c >= 'a' && c <= 'f';
            boolean isUpperHex = c >= 'A' && c <= 'F';

            if (!isDigit && !isLowerHex && !isUpperHex) {
                return false;
            }
        }

        return true;
    }

    public static String normalizeColorHex(String value, String fallback) {
        if (!isValidColorHex(value)) {
            return fallback;
        }

        String normalized = value.trim();

        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        return "#" + normalized.toUpperCase();
    }

    public static int colorWithOpacity(String colorHex, int opacityPercent, int fallbackRgb) {
        int rgb = parseRgb(colorHex, fallbackRgb) & 0x00FFFFFF;
        int alpha = Math.round(clampInt(opacityPercent, 0, 100) * 255.0f / 100.0f);

        return (alpha << 24) | rgb;
    }

    public static int parseRgb(String value, int fallback) {
        if (!isValidColorHex(value)) {
            return fallback;
        }

        String normalized = value.trim();

        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        return 0xFF000000 | Integer.parseInt(normalized, 16);
    }

    public static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static class OverlayGroupConfig {
        public String id = "";
        public int x = 0;
        public int y = 10;

        public boolean showBox = false;
        public String backgroundColor = DEFAULT_GROUP_BACKGROUND_COLOR;
        public int backgroundOpacityPercent = 67;

        public boolean showBorder = false;
        public String borderColor = DEFAULT_GROUP_BORDER_COLOR;
        public int borderOpacityPercent = 100;

        public int borderRadius = 0;

        public int paddingX = 8;
        public int paddingY = 8;
        public int lineGap = 3;

        public int scalePercent = 100;

        public boolean hideWhenPlayerListOpen = false;

        public ArrayList<String> modules = new ArrayList<>();

        public static OverlayGroupConfig createDefault(String id, int x, int y) {
            OverlayGroupConfig group = new OverlayGroupConfig();
            group.id = id;
            group.x = x;
            group.y = y;
            return group;
        }

        public void repair() {
            if (id == null || id.isBlank()) {
                id = "group-" + System.currentTimeMillis();
            }

            backgroundColor = normalizeColorHex(backgroundColor, DEFAULT_GROUP_BACKGROUND_COLOR);
            borderColor = normalizeColorHex(borderColor, DEFAULT_GROUP_BORDER_COLOR);

            backgroundOpacityPercent = clampInt(backgroundOpacityPercent, 0, 100);
            borderOpacityPercent = clampInt(borderOpacityPercent, 0, 100);

            borderRadius = clampInt(borderRadius, 0, 16);

            paddingX = clampInt(paddingX, 0, 32);
            paddingY = clampInt(paddingY, 0, 32);

            lineGap = clampInt(lineGap, 0, 20);
            scalePercent = clampInt(scalePercent, 50, 200);

            if (modules == null) {
                modules = new ArrayList<>();
            }
        }
    }
}