package com.dsh.randomrecipes.client;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import com.dsh.randomrecipes.RecipeShuffler;

/**
 * Applies a pending shuffle on the client thread. Packet handlers run on the netty IO
 * thread, where touching the crafting table is not safe.
 */
public class ClientTickHandler {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        int pending = RecipeShuffler.takePendingEpoch();
        if (pending >= 0) {
            RecipeShuffler.apply(pending, false);
        }
    }
}
