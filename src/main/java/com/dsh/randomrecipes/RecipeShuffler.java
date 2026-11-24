package com.dsh.randomrecipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

/**
 * Owns the live crafting table: replaces eligible recipes with {@link ShuffledRecipe}
 * wrappers once, then permutes their products on every timer tick.
 *
 * <p>Wrappers are created once and then reused, so object identity inside the recipe
 * list never changes while the game is running. Only the assigned product of each
 * wrapper is swapped, which keeps mod caches (JEI/NEI) and recipe sorting stable.</p>
 */
public final class RecipeShuffler {

    private static final List<ShuffledRecipe> WRAPPERS = new ArrayList<ShuffledRecipe>();
    private static final List<ItemStack> ORIGINALS = new ArrayList<ItemStack>();
    private static final AtomicInteger PENDING_EPOCH = new AtomicInteger(-1);

    /** Highest interval index that has been applied, or -1 when nothing was applied yet. */
    private static int currentEpoch = -1;
    private static int shuffleCount = 0;
    private static int loggedChanges = 0;
    private static boolean suspended = false;

    private RecipeShuffler() {
    }

    // ------------------------------------------------------------------ queries

    public static synchronized int getCurrentEpoch() {
        return currentEpoch;
    }

    public static synchronized int getShuffleCount() {
        return shuffleCount;
    }

    public static synchronized boolean isSuspended() {
        return suspended;
    }

    public static synchronized int getWrappedCount() {
        return WRAPPERS.size();
    }

    public static synchronized void setEpochWithoutShuffle(int epoch) {
        currentEpoch = epoch;
    }

    public static void setPendingEpoch(int epoch) {
        PENDING_EPOCH.set(epoch);
    }

    /** Returns the epoch waiting to be applied on the client thread, or -1. */
    public static int takePendingEpoch() {
        return PENDING_EPOCH.getAndSet(-1);
    }

    // ------------------------------------------------------------------ mutation

    /**
     * Wraps every eligible recipe that is not wrapped yet. Safe to call repeatedly; new
     * recipes registered later by other mods are picked up on the next call.
     *
     * @return the number of recipes wrapped by this call
     */
    public static synchronized int ensureWrapped() {
        List<IRecipe> recipes = CraftingManager.getInstance().getRecipeList();
        int added = 0;
        for (int i = 0; i < recipes.size(); i++) {
            IRecipe recipe = recipes.get(i);
            if (recipe == null || recipe instanceof ShuffledRecipe) {
                continue;
            }
            if (!eligible(recipe)) {
                continue;
            }
            ItemStack output = safeOutput(recipe);
            if (output == null) {
                continue;
            }
            ShuffledRecipe wrapper = new ShuffledRecipe(recipe);
            recipes.set(i, wrapper);
            WRAPPERS.add(wrapper);
            ORIGINALS.add(output.copy());
            added++;
        }
        if (added > 0 && RandomRecipes.logger != null) {
            RandomRecipes.logger.info("Wrapped {} crafting recipes (pool size now {})",
                    Integer.valueOf(added), Integer.valueOf(WRAPPERS.size()));
        }
        return added;
    }

    /**
     * Applies the deterministic permutation belonging to {@code epoch}.
     *
     * @return true when the table actually changed
     */
    public static synchronized boolean apply(int epoch, boolean force) {
        if (!force && epoch == currentEpoch) {
            return false;
        }
        ensureWrapped();
        int size = WRAPPERS.size();
        if (size < 2) {
            currentEpoch = epoch;
            return false;
        }
        assign(permutation(size, seedFor(epoch)));
        currentEpoch = epoch;
        shuffleCount++;
        suspended = false;
        if (RandomRecipes.logger != null) {
            RandomRecipes.logger.info("Shuffled {} crafting results for interval #{}", Integer.valueOf(size),
                    Integer.valueOf(epoch));
        }
        refreshJei();
        return true;
    }

    /** Shuffles with a non-deterministic seed, used by {@code /randrec now}. */
    public static synchronized boolean shuffleRandom() {
        ensureWrapped();
        int size = WRAPPERS.size();
        if (size < 2) {
            return false;
        }
        assign(permutation(size, System.nanoTime()));
        shuffleCount++;
        suspended = false;
        if (RandomRecipes.logger != null) {
            RandomRecipes.logger.info("Manually shuffled {} crafting results", Integer.valueOf(size));
        }
        refreshJei();
        return true;
    }

    /** Puts every product back where it came from and pauses the automatic timer. */
    public static synchronized void restore() {
        for (int i = 0; i < WRAPPERS.size(); i++) {
            WRAPPERS.get(i).setOutput(ORIGINALS.get(i));
        }
        suspended = true;
        if (RandomRecipes.logger != null) {
            RandomRecipes.logger.info("Restored all {} recipes to their original products", Integer.valueOf(WRAPPERS.size()));
        }
        refreshJei();
    }

    public static synchronized void resume() {
        suspended = false;
    }

    /** Human readable dump of the current assignment, limited to {@code limit} lines. */
    public static synchronized List<String> describe(int limit) {
        List<String> lines = new ArrayList<String>();
        int max = Math.min(limit <= 0 ? 20 : limit, WRAPPERS.size());
        for (int i = 0; i < max; i++) {
            ShuffledRecipe wrapper = WRAPPERS.get(i);
            String original = nameOf(wrapper.getDelegate().getRecipeOutput());
            String current = nameOf(wrapper.getRecipeOutput());
            String line = "#" + i + " " + wrapper.getDelegate().getClass().getSimpleName() + ": " + original;
            if (!original.equals(current)) {
                line = line + " => " + current;
            }
            lines.add(line);
        }
        return lines;
    }

    // ------------------------------------------------------------------ internals

    /** Lets the client side re-index JEI, which caches the recipe outputs it saw at load time. */
    private static void refreshJei() {
        CommonProxy proxy = RandomRecipes.proxy;
        if (proxy != null) {
            proxy.refreshJei();
        }
    }

    private static void assign(int[] permutation) {
        for (int i = 0; i < WRAPPERS.size(); i++) {
            ItemStack product = ORIGINALS.get(permutation[i]);
            WRAPPERS.get(i).setOutput(product);
            if (ModConfig.logChanges && loggedChanges < 200 && RandomRecipes.logger != null) {
                RandomRecipes.logger.info("  #{} {} => {}", Integer.valueOf(i),
                        WRAPPERS.get(i).getDelegate().getClass().getSimpleName(), nameOf(product));
                loggedChanges++;
            }
        }
    }

    private static int[] permutation(int size, long seed) {
        int[] order = new int[size];
        for (int i = 0; i < size; i++) {
            order[i] = i;
        }
        Random random = new Random(seed);
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = order[i];
            order[i] = order[j];
            order[j] = tmp;
        }
        // A plain shuffle leaves ~37% of the recipes producing their own product again.
        // Swap those fixed points away so the change is visible immediately.
        for (int i = 0; i < size; i++) {
            if (order[i] == i) {
                int j = random.nextInt(size - 1);
                if (j >= i) {
                    j++;
                }
                int tmp = order[i];
                order[i] = order[j];
                order[j] = tmp;
            }
        }
        return order;
    }

    private static long seedFor(int epoch) {
        long mixed = 0x9E3779B97F4A7C15L * ((long) epoch + 1L);
        return mixed ^ 0xD1B54A32D192ED03L;
    }

    /**
     * Whether a recipe may take part in the shuffle. Recipes whose product depends on the
     * actual crafting grid contents (dyeing, repairs, map cloning, fireworks, book copying)
     * are left untouched so those mechanics keep working.
     */
    public static boolean eligible(IRecipe recipe) {
        String name = recipe.getClass().getName();
        if (name.startsWith("net.minecraft.item.crafting.Recipe")) {
            return false;
        }
        if (name.equals("net.minecraft.item.crafting.RecipesArmorDyes")
                || name.equals("net.minecraft.item.crafting.RecipesMapCloning")
                || name.equals("net.minecraft.item.crafting.RecipesMapExtending")) {
            return false;
        }
        ItemStack output = safeOutput(recipe);
        return output != null && !ModConfig.isBlacklisted(output);
    }

    public static ItemStack safeOutput(IRecipe recipe) {
        try {
            ItemStack stack = recipe.getRecipeOutput();
            if (stack != null && stack.getItem() != null) {
                return stack;
            }
        } catch (Throwable throwable) {
            if (RandomRecipes.logger != null) {
                RandomRecipes.logger.warn("Recipe {} threw while reading its output",
                        recipe.getClass().getName(), throwable);
            }
        }
        return null;
    }

    public static String nameOf(ItemStack stack) {
        if (stack == null) {
            return "<null>";
        }
        String base = registryNameOf(stack);
        if (base == null) {
            base = String.valueOf(stack.getItem());
        }
        return stack.stackSize > 1 ? base + " x" + stack.stackSize : base;
    }

    public static String registryNameOf(ItemStack stack) {
        try {
            Item item = stack.getItem();
            if (item == null) {
                return null;
            }
            ResourceLocation location = Item.itemRegistry.getNameForObject(item);
            return location == null ? null : location.toString();
        } catch (Throwable throwable) {
            return null;
        }
    }
}
