package com.sattik03.cooksense.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CampfireBlockEntity.class)
public class CampfireBlockEntityMixin {

    /**
     * Vanilla does not write CookingTimes or CookingTotalTimes to initial chunk data / update packets.
     * We inject here so both Singleplayer and Modded Servers properly send cook progress in the packet.
     */
    @Inject(method = "getUpdateTag", at = @At("RETURN"))
    private void cooksense$writeCookingTimesToPacket(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        CampfireBlockEntityAccessor accessor = (CampfireBlockEntityAccessor) (Object) this;
        tag.putIntArray("CookingTimes", accessor.getCookingProgress());
        tag.putIntArray("CookingTotalTimes", accessor.getCookingTime());
    }

    /**
     * Advance client-side cooking ticks and resolve recipe cook time if not synced.
     */
    @Inject(method = "particleTick", at = @At("TAIL"))
    private static void cooksense$onClientTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire, CallbackInfo ci) {
        if (state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT)) {
            CampfireBlockEntityAccessor accessor = (CampfireBlockEntityAccessor) campfire;
            int[] cookingProgress = accessor.getCookingProgress();
            int[] cookingTime = accessor.getCookingTime();
            NonNullList<ItemStack> items = campfire.getItems();

            for (int i = 0; i < items.size(); ++i) {
                ItemStack stack = items.get(i);
                if (!stack.isEmpty()) {
                    // Fallback to recipe cooking time if total time is not yet known
                    if (cookingTime[i] <= 0) {
                        cookingTime[i] = campfire.getCookableRecipe(stack)
                                .map(recipe -> recipe.value().getCookingTime())
                                .orElse(600); // 600 ticks = 30s default
                    }

                    if (cookingProgress[i] < cookingTime[i]) {
                        cookingProgress[i]++;
                    }
                } else {
                    cookingProgress[i] = 0;
                    cookingTime[i] = 0;
                }
            }
        }
    }
}
