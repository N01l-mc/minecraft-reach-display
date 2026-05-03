package com.example.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PvPOverlayConfig {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("pvp-overlay.json");

    public boolean overlayEnabled = true;
    public boolean showHit = true;
    public boolean showTaken = true;

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
}