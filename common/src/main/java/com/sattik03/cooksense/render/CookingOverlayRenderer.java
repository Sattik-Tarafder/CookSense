package com.sattik03.cooksense.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sattik03.cooksense.config.CookSenseConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.CampfireRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;

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
            CampfireRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState
    ) {
        CookSenseConfig config = CookSenseConfig.getInstance();
        if (!config.enabled || !config.showOnCampfires) {
            return;
        }

        CookSenseCampfireRenderState ext = (CookSenseCampfireRenderState) state;
        List<ItemStack> items = ext.cooksense$getItems();
        int[] cookingProgress = ext.cooksense$getCookingProgress();
        int[] cookingTime = ext.cooksense$getCookingTime();

        if (items == null || items.isEmpty() || cookingProgress == null || cookingTime == null) {
            return;
        }

        // Distance check: full opacity 0-6 blocks, smoothly fades out 6-8 blocks, culled beyond 8
        BlockPos pos = state.blockPos;
        if (pos == null) {
            return;
        }

        Vec3 cameraPos = cameraRenderState.pos;
        if (cameraPos == null) {
            return;
        }

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
                totalTicks = getCampfireCookingTime(Minecraft.getInstance().level, item);
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

        boolean isSoulCampfire = ext.cooksense$isSoulCampfire() && config.soulCampfireBlue;
        Font font = Minecraft.getInstance().font;

        // Layout measurements: 9px icon + 4px gap + text + mini progress bar
        int iconSize = 9;
        int gap = 4;
        int rowHeight = 16;
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
        int bgAlpha = (int) (alpha * 190.0f);

        int borderColor = isSoulCampfire
                ? ((borderAlpha << 24) | 0x4FC3F7)
                : ((borderAlpha << 24) | 0xC4976A);

        int bgColor = isSoulCampfire
                ? ((bgAlpha << 24) | 0x0A141C)
                : ((bgAlpha << 24) | 0x16100C);

        // Subtle heat updraft bobbing: breathes gently above the campfire
        float gameTime = ext.cooksense$getGameTime();
        float bob = (float) Math.sin(gameTime * 0.08f) * 0.02f;

        boolean hasBlockAbove = ext.cooksense$hasBlockAbove();
        boolean adaptToCeiling = hasBlockAbove && !config.seeThroughBlocks;
        float baseY = adaptToCeiling ? 0.68f : 1.45f;
        float scaleFactor = adaptToCeiling ? 0.018f : 0.020f;
        float scale = scaleFactor * config.textScale;

        // Horizontal forward offset towards camera
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
            Camera mainCam = Minecraft.getInstance().gameRenderer.getMainCamera();
            if (mainCam != null) {
                float pitch = Math.min(0.0f, mainCam.xRot());
                Quaternionf rot = new Quaternionf()
                        .rotationYXZ((float) Math.PI - mainCam.yRot() * 0.017453292F, -pitch * 0.017453292F, 0.0F);
                poseStack.mulPose(rot);
            } else {
                poseStack.mulPose(cameraRenderState.orientation);
            }
        } else {
            poseStack.mulPose(cameraRenderState.orientation);
        }
        poseStack.scale(scale, -scale, scale);

        // =========================================================================
        // PASS 1: Render background quads and progress bars via submitCustomGeometry
        // =========================================================================
        final int fMaxTextWidth = maxTextWidth;
        final boolean fIsSoulCampfire = isSoulCampfire;
        final int fAlphaInt = alphaInt;
        final int fBorderColor = borderColor;
        final int fBgColor = bgColor;

        SubmitNodeCollector.CustomGeometryRenderer bgDrawer = (pose, consumer) -> {
            Matrix4f posMatrix = pose.pose();

            // 1a. Warm dark interior plate
            drawQuad(consumer, posMatrix, minX, minY, maxX, maxY, 0.000f, fBgColor, 0xF000F0);

            // 1b. Cozy 1px outer framing border
            drawHollowBorder(consumer, posMatrix, minX - 1.0f, minY - 1.0f, maxX + 1.0f, maxY + 1.0f, 0.001f, fBorderColor, 0xF000F0);

            // 1c. Mini progress bar tracks and fills for each row
            for (int r = 0; r < groups.size(); ++r) {
                CookingGroup g = groups.get(r);
                float rowY = startY + (r * rowHeight);
                float barX = startX + iconSize + gap;
                float barY = rowY + 10.0f;
                float barWidth = Math.max(28.0f, fMaxTextWidth);
                float barHeight = 3.0f;

                int effectiveTotalTicks = Math.max(g.totalTicks, g.minRemainingTicks);
                float progress = effectiveTotalTicks > 0
                        ? 1.0f - ((float) g.minRemainingTicks / (float) effectiveTotalTicks)
                        : 0.0f;
                progress = Math.max(0.0f, Math.min(1.0f, progress));

                int barColorRgb = (g.minRemainingTicks <= 0)
                        ? 0x00FF88
                        : (fIsSoulCampfire ? 0x00E5FF : 0xFFA726);
                int grooveBorderColor = fIsSoulCampfire
                        ? ((fAlphaInt << 24) | 0x1E5970)
                        : ((fAlphaInt << 24) | 0x5C3B24);
                int trackBgColor = (fAlphaInt << 24) | (fIsSoulCampfire ? 0x0A1620 : 0x1A120E);
                int barFillColor = (fAlphaInt << 24) | barColorRgb;

                drawHollowBorder(consumer, posMatrix, barX - 1.0f, barY - 1.0f, barX + barWidth + 1.0f, barY + barHeight + 1.0f, 0.005f, grooveBorderColor, 0xF000F0);

                float filledWidth = progress > 0.0f ? Math.min(barWidth, Math.max(1.5f, barWidth * progress)) : 0.0f;

                if (filledWidth > 0.0f) {
                    drawQuad(consumer, posMatrix, barX, barY, barX + filledWidth, barY + barHeight, 0.005f, barFillColor, 0xF000F0);
                }

                if (filledWidth < barWidth) {
                    drawQuad(consumer, posMatrix, barX + filledWidth, barY, barX + barWidth, barY + barHeight, 0.005f, trackBgColor, 0xF000F0);
                }
            }
        };

        // Submit background plate to Order 1 so it is guaranteed to render BEFORE foreground text and items (Order 2)
        net.minecraft.client.renderer.OrderedSubmitNodeCollector bgCollector = submitNodeCollector.order(1);
        net.minecraft.client.renderer.OrderedSubmitNodeCollector fgCollector = submitNodeCollector.order(2);
        SubmitNodeCollector fgNodeCollector = new OrderedSubmitNodeCollectorAdapter(submitNodeCollector, 2);

        // If see-through is requested, submit the see-through layer so the plate punches through occluding solid blocks
        if (config.seeThroughBlocks) {
            bgCollector.submitCustomGeometry(poseStack, RenderTypes.textBackgroundSeeThrough(), bgDrawer);
        }

        // Always submit the standard textBackground layer which writes depth (depthMask=true)
        // so that smoke particles behind the campfire cannot bleed through the plate in direct line-of-sight
        bgCollector.submitCustomGeometry(poseStack, RenderTypes.textBackground(), bgDrawer);

        // =========================================================================
        // PASS 2: Render 2D item icons inside the plate (+0.030f towards camera)
        // =========================================================================
        ItemModelResolver itemModelResolver = Minecraft.getInstance().getItemModelResolver();
        Level level = Minecraft.getInstance().level;

        for (int r = 0; r < groups.size(); ++r) {
            CookingGroup g = groups.get(r);
            float rowY = startY + (r * rowHeight);

            poseStack.pushPose();
            poseStack.translate(startX + (iconSize / 2.0f), rowY + (iconSize / 2.0f), 0.030f);
            poseStack.scale(iconSize, -iconSize, iconSize);

            ItemStackRenderState itemState = new ItemStackRenderState();
            itemModelResolver.updateForTopItem(itemState, g.item, ItemDisplayContext.GUI, level, null, 0);
            itemState.submit(poseStack, fgNodeCollector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);

            poseStack.popPose();
        }

        // =========================================================================
        // PASS 3: Render text labels (+0.015f towards camera in front of plate)
        // =========================================================================
        int textColor = (alphaInt << 24) | 0xFFFFFF;
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, 0.015f);

        for (int r = 0; r < groups.size(); ++r) {
            CookingGroup g = groups.get(r);
            float rowY = startY + (r * rowHeight);

            String timeStr = TimerFormatter.formatTime(g.minRemainingTicks);
            String label = (g.count > 1 ? "x" + g.count + " " : "") + timeStr;

            float textX = startX + iconSize + gap;
            float textY = rowY;
            var visualText = Component.literal(label).getVisualOrderText();

            // When seeThroughBlocks is enabled, submit a SEE_THROUGH text pass (with dropShadow=false)
            // so it punches through occluding walls, matching vanilla nametag behavior.
            if (config.seeThroughBlocks) {
                fgCollector.submitText(
                        poseStack,
                        textX,
                        textY,
                        visualText,
                        false,
                        Font.DisplayMode.SEE_THROUGH,
                        0xF000F0,
                        (Math.min(alphaInt, 0x80) << 24) | 0xFFFFFF,
                        0,
                        0
                );
            }

            // Always submit the NORMAL text pass with crisp colors, full brightness, and drop shadows
            fgCollector.submitText(
                    poseStack,
                    textX,
                    textY,
                    visualText,
                    config.textShadow,
                    Font.DisplayMode.NORMAL,
                    0xF000F0,
                    textColor,
                    0,
                    0
            );
        }
        poseStack.popPose();

        poseStack.popPose();
    }

    /**
     * Draws a 1-pixel hollow border frame around a rectangle.
     */
    private static void drawHollowBorder(VertexConsumer consumer, Matrix4f matrix, float x1, float y1, float x2, float y2, float z, int color, int light) {
        drawQuad(consumer, matrix, x1, y1, x2, y1 + 1.0f, z, color, light);
        drawQuad(consumer, matrix, x1, y2 - 1.0f, x2, y2, z, color, light);
        drawQuad(consumer, matrix, x1, y1 + 1.0f, x1 + 1.0f, y2 - 1.0f, z, color, light);
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
     * Resolves campfire cooking duration fallback (600 ticks = 30 seconds).
     */
     public static int getCampfireCookingTime(Level level, ItemStack stack) {
         return 600;
     }

    /**
     * Adapter wrapping an OrderedSubmitNodeCollector into a SubmitNodeCollector,
     * ensuring that submissions routed through ItemStackRenderState are placed into the desired order.
     */
    private static class OrderedSubmitNodeCollectorAdapter implements SubmitNodeCollector {
        private final SubmitNodeCollector root;
        private final OrderedSubmitNodeCollector delegate;

        OrderedSubmitNodeCollectorAdapter(SubmitNodeCollector root, int order) {
            this.root = root;
            this.delegate = root.order(order);
        }

        @Override
        public OrderedSubmitNodeCollector order(int order) {
            return root.order(order);
        }

        @Override
        public void submitShadow(PoseStack poseStack, float f, List<EntityRenderState.ShadowPiece> list) {
            delegate.submitShadow(poseStack, f, list);
        }

        @Override
        public void submitNameTag(PoseStack poseStack, Vec3 vec3, int i, Component component, boolean bl, int j, double d, CameraRenderState cameraRenderState) {
            delegate.submitNameTag(poseStack, vec3, i, component, bl, j, d, cameraRenderState);
        }

        @Override
        public void submitText(PoseStack poseStack, float f, float g, FormattedCharSequence formattedCharSequence, boolean bl, Font.DisplayMode displayMode, int i, int j, int k, int l) {
            delegate.submitText(poseStack, f, g, formattedCharSequence, bl, displayMode, i, j, k, l);
        }

        @Override
        public void submitFlame(PoseStack poseStack, EntityRenderState entityRenderState, Quaternionf quaternionf) {
            delegate.submitFlame(poseStack, entityRenderState, quaternionf);
        }

        @Override
        public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
            delegate.submitLeash(poseStack, leashState);
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S object, PoseStack poseStack, RenderType renderType, int i, int j, int k, TextureAtlasSprite sprite, int l, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
            delegate.submitModel(model, object, poseStack, renderType, i, j, k, sprite, l, crumblingOverlay);
        }

        @Override
        public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int i, int j, TextureAtlasSprite sprite, boolean bl, boolean bl2, int k, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int l) {
            delegate.submitModelPart(modelPart, poseStack, renderType, i, j, sprite, bl, bl2, k, crumblingOverlay, l);
        }

        @Override
        public void submitBlock(PoseStack poseStack, BlockState blockState, int i, int j, int k) {
            delegate.submitBlock(poseStack, blockState, i, j, k);
        }

        @Override
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
            delegate.submitMovingBlock(poseStack, movingBlockRenderState);
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, BlockStateModel blockStateModel, float f, float g, float h, int i, int j, int k) {
            delegate.submitBlockModel(poseStack, renderType, blockStateModel, f, g, h, i, j, k);
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext itemDisplayContext, int i, int j, int k, int[] is, List<BakedQuad> list, RenderType renderType, ItemStackRenderState.FoilType foilType) {
            delegate.submitItem(poseStack, itemDisplayContext, i, j, k, is, list, renderType, foilType);
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
            delegate.submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
        }

        @Override
        public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer particleGroupRenderer) {
            delegate.submitParticleGroup(particleGroupRenderer);
        }
    }
}
