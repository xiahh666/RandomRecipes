package com.dsh.randomrecipes;

import java.io.File;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.config.Configuration;

/** Configuration backing store for Random Recipes. */
public final class ModConfig {

    private static Configuration config;

    /** Master switch for the automatic timer. */
    public static boolean enabled = true;
    /** How often (in minutes) the recipe table is re-scrambled. */
    public static int intervalMinutes = 5;
    /** Send a chat line to every player whenever the table changes. */
    public static boolean announce = true;
    /** Scramble once when the world is first ticked. */
    public static boolean shuffleAtStartup = true;
    /** Log every assignment change to the game log. */
    public static boolean logChanges = true;
    /** Registry names of output items that must never be used as a shuffled product. */
    public static String[] blacklist = new String[0];
    /** Let JEI display the scrambled products instead of the original ones. */
    public static boolean jeiShowShuffled = true;

    private ModConfig() {
    }

    public static void load(File file) {
        Configuration cfg = new Configuration(file);
        try {
            cfg.load();
            enabled = cfg.getBoolean("enabled", "general", true,
                    "Master switch for the automatic recipe shuffling timer.");
            intervalMinutes = cfg.getInt("intervalMinutes", "general", 5, 1, 1440,
                    "How often, in minutes, all crafting recipe outputs are re-scrambled.");
            announce = cfg.getBoolean("announce", "general", true,
                    "Announce every shuffle in the chat of all online players.");
            shuffleAtStartup = cfg.getBoolean("shuffleAtStartup", "general", true,
                    "Scramble the table once as soon as the world starts ticking.");
            logChanges = cfg.getBoolean("logChanges", "general", true,
                    "Write every reassigned recipe output to the game log (only the first 200 are logged).");
            blacklist = cfg.getStringList("blacklist", "general", new String[0],
                    "Registry names of items that may never appear as a shuffled product, e.g. minecraft:diamond.");
            jeiShowShuffled = cfg.getBoolean("jeiShowShuffled", "compat", true,
                    "Show the scrambled products in JEI's crafting category. Set to false to keep"
                            + " JEI as a reference of the unmodified recipes. Requires a game restart.");
        } catch (Exception e) {
            if (RandomRecipes.logger != null) {
                RandomRecipes.logger.error("Failed to read the configuration file", e);
            }
        } finally {
            if (cfg.hasChanged()) {
                cfg.save();
            }
            config = cfg;
        }
    }

    public static Configuration getConfig() {
        return config;
    }

    public static int intervalTicks() {
        return Math.max(1, intervalMinutes) * 20 * 60;
    }

    /** True when the given crafting result must be excluded from the shuffling pool. */
    public static boolean isBlacklisted(ItemStack stack) {
        if (stack == null || stack.getItem() == null || blacklist == null || blacklist.length == 0) {
            return false;
        }
        String name = RecipeShuffler.registryNameOf(stack);
        if (name == null) {
            return false;
        }
        for (String entry : blacklist) {
            if (entry != null && name.equalsIgnoreCase(entry.trim())) {
                return true;
            }
        }
        return false;
    }

    /** Kept separate so the command layer never has to touch the raw Configuration object. */
    public static boolean acceptsRecipe(IRecipe recipe) {
        return RecipeShuffler.safeOutput(recipe) != null && !isBlacklisted(RecipeShuffler.safeOutput(recipe));
    }
}
