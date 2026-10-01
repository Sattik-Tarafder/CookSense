package com.sattik03.cooksense.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sattik03.cooksense.render.CookingOverlayRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.CampfireRenderer;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireRenderer.class)
public class CampfireRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("TAIL")
    )
    private void cooksense$renderOverlay(
            CampfireBlockEntity campfire,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            CallbackInfo ci
    ) {
        CookingOverlayRenderer.renderCampfireOverlay(campfire, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
