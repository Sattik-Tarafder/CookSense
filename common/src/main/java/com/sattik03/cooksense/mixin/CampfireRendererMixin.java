package com.sattik03.cooksense.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sattik03.cooksense.render.CookSenseCampfireRenderState;
import com.sattik03.cooksense.render.CookingOverlayRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.CampfireRenderer;
import net.minecraft.client.renderer.blockentity.state.CampfireRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(CampfireRenderer.class)
public class CampfireRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/CampfireRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("RETURN")
    )
    private void cooksense$onExtractRenderState(
            CampfireBlockEntity blockEntity,
            CampfireRenderState state,
            float partialTick,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            CallbackInfo ci
    ) {
        CookSenseCampfireRenderState ext = (CookSenseCampfireRenderState) state;
        CampfireBlockEntityAccessor accessor = (CampfireBlockEntityAccessor) blockEntity;

        int[] progress = accessor.getCookingProgress();
        int[] times = accessor.getCookingTime();
        ext.cooksense$setCookingProgress(progress != null ? progress.clone() : null);
        ext.cooksense$setCookingTime(times != null ? times.clone() : null);

        BlockState blockState = blockEntity.getBlockState();
        ext.cooksense$setSoulCampfire(blockState.is(Blocks.SOUL_CAMPFIRE));
        ext.cooksense$setLit(blockState.hasProperty(CampfireBlock.LIT) && blockState.getValue(CampfireBlock.LIT));

        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();
        boolean hasBlockAbove = false;
        if (level != null) {
            BlockState stateAbove = level.getBlockState(pos.above());
            hasBlockAbove = !stateAbove.isAir() && !stateAbove.getCollisionShape(level, pos.above()).isEmpty();
        }
        ext.cooksense$setHasBlockAbove(hasBlockAbove);

        NonNullList<ItemStack> rawItems = blockEntity.getItems();
        List<ItemStack> copiedItems = new ArrayList<>(rawItems.size());
        for (ItemStack item : rawItems) {
            copiedItems.add(item.copy());
        }
        ext.cooksense$setItems(copiedItems);
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/blockentity/state/CampfireRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("RETURN")
    )
    private void cooksense$onSubmit(
            CampfireRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState,
            CallbackInfo ci
    ) {
        CookingOverlayRenderer.renderCampfireOverlay(state, poseStack, submitNodeCollector, cameraRenderState);
    }
}
