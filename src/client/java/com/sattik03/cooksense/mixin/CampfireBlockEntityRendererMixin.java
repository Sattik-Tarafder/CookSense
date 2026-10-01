package com.sattik03.cooksense.mixin;

import com.sattik03.cooksense.render.CookingOverlayRenderer;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.CampfireBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlockEntityRenderer.class)
public class CampfireBlockEntityRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/block/entity/CampfireBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
            at = @At("TAIL")
    )
    private void cooksense$renderOverlay(
            CampfireBlockEntity campfire,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            CallbackInfo ci
    ) {
        CookingOverlayRenderer.renderCampfireOverlay(campfire, tickDelta, matrices, vertexConsumers, light, overlay);
    }
}
