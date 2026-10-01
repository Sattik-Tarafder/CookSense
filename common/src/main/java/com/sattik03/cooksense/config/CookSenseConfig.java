package com.sattik03.cooksense.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sattik03.cooksense.platform.Services;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CookSenseConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = Services.PLATFORM.getConfigDirectory().resolve("cooksense.json").toFile();

    private static CookSenseConfig INSTANCE;

    public boolean enabled = true;
    public double renderDistance = 8.0;
    public float textScale = 1.0f;
    public boolean showOnCampfires = true;
    public boolean soulCampfireBlue = true;
    public boolean seeThroughBlocks = false;
    public boolean textShadow = true;

    public static CookSenseConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static CookSenseConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                CookSenseConfig config = GSON.fromJson(reader, CookSenseConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (Exception e) {
                System.err.println("[CookSense] Failed to load config, resetting to defaults: " + e.getMessage());
            }
        }
        CookSenseConfig config = new CookSenseConfig();
        config.save();
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            System.err.println("[CookSense] Failed to save config: " + e.getMessage());
        }
    }
}
