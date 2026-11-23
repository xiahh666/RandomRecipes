package com.dsh.randomrecipes.compat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.dsh.randomrecipes.ModConfig;
import com.dsh.randomrecipes.ShuffledRecipe;

import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.wrapper.ICraftingRecipeWrapper;
import mezz.jei.api.recipe.wrapper.IShapedCraftingRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * JEI view of a {@link ShuffledRecipe}.
 *
 * <p>The inputs are read from the original recipe, which is exactly what the player has to
 * put on the grid; only the result shown on the right hand side changes. That is what the
 * whole mod is about: the shape stays, the product moves.</p>
 *
 * <p>When {@link ModConfig#jeiShowShuffled} is false the original product is displayed
 * instead, so players who dislike the scrambled listing can keep JEI as a reference of the
 * unmodified game.</p>
 */
public final class ShuffledRecipeWrapper {

    private ShuffledRecipeWrapper() {
    }

    public static IRecipeWrapper create(ShuffledRecipe recipe) {
        Object delegate = recipe.getDelegate();
        if (delegate instanceof ShapedRecipes) {
            return new Shaped(recipe, (ShapedRecipes) delegate);
        }
        if (delegate instanceof ShapelessRecipes) {
            return new Shapeless(recipe, (ShapelessRecipes) delegate);
        }
        if (delegate instanceof ShapedOreRecipe) {
            return new ShapedOre(recipe, (ShapedOreRecipe) delegate);
        }
        if (delegate instanceof ShapelessOreRecipe) {
            return new ShapelessOre(recipe, (ShapelessOreRecipe) delegate);
        }
        return new Generic(recipe);
    }

    // ------------------------------------------------------------------ helpers

    static ItemStack copyOne(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        return copy;
    }

    static List<ItemStack> copyAll(ItemStack[] stacks) {
        List<ItemStack> out = new ArrayList<ItemStack>(stacks.length);
        for (ItemStack stack : stacks) {
            out.add(copyOne(stack));
        }
        return out;
    }

    static List<ItemStack> copyList(List<ItemStack> stacks) {
        List<ItemStack> out = new ArrayList<ItemStack>(stacks.size());
        for (ItemStack stack : stacks) {
            out.add(copyOne(stack));
        }
        return out;
    }

    static List<Object> copyObjects(Object[] objects) {
        List<Object> out = new ArrayList<Object>(objects.length);
        for (Object object : objects) {
            if (object instanceof ItemStack) {
                out.add(copyOne((ItemStack) object));
            } else {
                out.add(object);
            }
        }
        return out;
    }

    static int privateInt(Object owner, Class<?> ownerClass, String... names) {
        for (String name : names) {
            try {
                Integer value = ObfuscationReflectionHelper.getPrivateValue(
                        (Class<Object>) ownerClass, owner, name);
                if (value != null) {
                    return value.intValue();
                }
            } catch (RuntimeException ignored) {
                // try the next candidate name
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ variants

    private abstract static class Base implements IRecipeWrapper {

        protected final ShuffledRecipe shuffled;

        Base(ShuffledRecipe shuffled) {
            this.shuffled = shuffled;
        }

        public List<ItemStack> getOutputs() {
            ItemStack shown = ModConfig.jeiShowShuffled
                    ? shuffled.getRecipeOutput()
                    : copyOne(shuffled.getDelegate().getRecipeOutput());
            return Collections.singletonList(shown);
        }

        @Override
        public List getFluidInputs() {
            return Collections.emptyList();
        }

        @Override
        public List getFluidOutputs() {
            return Collections.emptyList();
        }

        @Override
        public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight) {
        }

        @Override
        public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        }

        @Override
        public void drawAnimations(Minecraft minecraft, int recipeWidth, int recipeHeight) {
        }

        @Override
        public List<String> getTooltipStrings(int mouseX, int mouseY) {
            return null;
        }

        @Override
        public boolean handleClick(Minecraft minecraft, int mouseX, int mouseY, int mouseButton) {
            return false;
        }
    }

    private static final class Shaped extends Base implements IShapedCraftingRecipeWrapper {

        private final ShapedRecipes recipe;

        Shaped(ShuffledRecipe shuffled, ShapedRecipes recipe) {
            super(shuffled);
            this.recipe = recipe;
        }

        @Override
        public List getInputs() {
            return copyAll(this.recipe.recipeItems);
        }

        @Override
        public int getWidth() {
            return this.recipe.recipeWidth;
        }

        @Override
        public int getHeight() {
            return this.recipe.recipeHeight;
        }
    }

    private static final class Shapeless extends Base implements ICraftingRecipeWrapper {

        private final ShapelessRecipes recipe;

        Shapeless(ShuffledRecipe shuffled, ShapelessRecipes recipe) {
            super(shuffled);
            this.recipe = recipe;
        }

        @Override
        public List getInputs() {
            return copyList(this.recipe.recipeItems);
        }
    }

    private static final class ShapedOre extends Base implements IShapedCraftingRecipeWrapper {

        private final ShapedOreRecipe recipe;
        private final int width;
        private final int height;

        ShapedOre(ShuffledRecipe shuffled, ShapedOreRecipe recipe) {
            super(shuffled);
            this.recipe = recipe;
            this.width = privateInt(recipe, ShapedOreRecipe.class, "width");
            this.height = privateInt(recipe, ShapedOreRecipe.class, "height");
        }

        @Override
        public List getInputs() {
            return copyObjects(this.recipe.getInput());
        }

        @Override
        public int getWidth() {
            return this.width > 0 ? this.width : 3;
        }

        @Override
        public int getHeight() {
            return this.height > 0 ? this.height : 3;
        }
    }

    private static final class ShapelessOre extends Base implements ICraftingRecipeWrapper {

        private final ShapelessOreRecipe recipe;

        ShapelessOre(ShuffledRecipe shuffled, ShapelessOreRecipe recipe) {
            super(shuffled);
            this.recipe = recipe;
        }

        @Override
        public List getInputs() {
            List<Object> inputs = this.recipe.getInput();
            List<Object> out = new ArrayList<Object>(inputs.size());
            for (Object input : inputs) {
                out.add(input instanceof ItemStack ? copyOne((ItemStack) input) : input);
            }
            return out;
        }
    }

    /** Fallback for recipes added by other mods: dig one item container out of the object. */
    private static final class Generic extends Base implements ICraftingRecipeWrapper {

        private final List<Object> inputs;

        Generic(ShuffledRecipe shuffled) {
            super(shuffled);
            this.inputs = scanInputs(shuffled.getDelegate());
        }

        @Override
        public List getInputs() {
            return this.inputs;
        }

        private static List<Object> scanInputs(Object delegate) {
            for (Class<?> type = delegate.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(delegate);
                        if (value instanceof ItemStack[]) {
                            return new ArrayList<Object>(copyAll((ItemStack[]) value));
                        }
                        if (value instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> list = (List<Object>) value;
                            List<Object> out = new ArrayList<Object>(list.size());
                            for (Object entry : list) {
                                out.add(entry instanceof ItemStack ? copyOne((ItemStack) entry) : entry);
                            }
                            return out;
                        }
                        if (value instanceof Object[]) {
                            return copyObjects((Object[]) value);
                        }
                    } catch (Throwable ignored) {
                        // keep scanning: this field is not usable
                    }
                }
            }
            return Arrays.asList(new Object[0]);
        }
    }
}
