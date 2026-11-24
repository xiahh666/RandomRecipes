package com.dsh.randomrecipes.compat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Table;

import com.dsh.randomrecipes.RandomRecipes;

import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IRecipeRegistry;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.util.RecipeMap;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * Keeps JEI's "R" lookup (recipes that produce a given item) in sync with the shuffled table.
 *
 * <p>JEI builds two lookup maps once, when its RecipeRegistry is constructed at load complete:
 * one keyed by recipe inputs, one keyed by recipe outputs
 * (RecipeRegistry#addRecipeUnchecked -> RecipeMap#addRecipe). The output map is a plain
 * stackKey -> recipes index captured at that moment, so after we permute the products it still
 * answers with the products that were live at load time. The input map never goes stale because
 * shapes do not change, which is exactly why "U" (uses) kept working while "R" (recipe for) did
 * not.</p>
 *
 * <p>JEI has no public API to remove or re-index a recipe, so the output map is cleared and
 * rebuilt from the current wrappers. Any structural change in JEI is caught and only disables
 * the refresh; JEI keeps working, it just stays stale.</p>
 */
public final class JeiSync {

    private static IJeiRuntime runtime;
    private static boolean warned;
    private static Field outputMapField;
    private static Field tableField;
    private static Field categoryMapField;

    private JeiSync() {
    }

    public static void setRuntime(IJeiRuntime newRuntime) {
        runtime = newRuntime;
    }

    public static boolean isAvailable() {
        return runtime != null;
    }

    /** Rebuilds JEI's output index from the current products. Must run on the client thread. */
    public static synchronized void refresh() {
        if (runtime == null) {
            return;
        }
        try {
            IRecipeRegistry registry = runtime.getRecipeRegistry();
            RecipeMap outputMap = outputMap(registry);
            if (outputMap == null) {
                return;
            }
            Table<?, ?, ?> table = (Table<?, ?, ?>) tableField.get(outputMap);
            ArrayListMultimap<?, ?> categories = (ArrayListMultimap<?, ?>) categoryMapField.get(outputMap);
            table.clear();
            categories.clear();
            int indexed = 0;
            int skipped = 0;
            List<IRecipeCategory> recipeCategories = registry.getRecipeCategories();
            for (IRecipeCategory category : recipeCategories) {
                List<Object> recipes = new ArrayList<Object>(registry.getRecipes(category));
                for (Object recipe : recipes) {
                    int result = index(registry, outputMap, category, recipe);
                    if (result > 0) {
                        indexed += result;
                    } else {
                        skipped++;
                    }
                }
            }
            if (RandomRecipes.logger != null) {
                RandomRecipes.logger.info("Refreshed JEI output index ({} recipes, {} categories, {} skipped)",
                        Integer.valueOf(indexed), Integer.valueOf(recipeCategories.size()),
                        Integer.valueOf(skipped));
            }
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                if (RandomRecipes.logger != null) {
                    RandomRecipes.logger.warn(
                            "Could not refresh the JEI output index; JEI will keep showing the products it cached at load time",
                            t);
                }
            }
        }
    }

    private static RecipeMap outputMap(IRecipeRegistry registry) throws Exception {
        if (outputMapField == null) {
            Field field = registry.getClass().getDeclaredField("recipeOutputMap");
            field.setAccessible(true);
            Class<?> mapClass = field.getType();
            Field table = mapClass.getDeclaredField("recipeTable");
            table.setAccessible(true);
            Field categories = mapClass.getDeclaredField("categoryMap");
            categories.setAccessible(true);
            tableField = table;
            categoryMapField = categories;
            outputMapField = field;
        }
        return (RecipeMap) outputMapField.get(registry);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static int index(IRecipeRegistry registry, RecipeMap outputMap, IRecipeCategory category, Object recipe) {
        IRecipeHandler handler = registry.getRecipeHandler(recipe.getClass());
        if (handler == null) {
            return 0;
        }
        IRecipeWrapper wrapper = handler.getRecipeWrapper(recipe);
        if (wrapper == null) {
            return 0;
        }
        List<ItemStack> outputs = itemStacks(wrapper.getOutputs());
        List<FluidStack> fluids = fluidStacks(wrapper.getFluidOutputs());
        if (outputs.isEmpty() && fluids.isEmpty()) {
            return 0;
        }
        try {
            outputMap.addRecipe(recipe, category, outputs, fluids);
        } catch (Throwable t) {
            // JEI's StackHelper throws for stacks whose item is missing from GameData; JEI skips
            // those at load time as well, so mirror that and keep the rest of the index alive.
            return 0;
        }
        return 1;
    }

    private static List<ItemStack> itemStacks(List<?> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemStack> stacks = new ArrayList<ItemStack>(outputs.size());
        for (Object output : outputs) {
            if (output instanceof ItemStack) {
                stacks.add((ItemStack) output);
            }
        }
        return stacks;
    }

    private static List<FluidStack> fluidStacks(List<FluidStack> fluids) {
        if (fluids == null || fluids.isEmpty()) {
            return Collections.emptyList();
        }
        List<FluidStack> stacks = new ArrayList<FluidStack>(fluids.size());
        for (FluidStack fluid : fluids) {
            if (fluid != null) {
                stacks.add(fluid);
            }
        }
        return stacks;
    }
}
