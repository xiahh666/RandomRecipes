package com.dsh.randomrecipes.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.dsh.randomrecipes.CommonProxy;
import com.dsh.randomrecipes.compat.JeiSync;

/** Client side proxy: adds the client tick hook that lands incoming shuffle packets. */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        FMLCommonHandler.instance().bus().register(new ClientTickHandler());
    }

    /**
     * Rebuilds JEI's output index. The integrated server applies the shuffle on the server
     * thread (single player shares one JVM), so that case is bounced onto the client thread.
     */
    @Override
    public void refreshJei() {
        final Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }
        if (minecraft.isCallingFromMinecraftThread()) {
            JeiSync.refresh();
        } else {
            minecraft.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    JeiSync.refresh();
                }
            });
        }
    }
}
