package com.malinghan.marpc.transport.netty;

import com.alibaba.fastjson2.JSON;
import com.malinghan.marpc.context.RpcContext;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import com.malinghan.marpc.exception.MarpcBizException;
import com.malinghan.marpc.provider.ProviderBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<MarpcFrame> {

    private final ProviderBootstrap providerBootstrap;

    public NettyServerHandler(ProviderBootstrap providerBootstrap) {
        this.providerBootstrap = providerBootstrap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MarpcFrame frame) {
        log.info("[NettyServerHandler] 收到请求, sequenceId={}, payloadLen={}",
                frame.getSequenceId(), frame.getPayload().length);
        RpcRequest request = JSON.parseObject(frame.getPayload(), RpcRequest.class);

        if (request.getContext() != null && !request.getContext().isEmpty()) {
            RpcContext.setAll(request.getContext());
        }

        RpcResponse response;
        try {
            response = providerBootstrap.invoke(request);
        } catch (MarpcBizException e) {
            response = RpcResponse.error(e.getErrorCode() + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("[NettyServerHandler] 调用异常", e);
            response = RpcResponse.error("INTERNAL_ERROR: " + e.getMessage());
        } finally {
            RpcContext.clear();
        }

        byte[] payload = JSON.toJSONBytes(response);
        MarpcFrame responseFrame = new MarpcFrame(MarpcProtocol.TYPE_RESPONSE, frame.getSequenceId(), payload);
        log.info("[NettyServerHandler] 发送响应, sequenceId={}, status={}", frame.getSequenceId(), response.isStatus());
        ctx.writeAndFlush(responseFrame);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("[NettyServerHandler] 连接异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
