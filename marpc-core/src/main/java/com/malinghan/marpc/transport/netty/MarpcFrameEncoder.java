package com.malinghan.marpc.transport.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MarpcFrameEncoder extends MessageToByteEncoder<MarpcFrame> {

    @Override
    protected void encode(ChannelHandlerContext ctx, MarpcFrame frame, ByteBuf out) {
        out.writeByte(MarpcProtocol.MAGIC_1);
        out.writeByte(MarpcProtocol.MAGIC_2);
        out.writeByte(MarpcProtocol.VERSION);
        out.writeByte(frame.getType());
        out.writeInt(frame.getSequenceId());
        out.writeInt(frame.getPayload().length);
        out.writeBytes(frame.getPayload());
    }
}
