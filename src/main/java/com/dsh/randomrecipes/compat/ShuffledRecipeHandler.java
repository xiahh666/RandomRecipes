package com.dsh.randomrecipes.compat;

import javax.annotation.Nonnull;

import com.dsh.randomrecipes.ShuffledRecipe;

import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;

/**
 * Teaches JEI how to display {@link ShuffledRecipe}.
 *
 * <p>JEI picks a handler by the runtime class of every recipe object (see
 * {@code IRecipeHandler#getRecipeClass()}). Once this mod has swapped the vanilla recipes
 * for wrappers, no built in JEI handler matches them any more and JEI silently drops the
 * whole crafting category. Registering this handler puts them back on the screen.</p>
 */
public class ShuffledRecipeHandler implements IRecipeHandler<ShuffledRecipe> {

    @Nonnull
    @Override
    public Class<ShuffledRecipe> getRecipeClass() {
        return ShuffledRecipe.class;
    }

    @Nonnull
    @Override
    public String getRecipeCategoryUid() {
        return VanillaRecipeCategoryUid.CRAFTING;
    }

    @Nonnull
    @Override
    public IRecipeWrapper getRecipeWrapper(@Nonnull ShuffledRecipe recipe) {
        return ShuffledRecipeWrapper.create(recipe);
    }

    @Override
    public boolean isRecipeValid(@Nonnull ShuffledRecipe recipe) {
        return recipe.getDelegate() != null && recipe.getRecipeOutput() != null;
    }
}
