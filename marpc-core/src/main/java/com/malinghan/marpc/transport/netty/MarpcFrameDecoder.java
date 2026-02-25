package com.malinghan.marpc.transport.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class MarpcFrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 等待至少 header 长度
        if (in.readableBytes() < MarpcProtocol.HEADER_LENGTH) return;

        in.markReaderIndex();

        // 校验魔数
        byte m1 = in.readByte();
        byte m2 = in.readByte();
        if (m1 != MarpcProtocol.MAGIC_1 || m2 != MarpcProtocol.MAGIC_2) {
            ctx.close();
            return;
        }

        byte version = in.readByte(); // 版本，暂不处理
        byte type = in.readByte();
        int sequenceId = in.readInt();
        int length = in.readInt();

        // 等待 body 到齐
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        byte[] payload = new byte[length];
        in.readBytes(payload);
        out.add(new MarpcFrame(type, sequenceId, payload));
    }
}
