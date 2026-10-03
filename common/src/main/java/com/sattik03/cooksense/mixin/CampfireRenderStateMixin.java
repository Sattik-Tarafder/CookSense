package com.sattik03.cooksense.mixin;

import com.sattik03.cooksense.render.CookSenseCampfireRenderState;
import net.minecraft.client.renderer.blockentity.state.CampfireRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(CampfireRenderState.class)
public class CampfireRenderStateMixin implements CookSenseCampfireRenderState {
    @Unique private int[] cooksense$cookingProgress;
    @Unique private int[] cooksense$cookingTime;
    @Unique private boolean cooksense$isSoulCampfire;
    @Unique private boolean cooksense$isLit;
    @Unique private boolean cooksense$hasBlockAbove;
    @Unique private float cooksense$gameTime;
    @Unique private List<ItemStack> cooksense$items;

    @Override
    public int[] cooksense$getCookingProgress() {
        return cooksense$cookingProgress;
    }

    @Override
    public void cooksense$setCookingProgress(int[] progress) {
        this.cooksense$cookingProgress = progress;
    }

    @Override
    public int[] cooksense$getCookingTime() {
        return cooksense$cookingTime;
    }

    @Override
    public void cooksense$setCookingTime(int[] time) {
        this.cooksense$cookingTime = time;
    }

    @Override
    public boolean cooksense$isSoulCampfire() {
        return cooksense$isSoulCampfire;
    }

    @Override
    public void cooksense$setSoulCampfire(boolean soulCampfire) {
        this.cooksense$isSoulCampfire = soulCampfire;
    }

    @Override
    public boolean cooksense$isLit() {
        return cooksense$isLit;
    }

    @Override
    public void cooksense$setLit(boolean lit) {
        this.cooksense$isLit = lit;
    }

    @Override
    public boolean cooksense$hasBlockAbove() {
        return cooksense$hasBlockAbove;
    }

    @Override
    public void cooksense$setHasBlockAbove(boolean hasBlockAbove) {
        this.cooksense$hasBlockAbove = hasBlockAbove;
    }

    @Override
    public float cooksense$getGameTime() {
        return cooksense$gameTime;
    }

    @Override
    public void cooksense$setGameTime(float gameTime) {
        this.cooksense$gameTime = gameTime;
    }

    @Override
    public List<ItemStack> cooksense$getItems() {
        return cooksense$items;
    }

    @Override
    public void cooksense$setItems(List<ItemStack> items) {
        this.cooksense$items = items;
    }
}
