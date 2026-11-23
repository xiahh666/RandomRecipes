package com.dsh.randomrecipes.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** Network channel registration. */
public final class PacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("randomrecipes");

    private static boolean initialised;

    private PacketHandler() {
    }

    public static void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        INSTANCE.registerMessage(MessageShuffleSync.Handler.class, MessageShuffleSync.class, 0, Side.CLIENT);
    }
}
