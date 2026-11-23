package com.dsh.randomrecipes;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

/** A vanilla recipe whose product has been replaced by another craftable item. */
public class ShuffledRecipe implements IRecipe {

    private final IRecipe delegate;
    private ItemStack output;

    public ShuffledRecipe(IRecipe delegate) {
        this.delegate = delegate;
        ItemStack original = delegate.getRecipeOutput();
        this.output = original == null ? null : original.copy();
    }

    public IRecipe getDelegate() {
        return delegate;
    }

    public void setOutput(ItemStack stack) {
        this.output = stack == null ? null : stack.copy();
    }

    @Override
    public boolean matches(net.minecraft.inventory.InventoryCrafting inv, net.minecraft.world.World world) {
        return delegate.matches(inv, world);
    }

    @Override
    public ItemStack getCraftingResult(net.minecraft.inventory.InventoryCrafting inv) {
        return output == null ? null : output.copy();
    }

    @Override
    public int getRecipeSize() {
        return delegate.getRecipeSize();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return output == null ? null : output.copy();
    }

    @Override
    public ItemStack[] getRemainingItems(net.minecraft.inventory.InventoryCrafting inv) {
        return delegate.getRemainingItems(inv);
    }

    @Override
    public String toString() {
        return "ShuffledRecipe[" + delegate.getClass().getSimpleName() + " -> " + output + "]";
    }
}
