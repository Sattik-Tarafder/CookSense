package com.sattik03.cooksense.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sattik03.cooksense.config.CookSenseConfig;
import com.sattik03.cooksense.mixin.CampfireBlockEntityAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

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
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        CookSenseConfig config = CookSenseConfig.getInstance();
        if (!config.enabled || !config.showOnCampfires) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        Camera camera = client.gameRenderer.getMainCamera();
        if (camera == null) {
            return;
        }

        // Distance check: full opacity 0-6 blocks, smoothly fades out 6-8 blocks, culled beyond 8
        BlockPos pos = campfire.getBlockPos();
        Vec3 cameraPos = camera.getPosition();
        double distSq = pos.distToCenterSqr(cameraPos.x, cameraPos.y, cameraPos.z);
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

        NonNullList<ItemStack> items = campfire.getItems();
        CampfireBlockEntityAccessor accessor = (CampfireBlockEntityAccessor) campfire;
        int[] cookingProgress = accessor.getCookingProgress();
        int[] cookingTime = accessor.getCookingTime();

        if (cookingProgress == null || cookingTime == null) {
            return;
        }

        // Smart Grouping: group identical items if placed within less than 1 second (<= 20 ticks difference)
        List<CookingGroup> groups = new ArrayList<>();
        for (int i = 0; i < items.size(); ++i) {
            ItemStack item = items.get(i);
            if (item.isEmpty()) {
                continue;
            }

            int currentTicks = i < cookingProgress.length ? cookingProgress[i] : 0;
            int totalTicks = i < cookingTime.length ? cookingTime[i] : 0;

            if (totalTicks <= 0) {
                totalTicks = campfire.getCookableRecipe(item)
                        .map(recipe -> recipe.value().getCookingTime())
                        .orElse(600);
                if (i < cookingTime.length) {
                    cookingTime[i] = totalTicks;
                }
            }

            int remainingTicks = Math.max(0, totalTicks - currentTicks);

            boolean merged = false;
            for (CookingGroup g : groups) {
                if (ItemStack.isSameItem(g.item, item)) {
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

        boolean isSoulCampfire = campfire.getBlockState().is(Blocks.SOUL_CAMPFIRE) && config.soulCampfireBlue;
        Font font = client.font;

        // Layout measurements: 9px icon + 4px gap + text + mini progress bar
        int iconSize = 9;
        int gap = 4;
        int rowHeight = 16; // Room for icon/label (9px) + mini progress bar (3px) + row padding
        int maxTextWidth = 0;

        for (CookingGroup g : groups) {
            String timeStr = TimerFormatter.formatTime(g.minRemainingTicks);
            String label = (g.count > 1 ? "x" + g.count + " " : "") + timeStr;
            int textWidth = font.width(label);
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
        Level level = campfire.getLevel();
        float time = level != null ? level.getGameTime() + partialTick : 0;
        float bob = (float) Math.sin(time * 0.08f) * 0.02f;

        // Ceiling Detection: Check if there is a solid block directly above the campfire
        boolean hasBlockAbove = false;
        if (level != null) {
            BlockState stateAbove = level.getBlockState(pos.above());
            hasBlockAbove = !stateAbove.isAir() && !stateAbove.getCollisionShape(level, pos.above()).isEmpty();
        }

        // Adaptive placement:
        // - When seeThroughBlocks is enabled: Keep at natural height (Y=1.45f) and full scale (0.020f).
        // - When seeThroughBlocks is disabled AND a ceiling block is present:
        //   Position at Y=0.68f and offset horizontally towards camera so it steps in front of the block.
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

        poseStack.pushPose();
        poseStack.translate(0.5f + offX, baseY + bob, 0.5f + offZ);
        if (adaptToCeiling) {
            // Clamped pitch: When looking down (pitch > 0), clamp to 0 so billboard stays vertical
            float pitch = Math.min(0.0f, camera.getXRot());
            Quaternionf rot = new Quaternionf()
                    .rotationYXZ((float) Math.PI - camera.getYRot() * 0.017453292F, -pitch * 0.017453292F, 0.0F);
            poseStack.mulPose(rot);
        } else {
            poseStack.mulPose(camera.rotation());
        }
        poseStack.scale(scale, -scale, scale);

        Matrix4f posMatrix = poseStack.last().pose();

        // =========================================================================
        // PASS 1: Render all background quads in one contiguous batch.
        // =========================================================================
        RenderType bgLayer = config.seeThroughBlocks
                ? COOKSENSE_BG_SEE_THROUGH
                : COOKSENSE_BG_NORMAL;
        VertexConsumer bgConsumer = bufferSource.getBuffer(bgLayer);

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
        // =========================================================================
        for (int r = 0; r < groups.size(); ++r) {
            CookingGroup g = groups.get(r);
            float rowY = startY + (r * rowHeight);

            poseStack.pushPose();
            poseStack.translate(startX + (iconSize / 2.0f), rowY + (iconSize / 2.0f), 0.030f);
            poseStack.scale(iconSize, -iconSize, iconSize);
            client.getItemRenderer().renderStatic(
                    g.item,
                    ItemDisplayContext.GUI,
                    0xF000F0,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    bufferSource,
                    campfire.getLevel(),
                    0
            );
            poseStack.popPose();
        }

        // =========================================================================
        // PASS 3: Render text labels (+0.015f towards camera in front of plate)
        // =========================================================================
        int textColor = (alphaInt << 24) | 0xFFFFFF;

        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, 0.015f);
        Matrix4f textMatrix = poseStack.last().pose();

        for (int r = 0; r < groups.size(); ++r) {
            CookingGroup g = groups.get(r);
            float rowY = startY + (r * rowHeight);

            String timeStr = TimerFormatter.formatTime(g.minRemainingTicks);
            String label = (g.count > 1 ? "x" + g.count + " " : "") + timeStr;

            float textX = startX + iconSize + gap;
            float textY = rowY;

            font.drawInBatch(
                    label,
                    textX,
                    textY,
                    textColor,
                    config.textShadow,
                    textMatrix,
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    0,
                    0xF000F0
            );
        }
        poseStack.popPose();

        poseStack.popPose();

        // Flush CookSense batches immediately so the overlay renders during the block entity pass
        // BEFORE translucent world geometry (water, ice, stained glass) is drawn.
        // This ensures translucent blocks in front properly tint the overlay, while translucent
        // blocks behind are correctly occluded and never bleed through.
        if (bufferSource instanceof MultiBufferSource.BufferSource immediate) {
            immediate.endBatch(bgLayer);
            immediate.endBatch();
        }
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
        consumer.addVertex(matrix, x1, y1, z).setColor(color).setLight(light);
        consumer.addVertex(matrix, x1, y2, z).setColor(color).setLight(light);
        consumer.addVertex(matrix, x2, y2, z).setColor(color).setLight(light);
        consumer.addVertex(matrix, x2, y1, z).setColor(color).setLight(light);
    }

    /**
     * See-through variant of background plate:
     * Uses GL_ALWAYS (519) so the plate punches through any occluding solid blocks/walls,
     * while writing depth (depthMask=true) so items and text render with full 3D solid depth
     * and translucent objects behind (water/ice) cannot bleed over it.
     */
    private static final RenderType COOKSENSE_BG_SEE_THROUGH = new RenderType(
            "cooksense_bg_seethrough",
            DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false, // sortOnUpload = false: disables BuiltBuffer CPU quad sorting
            () -> {
                RenderType.textBackground().setupRenderState();
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(519); // org.lwjgl.opengl.GL11.GL_ALWAYS
                RenderSystem.depthMask(true);
            },
            () -> {
                RenderSystem.depthMask(true);
                RenderSystem.depthFunc(515); // org.lwjgl.opengl.GL11.GL_LEQUAL
                RenderType.textBackground().clearRenderState();
            }
    ) {};

    /**
     * Standard variant of background plate:
     * Uses GL_LEQUAL depth testing so it is naturally occluded by solid blocks.
     */
    private static final RenderType COOKSENSE_BG_NORMAL = new RenderType(
            "cooksense_bg_normal",
            DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false, // sortOnUpload = false: disables BuiltBuffer CPU quad sorting
            () -> {
                RenderType.textBackground().setupRenderState();
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(515); // org.lwjgl.opengl.GL11.GL_LEQUAL
                RenderSystem.depthMask(true);
            },
            () -> {
                RenderSystem.depthMask(true);
                RenderSystem.depthFunc(515); // org.lwjgl.opengl.GL11.GL_LEQUAL
                RenderType.textBackground().clearRenderState();
            }
    ) {};
}
