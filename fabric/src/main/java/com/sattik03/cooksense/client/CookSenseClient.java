package com.sattik03.cooksense.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sattik03.cooksense.config.CookSenseConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class CookSenseClient implements ClientModInitializer {

    public static final String MOD_ID = "cooksense";
    private static final KeyMapping.Category COOKSENSE_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("cooksense", "category"));
    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        // Load persistent config
        CookSenseConfig.getInstance();

        // Register toggle keybind (default: 'K')
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.cooksense.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                COOKSENSE_CATEGORY
        ));

        // Listen for key presses every client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                CookSenseConfig config = CookSenseConfig.getInstance();
                config.enabled = !config.enabled;
                config.save();

                if (client.player != null) {
                    Component message = Component.translatable(config.enabled ? "message.cooksense.toggle.on" : "message.cooksense.toggle.off");
                    client.player.sendOverlayMessage(message);
                }
            }
        });

        System.out.println("[CookSense] Fabric Client initialized successfully.");
    }
}
