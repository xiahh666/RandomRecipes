package com.dsh.randomrecipes.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.dsh.randomrecipes.RecipeShuffler;

/**
 * Server to client notification carrying nothing but the interval index. The permutation is
 * derived from that index alone, so a single int is enough to reproduce it on the client.
 *
 * <p>This class must stay free of any client-only type: {@code registerMessage} instantiates
 * it reflectively, which verifies the whole class on the dedicated server as well.</p>
 */
public class MessageShuffleSync implements IMessage {

    private int epoch;

    public MessageShuffleSync() {
    }

    public MessageShuffleSync(int epoch) {
        this.epoch = epoch;
    }

    public int getEpoch() {
        return epoch;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        epoch = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(epoch);
    }

    public static class Handler implements IMessageHandler<MessageShuffleSync, IMessage> {
        @Override
        public IMessage onMessage(MessageShuffleSync message, MessageContext context) {
            // Never touch game state from the netty thread; the client tick handler drains this.
            RecipeShuffler.setPendingEpoch(message.getEpoch());
            return null;
        }
    }
}
