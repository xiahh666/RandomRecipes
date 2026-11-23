package com.dsh.randomrecipes.compat;

import javax.annotation.Nonnull;

import com.dsh.randomrecipes.ModConfig;
import com.dsh.randomrecipes.RandomRecipes;

import mezz.jei.api.BlankModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;

/**
 * JEI integration.
 *
 * <p>Only {@code mezz.jei.api} is touched here, so no JEI internals are referenced. The
 * plugin class is discovered by JEI itself through the {@link JEIPlugin} annotation, which
 * means it is never loaded when JEI is absent.</p>
 */
@JEIPlugin
public class JeiPlugin extends BlankModPlugin {

    @Override
    public void register(@Nonnull IModRegistry registry) {
        registry.addRecipeHandlers(new ShuffledRecipeHandler());
        if (RandomRecipes.logger != null) {
            RandomRecipes.logger.info(
                    "Registered JEI handler for shuffled recipes (showing {} products)",
                    ModConfig.jeiShowShuffled ? "scrambled" : "original");
        }
    }
}
