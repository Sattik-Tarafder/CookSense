package com.sattik03.cooksense;

import com.mojang.blaze3d.platform.InputConstants;
import com.sattik03.cooksense.config.CookSenseConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod("cooksense")
public class CookSenseNeoForge {

    private static KeyMapping toggleKey;

    public CookSenseNeoForge(IEventBus eventBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            // Initialize config
            CookSenseConfig.getInstance();

            // Register key mappings on mod event bus
            eventBus.addListener(RegisterKeyMappingsEvent.class, event -> {
                toggleKey = new KeyMapping(
                        "key.cooksense.toggle",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_K,
                        "key.cooksense.category"
                );
                event.register(toggleKey);
            });

            // Listen for key presses on NeoForge event bus
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
                if (toggleKey != null && Minecraft.getInstance().player != null) {
                    while (toggleKey.consumeClick()) {
                        CookSenseConfig config = CookSenseConfig.getInstance();
                        config.enabled = !config.enabled;
                        config.save();

                        Component message = Component.translatable(config.enabled ? "message.cooksense.toggle.on" : "message.cooksense.toggle.off");
                        Minecraft.getInstance().player.displayClientMessage(message, true);
                    }
                }
            });

            System.out.println("[CookSense] NeoForge Client initialized successfully.");
        }
    }
}
