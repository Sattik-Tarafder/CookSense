package com.sattik03.cooksense.mixin;

import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CampfireBlockEntity.class)
public interface CampfireBlockEntityAccessor {
    @Accessor("cookingProgress")
    int[] getCookingProgress();

    @Accessor("cookingTime")
    int[] getCookingTime();
}
