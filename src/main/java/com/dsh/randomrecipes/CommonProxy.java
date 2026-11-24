package com.dsh.randomrecipes;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import com.dsh.randomrecipes.network.MessageShuffleSync;
import com.dsh.randomrecipes.network.PacketHandler;

/**
 * Server side proxy. Deliberately free of any {@code net.minecraft.client} reference so the
 * dedicated server never trips over a missing client class at class verification time.
 */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(new ServerTickHandler());
        FMLCommonHandler.instance().bus().register(this);
    }

    /**
     * Hook for the client side to re-index JEI after the table changed. The dedicated server
     * has no client recipe index, so this is deliberately empty here.
     */
    public void refreshJei() {
    }

    /** Late joiners need the current interval index, otherwise their client copy stays original. */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        int epoch = RecipeShuffler.getCurrentEpoch();
        if (epoch < 0) {
            return;
        }
        PacketHandler.INSTANCE.sendTo(new MessageShuffleSync(epoch), (EntityPlayerMP) event.player);
    }
}
