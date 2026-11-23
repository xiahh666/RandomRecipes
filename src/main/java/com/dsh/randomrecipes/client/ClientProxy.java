package com.dsh.randomrecipes.client;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.dsh.randomrecipes.CommonProxy;

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
}
