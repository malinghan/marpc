package com.malinghan.marpc.transport.netty;

import com.alibaba.fastjson2.JSON;
import com.malinghan.marpc.core.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class NettyClientHandler extends SimpleChannelInboundHandler<MarpcFrame> {

    private final ConcurrentHashMap<Integer, CompletableFuture<RpcResponse>> pendingRequests;

    public NettyClientHandler(ConcurrentHashMap<Integer, CompletableFuture<RpcResponse>> pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MarpcFrame frame) {
        CompletableFuture<RpcResponse> future = pendingRequests.remove(frame.getSequenceId());
        if (future != null) {
            RpcResponse response = JSON.parseObject(frame.getPayload(), RpcResponse.class);
            future.complete(response);
        } else {
            log.warn("[NettyClientHandler] 未找到对应的 Future, sequenceId={}", frame.getSequenceId());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("[NettyClientHandler] 异常: {}", cause.getMessage(), cause);
        // 完成所有等待中的 Future（异常）
        pendingRequests.forEach((id, future) -> future.completeExceptionally(cause));
        pendingRequests.clear();
        ctx.close();
    }
}
