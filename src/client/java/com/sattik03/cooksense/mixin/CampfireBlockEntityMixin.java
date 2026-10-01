package com.sattik03.cooksense.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
    @Inject(method = "toInitialChunkDataNbt", at = @At("RETURN"))
    private void cooksense$writeCookingTimesToPacket(RegistryWrapper.WrapperLookup registries, CallbackInfoReturnable<NbtCompound> cir) {
        NbtCompound nbt = cir.getReturnValue();
        CampfireBlockEntity self = (CampfireBlockEntity) (Object) this;
        CampfireBlockEntityAccessor accessor = (CampfireBlockEntityAccessor) self;
        nbt.putIntArray("CookingTimes", accessor.getCookingTimes());
        nbt.putIntArray("CookingTotalTimes", accessor.getCookingTotalTimes());
    }

    /**
     * Advance client-side cooking ticks and resolve recipe cook time if not synced.
     */
    @Inject(method = "clientTick", at = @At("TAIL"))
    private static void cooksense$onClientTick(World world, BlockPos pos, BlockState state, CampfireBlockEntity campfire, CallbackInfo ci) {
        if (state.contains(CampfireBlock.LIT) && state.get(CampfireBlock.LIT)) {
            CampfireBlockEntityAccessor accessor = (CampfireBlockEntityAccessor) campfire;
            int[] cookingTimes = accessor.getCookingTimes();
            int[] cookingTotalTimes = accessor.getCookingTotalTimes();
            DefaultedList<ItemStack> items = campfire.getItemsBeingCooked();

            for (int i = 0; i < items.size(); ++i) {
                ItemStack stack = items.get(i);
                if (!stack.isEmpty()) {
                    // Fallback to recipe cooking time if total time is not yet known
                    if (cookingTotalTimes[i] <= 0) {
                        cookingTotalTimes[i] = campfire.getRecipeFor(stack)
                                .map(recipe -> recipe.value().getCookingTime())
                                .orElse(600); // 600 ticks = 30s default
                    }

                    if (cookingTimes[i] < cookingTotalTimes[i]) {
                        cookingTimes[i]++;
                    }
                } else {
                    cookingTimes[i] = 0;
                    cookingTotalTimes[i] = 0;
                }
            }
        }
    }
}
