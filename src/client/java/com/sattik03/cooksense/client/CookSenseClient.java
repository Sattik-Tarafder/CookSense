package com.sattik03.cooksense.client;

import com.sattik03.cooksense.config.CookSenseConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class CookSenseClient implements ClientModInitializer {

    public static final String MOD_ID = "cooksense";
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        // Load persistent config
        CookSenseConfig.getInstance();

        // Register toggle keybind (default: 'K')
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cooksense.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "key.cooksense.category"
        ));

        // Listen for key presses every client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                CookSenseConfig config = CookSenseConfig.getInstance();
                config.enabled = !config.enabled;
                config.save();

                if (client.player != null) {
                    Text message = Text.translatable(config.enabled ? "message.cooksense.toggle.on" : "message.cooksense.toggle.off");
                    client.player.sendMessage(message, true); // true sends to Action Bar
                }
            }
        });

        System.out.println("[CookSense] Initialized successfully.");
    }
}
