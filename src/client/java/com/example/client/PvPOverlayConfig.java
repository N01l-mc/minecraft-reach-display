package com.example.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PvPOverlayConfig {
    public static final int DEFAULT_JUMP_RESET_PAIR_WINDOW_TICKS = 6;
    public static final int DEFAULT_JUMP_RESET_DISPLAY_TICKS = 10;

    public static final int MIN_JUMP_RESET_PAIR_WINDOW_TICKS = 0;
    public static final int MAX_JUMP_RESET_PAIR_WINDOW_TICKS = 10;

    public static final int MIN_JUMP_RESET_DISPLAY_TICKS = 1;
    public static final int MAX_JUMP_RESET_DISPLAY_TICKS = 20;

    public static final int DEFAULT_JUMP_RESET_UNIT_MODE = 0;

    public static final String DEFAULT_JUMP_RESET_PERFECT_COLOR = "#55FF55";
    public static final String DEFAULT_JUMP_RESET_EARLY_COLOR = "#FFFF55";
    public static final String DEFAULT_JUMP_RESET_LATE_COLOR = "#FF5555";

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

    // 0 = T, 1 = ticks, 2 = Ticks, 3 = none
    public int jumpResetUnitMode = DEFAULT_JUMP_RESET_UNIT_MODE;

    public String jumpResetPerfectColor = DEFAULT_JUMP_RESET_PERFECT_COLOR;
    public String jumpResetEarlyColor = DEFAULT_JUMP_RESET_EARLY_COLOR;
    public String jumpResetLateColor = DEFAULT_JUMP_RESET_LATE_COLOR;

    public boolean customPosition = false;
    public int overlayX = 0;
    public int overlayY = 10;

    public int configMenuOpacityPercent = 25;

    // 0 = off, 1 = force left, 2 = force right
    public int forcedOtherPlayerMainHand = 0;

    public static PvPOverlayConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            PvPOverlayConfig config = new PvPOverlayConfig();
            config.save();
            return config;
        }

        try {
            String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            PvPOverlayConfig config = GSON.fromJson(json, PvPOverlayConfig.class);

            if (config == null) {
                config = new PvPOverlayConfig();
            }

            config.configMenuOpacityPercent = clampInt(config.configMenuOpacityPercent, 0, 100);
            config.forcedOtherPlayerMainHand = clampInt(config.forcedOtherPlayerMainHand, 0, 2);

            config.jumpResetPairWindowTicks = clampInt(
                    config.jumpResetPairWindowTicks,
                    MIN_JUMP_RESET_PAIR_WINDOW_TICKS,
                    MAX_JUMP_RESET_PAIR_WINDOW_TICKS
            );

            config.jumpResetDisplayTicks = clampInt(
                    config.jumpResetDisplayTicks,
                    MIN_JUMP_RESET_DISPLAY_TICKS,
                    MAX_JUMP_RESET_DISPLAY_TICKS
            );

            config.jumpResetUnitMode = clampInt(config.jumpResetUnitMode, 0, 3);

            if (!isValidColorHex(config.jumpResetPerfectColor)) {
                config.jumpResetPerfectColor = DEFAULT_JUMP_RESET_PERFECT_COLOR;
            }

            if (!isValidColorHex(config.jumpResetEarlyColor)) {
                config.jumpResetEarlyColor = DEFAULT_JUMP_RESET_EARLY_COLOR;
            }

            if (!isValidColorHex(config.jumpResetLateColor)) {
                config.jumpResetLateColor = DEFAULT_JUMP_RESET_LATE_COLOR;
            }

            return config;
        } catch (Exception exception) {
            System.err.println("[PvP Overlay] Failed to load config. Using defaults.");
            exception.printStackTrace();

            return new PvPOverlayConfig();
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

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}