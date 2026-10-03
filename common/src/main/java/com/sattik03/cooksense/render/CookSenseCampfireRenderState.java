package com.sattik03.cooksense.render;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface CookSenseCampfireRenderState {
    int[] cooksense$getCookingProgress();
    void cooksense$setCookingProgress(int[] progress);

    int[] cooksense$getCookingTime();
    void cooksense$setCookingTime(int[] time);

    boolean cooksense$isSoulCampfire();
    void cooksense$setSoulCampfire(boolean soulCampfire);

    boolean cooksense$isLit();
    void cooksense$setLit(boolean lit);

    boolean cooksense$hasBlockAbove();
    void cooksense$setHasBlockAbove(boolean hasBlockAbove);

    float cooksense$getGameTime();
    void cooksense$setGameTime(float gameTime);

    List<ItemStack> cooksense$getItems();
    void cooksense$setItems(List<ItemStack> items);
}
