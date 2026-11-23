package com.dsh.randomrecipes;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import com.dsh.randomrecipes.network.MessageShuffleSync;
import com.dsh.randomrecipes.network.PacketHandler;

/**
 * Drives the timer on the logical server (integrated server included) and pushes the new
 * interval index to every client so that client side recipe prediction and JEI stay in sync.
 */
public class ServerTickHandler {

    private boolean primed;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!ModConfig.enabled || RecipeShuffler.isSuspended()) {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return;
        }
        World world = server.getEntityWorld();
        if (world == null) {
            return;
        }
        int epoch = (int) (world.getTotalWorldTime() / ModConfig.intervalTicks());
        if (!primed) {
            primed = true;
            if (!ModConfig.shuffleAtStartup) {
                RecipeShuffler.setEpochWithoutShuffle(epoch);
                return;
            }
        }
        if (RecipeShuffler.apply(epoch, false)) {
            sync(server, epoch);
        }
    }

    public static void sync(MinecraftServer server, int epoch) {
        if (ModConfig.announce && server != null) {
            int minutes = Math.max(1, ModConfig.intervalMinutes);
            for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
                player.addChatMessage(new ChatComponentTranslation("randomrecipes.shuffle.announce",
                        new Object[] { Integer.valueOf(RecipeShuffler.getShuffleCount()), Integer.valueOf(minutes) }));
            }
        }
        PacketHandler.INSTANCE.sendToAll(new MessageShuffleSync(epoch));
    }
}
