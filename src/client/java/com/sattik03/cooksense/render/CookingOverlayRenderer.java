package com.sattik03.cooksense.render;

import com.sattik03.cooksense.config.CookSenseConfig;
import com.sattik03.cooksense.mixin.CampfireBlockEntityAccessor;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CookingOverlayRenderer {

    /**
     * Group representing one or more identical items cooking with similar progress.
     */
    private static class CookingGroup {
        final ItemStack item;
        int count;
        int minRemainingTicks;
        int maxRemainingTicks;
        final int totalTicks;

        CookingGroup(ItemStack item, int remainingTicks, int totalTicks) {
            this.item = item;
            this.count = 1;
            this.minRemainingTicks = remainingTicks;
            this.maxRemainingTicks = remainingTicks;
            this.totalTicks = totalTicks;
        }
    }

    public static void renderCampfireOverlay(
            CampfireBlockEntity campfire,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay
    ) {
        CookSenseConfig config = CookSenseConfig.getInstance();
        if (!config.enabled || !config.showOnCampfires) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        if (camera == null) {
            return;
        }

        // Distance check: full opacity 0-6 blocks, smoothly fades out 6-8 blocks, culled beyond 8
        BlockPos pos = campfire.getPos();
        Vec3d cameraPos = camera.getPos();
        double distSq = pos.getSquaredDistanceFromCenter(cameraPos.x, cameraPos.y, cameraPos.z);
        double maxDist = Math.max(4.0, config.renderDistance > 0 ? config.renderDistance : 8.0);
        double fadeDist = Math.max(2.0, maxDist - 2.0);

        if (distSq > maxDist * maxDist) {
            return;
        }

        double dist = Math.sqrt(distSq);
        float alpha = 1.0f;
        if (dist > fadeDist) {
            alpha = (float) ((maxDist - dist) / (maxDist - fadeDist));
        }
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        int alphaInt = (int) (alpha * 255.0f);
        if (alphaInt <= 5) {
            return;
        }

        DefaultedList<ItemStack> items = campfire.getItemsBeingCooked();
        CampfireBlockEntityAccessor accessor = (CampfireBlockEntityAccessor) campfire;
        int[] cookingTimes = accessor.getCookingTimes();
        int[] cookingTotalTimes = accessor.getCookingTotalTimes();

        if (cookingTimes == null || cookingTotalTimes == null) {
            return;
        }

        // Smart Grouping: group identical items if placed within less than 1 second (<= 20 ticks difference)
        List<CookingGroup> groups = new ArrayList<>();
        for (int i = 0; i < items.size(); ++i) {
            ItemStack item = items.get(i);
            if (item.isEmpty()) {
                continue;
            }

            int currentTicks = i < cookingTimes.length ? cookingTimes[i] : 0;
            int totalTicks = i < cookingTotalTimes.length ? cookingTotalTimes[i] : 0;

            if (totalTicks <= 0) {
                totalTicks = campfire.getRecipeFor(item)
                        .map(recipe -> recipe.value().getCookingTime())
                        .orElse(600);
                if (i < cookingTotalTimes.length) {
                    cookingTotalTimes[i] = totalTicks;
                }
            }

            int remainingTicks = Math.max(0, totalTicks - currentTicks);

            boolean merged = false;
            for (CookingGroup g : groups) {
                if (ItemStack.areItemsEqual(g.item, item)) {
                    if (Math.abs(g.minRemainingTicks - remainingTicks) <= 20 &&
                        Math.abs(g.maxRemainingTicks - remainingTicks) <= 20) {
                        g.count++;
                        g.minRemainingTicks = Math.min(g.minRemainingTicks, remainingTicks);
                        g.maxRemainingTicks = Math.max(g.maxRemainingTicks, remainingTicks);
                        merged = true;
                        break;
                    }
                }
            }

            if (!merged) {
                groups.add(new CookingGroup(item, remainingTicks, totalTicks));
            }
        }

        if (groups.isEmpty()) {
            return;
        }

        boolean isSoulCampfire = campfire.getCachedState().isOf(Blocks.SOUL_CAMPFIRE) && config.soulCampfireBlue;
        TextRenderer textRenderer = client.textRenderer;

        // Layout measurements: 9px icon + 4px gap + text + mini progress bar
        int iconSize = 9;
        int gap = 4;
        int rowHeight = 16; // Room for icon/label (9px) + mini progress bar (3px) + row padding
        int maxTextWidth = 0;

        for (CookingGroup g : groups) {
            String timeStr = TimerFormatter.formatTime(g.minRemainingTicks);
            String label = (g.count > 1 ? "x" + g.count + " " : "") + timeStr;
            int textWidth = textRenderer.getWidth(label);
            maxTextWidth = Math.max(maxTextWidth, textWidth);
        }

        int maxRowWidth = iconSize + gap + Math.max(28, maxTextWidth);
        int totalHeight = groups.size() * rowHeight;

        float startX = -(float) maxRowWidth / 2.0f;
        float startY = -(float) totalHeight / 2.0f;

        // Cozy padding around the plate
        float padX = 5.0f;
        float padY = 4.0f;
        float minX = startX - padX;
        float maxX = startX + maxRowWidth + padX;
        float minY = startY - padY;
        float maxY = startY + totalHeight + padY;

        // Cozy Warm Color Palette
        int borderAlpha = (int) (alpha * 180.0f);
        int bgAlpha = (int) (alpha * 190.0f); // ~75% opacity warm dark plate

        int borderColor = isSoulCampfire
                ? ((borderAlpha << 24) | 0x4FC3F7)  // Soul campfire: mystical cyan border
                : ((borderAlpha << 24) | 0xC4976A); // Normal campfire: warm roasted wood/amber border

        int bgColor = isSoulCampfire
                ? ((bgAlpha << 24) | 0x0A141C)      // Soul campfire: deep dark mystic navy
                : ((bgAlpha << 24) | 0x16100C);     // Normal campfire: cozy warm dark charcoal

        // Subtle heat updraft bobbing: breathes gently above the campfire
        float time = campfire.getWorld() != null ? campfire.getWorld().getTime() + tickDelta : 0;
        float bob = (float) Math.sin(time * 0.08f) * 0.02f;

        // Ceiling Detection: Check if there is a solid block directly above the campfire
        net.minecraft.world.World world = campfire.getWorld();
        boolean hasBlockAbove = false;
        if (world != null) {
            net.minecraft.block.BlockState stateAbove = world.getBlockState(pos.up());
            hasBlockAbove = !stateAbove.isAir() && !stateAbove.getCollisionShape(world, pos.up()).isEmpty();
        }

        // Adaptive placement:
        // - When seeThroughBlocks is enabled: Keep at natural height (Y=1.45f) and full scale (0.020f).
        //   All components (plate, borders, progress bars, text, item icons) render see-through without depth culling.
        // - When seeThroughBlocks is disabled AND a ceiling block is present:
        //   Position at Y=0.68f (centered between campfire grill at 0.44 and ceiling at 1.0) and offset
        //   horizontally towards the camera (along the X-Z plane) so it steps in front of the block.
        //   Additionally, clamp camera pitch so that looking down keeps the billboard vertical and NEVER
        //   tilts backwards into the ceiling block, while looking up tilts forward naturally.
        boolean adaptToCeiling = hasBlockAbove && !config.seeThroughBlocks;
        float baseY = adaptToCeiling ? 0.68f : 1.45f;
        float scaleFactor = adaptToCeiling ? 0.018f : 0.020f;
        float scale = scaleFactor * config.textScale;

        // Horizontal forward offset towards the camera in the X-Z plane
        double dx = cameraPos.x - (pos.getX() + 0.5);
        double dz = cameraPos.z - (pos.getZ() + 0.5);
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        float offX = 0.0f;
        float offZ = 0.0f;
        if (adaptToCeiling && horizDist > 0.001) {
            float forwardDist = (float) Math.min(0.65, horizDist * 0.45);
            offX = (float) (dx / horizDist) * forwardDist;
            offZ = (float) (dz / horizDist) * forwardDist;
        }

        matrices.push();
        matrices.translate(0.5f + offX, baseY + bob, 0.5f + offZ);
        if (adaptToCeiling) {
            // Clamped pitch: When looking down (pitch > 0), clamp to 0 so billboard stays vertical
            // and never tilts backwards into the ceiling block. Looking up (pitch < 0) tilts forward cleanly.
            float pitch = Math.min(0.0f, camera.getPitch());
            org.joml.Quaternionf rot = new org.joml.Quaternionf()
                    .rotationYXZ((float) Math.PI - camera.getYaw() * 0.017453292F, -pitch * 0.017453292F, 0.0F);
            matrices.multiply(rot);
        } else {
            matrices.multiply(camera.getRotation());
        }
        matrices.scale(scale, -scale, scale);

        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        // =========================================================================
        // PASS 1: Render all background quads in one contiguous batch.
        // Doing this before other renderers prevents BufferBuilder "Not building!" state errors.
        // Layer Z-Progression (+Z is in front, towards the player):
        //    0.000f: Cozy dark plate background
        //    0.001f: 1px hollow outer frame border
        //    0.005f: Progress bar recessed groove border
        //    0.008f: Progress bar empty track
        //    0.012f: Progress bar filled progress
        //    0.015f: Progress bar top bevel highlight
        //    0.030f: 2D item icons & text labels
        // =========================================================================
        RenderLayer bgLayer = config.seeThroughBlocks
                ? COOKSENSE_BG_SEE_THROUGH
                : COOKSENSE_BG_NORMAL;
        VertexConsumer bgConsumer = vertexConsumers.getBuffer(bgLayer);

        // 1a. Warm dark interior plate (solid backing)
        drawQuad(bgConsumer, posMatrix, minX, minY, maxX, maxY, 0.000f, bgColor, 0xF000F0);

        // 1b. Cozy 1px outer framing border (HOLLOW frame so it never covers the interior)
        drawHollowBorder(bgConsumer, posMatrix, minX - 1.0f, minY - 1.0f, maxX + 1.0f, maxY + 1.0f, 0.001f, borderColor, 0xF000F0);

        // 1c. Mini progress bar tracks and fills for each row
        for (int r = 0; r < groups.size(); ++r) {
            CookingGroup g = groups.get(r);
            float rowY = startY + (r * rowHeight);
            float barX = startX + iconSize + gap;
            float barY = rowY + 10.0f;
            float barWidth = Math.max(28.0f, maxTextWidth);
            float barHeight = 3.0f;

            int effectiveTotalTicks = Math.max(g.totalTicks, g.minRemainingTicks);
            float progress = effectiveTotalTicks > 0
                    ? 1.0f - ((float) g.minRemainingTicks / (float) effectiveTotalTicks)
                    : 0.0f;
            progress = Math.max(0.0f, Math.min(1.0f, progress));

            int barColorRgb = (g.minRemainingTicks <= 0)
                    ? 0x00FF88 // Flash emerald green when fully cooked
                    : (isSoulCampfire ? 0x00E5FF : 0xFFA726); // Vibrant, high-contrast, saturated
            int grooveBorderColor = isSoulCampfire
                    ? ((alphaInt << 24) | 0x1E5970)
                    : ((alphaInt << 24) | 0x5C3B24);
            int trackBgColor = (alphaInt << 24) | (isSoulCampfire ? 0x0A1620 : 0x1A120E);
            int barFillColor = (alphaInt << 24) | barColorRgb;

            // Recessed groove outline (1px hollow border strictly outside the bar - never overlaps the bar!)
            drawHollowBorder(bgConsumer, posMatrix, barX - 1.0f, barY - 1.0f, barX + barWidth + 1.0f, barY + barHeight + 1.0f, 0.005f, grooveBorderColor, 0xF000F0);

            float filledWidth = progress > 0.0f ? Math.min(barWidth, Math.max(1.5f, barWidth * progress)) : 0.0f;

            // Filled progress bar portion (barX to barX + filledWidth) at clean z = 0.005f
            if (filledWidth > 0.0f) {
                drawQuad(bgConsumer, posMatrix, barX, barY, barX + filledWidth, barY + barHeight, 0.005f, barFillColor, 0xF000F0);
            }

            // Empty track portion (ONLY for the remaining unfilled segment: barX + filledWidth to barX + barWidth) at z = 0.005f
            if (filledWidth < barWidth) {
                drawQuad(bgConsumer, posMatrix, barX + filledWidth, barY, barX + barWidth, barY + barHeight, 0.005f, trackBgColor, 0xF000F0);
            }
        }

        // =========================================================================
        // PASS 2: Render 2D item icons inside the plate (+0.030f towards camera)
        // When seeThroughBlocks is enabled, route item render layers through see-through
        // wrapper layers so item icons are also visible through blocks!
        // =========================================================================
        VertexConsumerProvider itemConsumers = config.seeThroughBlocks
                ? layer -> vertexConsumers.getBuffer(getSeeThroughLayer(layer))
                : vertexConsumers;

        for (int r = 0; r < groups.size(); ++r) {
            CookingGroup g = groups.get(r);
            float rowY = startY + (r * rowHeight);

            matrices.push();
            matrices.translate(startX + (iconSize / 2.0f), rowY + (iconSize / 2.0f), 0.030f);
            matrices.scale(iconSize, -iconSize, iconSize);
            client.getItemRenderer().renderItem(
                    g.item,
                    ModelTransformationMode.GUI,
                    0xF000F0,
                    OverlayTexture.DEFAULT_UV,
                    matrices,
                    itemConsumers,
                    campfire.getWorld(),
                    0
            );
            matrices.pop();
        }

        // =========================================================================
        // PASS 3: Render text labels (clean crisp white for premium cozy readability)
        // =========================================================================
        int textColor = (alphaInt << 24) | 0xFFFFFF;

        for (int r = 0; r < groups.size(); ++r) {
            CookingGroup g = groups.get(r);
            float rowY = startY + (r * rowHeight);

            String timeStr = TimerFormatter.formatTime(g.minRemainingTicks);
            String label = (g.count > 1 ? "x" + g.count + " " : "") + timeStr;

            float textX = startX + iconSize + gap;
            float textY = rowY;

            TextRenderer.TextLayerType layerType = config.seeThroughBlocks
                    ? TextRenderer.TextLayerType.SEE_THROUGH
                    : TextRenderer.TextLayerType.NORMAL;

            textRenderer.draw(
                    label,
                    textX,
                    textY,
                    textColor,
                    config.textShadow,
                    posMatrix,
                    vertexConsumers,
                    layerType,
                    0,
                    0xF000F0
            );
        }

        matrices.pop();
    }

    /**
     * Draws a 1-pixel hollow border frame around a rectangle.
     */
    private static void drawHollowBorder(VertexConsumer consumer, Matrix4f matrix, float x1, float y1, float x2, float y2, float z, int color, int light) {
        // Top 1px edge
        drawQuad(consumer, matrix, x1, y1, x2, y1 + 1.0f, z, color, light);
        // Bottom 1px edge
        drawQuad(consumer, matrix, x1, y2 - 1.0f, x2, y2, z, color, light);
        // Left 1px edge
        drawQuad(consumer, matrix, x1, y1 + 1.0f, x1 + 1.0f, y2 - 1.0f, z, color, light);
        // Right 1px edge
        drawQuad(consumer, matrix, x2 - 1.0f, y1 + 1.0f, x2, y2 - 1.0f, z, color, light);
    }

    /**
     * Emits a clean single-sided front quad.
     */
    private static void drawQuad(VertexConsumer consumer, Matrix4f matrix, float x1, float y1, float x2, float y2, float z, int color, int light) {
        consumer.vertex(matrix, x1, y1, z).color(color).light(light);
        consumer.vertex(matrix, x1, y2, z).color(color).light(light);
        consumer.vertex(matrix, x2, y2, z).color(color).light(light);
        consumer.vertex(matrix, x2, y1, z).color(color).light(light);
    }

    /**
     * Non-translucent variant of text background (see-through) that bypasses BuiltBuffer.sortQuads().
     * This prevents dynamic distance-based quad sorting from ever rendering the large background plate
     * on top of progress bars and groove borders when viewing from an angle or looking down.
     */
    private static final RenderLayer COOKSENSE_BG_SEE_THROUGH = new RenderLayer(
            "cooksense_bg_seethrough",
            VertexFormats.POSITION_COLOR_LIGHT,
            VertexFormat.DrawMode.QUADS,
            1536,
            false,
            false, // isTranslucent = false: disables BuiltBuffer.sortQuads()
            () -> {
                RenderLayer.getTextBackgroundSeeThrough().startDrawing();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
            },
            () -> {
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                RenderLayer.getTextBackgroundSeeThrough().endDrawing();
            }
    ) {};

    /**
     * Non-translucent variant of standard text background that bypasses BuiltBuffer.sortQuads().
     */
    private static final RenderLayer COOKSENSE_BG_NORMAL = new RenderLayer(
            "cooksense_bg_normal",
            VertexFormats.POSITION_COLOR_LIGHT,
            VertexFormat.DrawMode.QUADS,
            1536,
            false,
            false, // isTranslucent = false: disables BuiltBuffer.sortQuads()
            () -> {
                RenderLayer.getTextBackground().startDrawing();
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(false);
            },
            () -> {
                RenderLayer.getTextBackground().endDrawing();
            }
    ) {};

    private static final Map<RenderLayer, RenderLayer> SEE_THROUGH_LAYERS = new HashMap<>();

    /**
     * Creates or retrieves a see-through variant of an item RenderLayer with depth testing disabled.
     */
    private static RenderLayer getSeeThroughLayer(RenderLayer orig) {
        return SEE_THROUGH_LAYERS.computeIfAbsent(orig, l -> new RenderLayer(
                l.toString() + "_cooksense_seethrough",
                l.getVertexFormat(),
                l.getDrawMode(),
                l.getExpectedBufferSize(),
                l.hasCrumbling(),
                false, // isTranslucent = false ensures items retain draw order without CPU sorting
                () -> {
                    l.startDrawing();
                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                },
                () -> {
                    RenderSystem.depthMask(true);
                    RenderSystem.enableDepthTest();
                    l.endDrawing();
                }
        ) {});
    }
}
