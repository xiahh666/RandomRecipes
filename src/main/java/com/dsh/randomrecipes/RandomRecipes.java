package com.dsh.randomrecipes;

import com.dsh.randomrecipes.command.CommandRandomRecipes;
import com.dsh.randomrecipes.network.PacketHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

/**
 * Random Recipes — periodically scrambles the output of every crafting recipe.
 *
 * <p>The set of recipe shapes and the set of products are both unchanged: only the
 * assignment between them is permuted. No recipe can therefore ever produce an item
 * that is not already craftable in the current game.</p>
 */
@Mod(modid = RandomRecipes.MODID, name = RandomRecipes.NAME, version = RandomRecipes.VERSION,
        acceptableRemoteVersions = "*", clientSideOnly = false, serverSideOnly = false)
public class RandomRecipes {

    public static final String MODID = "randomrecipes";
    public static final String NAME = "Random Recipes";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(RandomRecipes.MODID)
    public static RandomRecipes instance;

    @SidedProxy(clientSide = "com.dsh.randomrecipes.client.ClientProxy",
            serverSide = "com.dsh.randomrecipes.CommonProxy")
    public static CommonProxy proxy;

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        ModConfig.load(event.getSuggestedConfigurationFile());
        proxy.preInit(event);
        logger.info("Random Recipes starting up (interval = {} min, enabled = {})",
                Integer.valueOf(ModConfig.intervalMinutes), Boolean.valueOf(ModConfig.enabled));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        PacketHandler.init();
        proxy.init(event);
    }

    /**
     * Wraps the recipes as early as possible.
     *
     * <p>JEI snapshots {@code CraftingManager#getRecipeList()} when its own plugin is
     * registered (load complete, i.e. after post init) and dispatches every recipe through
     * a handler chosen by its runtime class. Wrapping here therefore guarantees that JEI
     * only ever sees {@link ShuffledRecipe} instances, which are routed to our own JEI
     * handler instead of being dropped.</p>
     */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        int wrapped = RecipeShuffler.ensureWrapped();
        logger.info("Wrapped {} crafting recipes for shuffling", Integer.valueOf(wrapped));
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandRandomRecipes());
    }
}
