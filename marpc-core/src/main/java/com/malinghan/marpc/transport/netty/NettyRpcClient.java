package com.malinghan.marpc.transport.netty;

import com.alibaba.fastjson2.JSON;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import com.malinghan.marpc.exception.MarpcNetworkException;
import com.malinghan.marpc.transport.RpcTransport;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.malinghan.marpc.exception.MarpcException.ErrorCode.NETWORK_ERROR;

@Slf4j
public class NettyRpcClient implements RpcTransport {

    private final ConcurrentHashMap<String, Channel> channelPool = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, CompletableFuture<RpcResponse>> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicInteger sequenceIdGenerator = new AtomicInteger(0);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final int timeoutMs;
    private final int nettyPort;

    public NettyRpcClient(int timeoutMs, int nettyPort) {
        this.timeoutMs = timeoutMs;
        this.nettyPort = nettyPort;
    }

    @Override
    public RpcResponse send(String instance, RpcRequest request) {
        try {
            String host = instance.split(":")[0];
            String nettyInstance = host + ":" + nettyPort;
            Channel channel = getOrCreateChannel(nettyInstance);
            int sequenceId = sequenceIdGenerator.incrementAndGet();
            CompletableFuture<RpcResponse> future = new CompletableFuture<>();
            pendingRequests.put(sequenceId, future);

            byte[] payload = JSON.toJSONBytes(request);
            MarpcFrame frame = new MarpcFrame(MarpcProtocol.TYPE_REQUEST, sequenceId, payload);
            channel.writeAndFlush(frame).addListener(f -> {
                if (!f.isSuccess()) {
                    pendingRequests.remove(sequenceId);
                    future.completeExceptionally(f.cause());
                }
            });

            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new MarpcNetworkException(NETWORK_ERROR, "Netty call failed: " + instance, e);
        }
    }

    private Channel getOrCreateChannel(String instance) {
        Channel ch = channelPool.get(instance);
        if (ch != null && ch.isActive()) return ch;
        // channel 不存在或已断开，重建
        channelPool.remove(instance);
        return channelPool.computeIfAbsent(instance, key -> {
            try {
                String[] parts = key.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);

                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(workerGroup)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline()
                                        .addLast(new MarpcFrameDecoder())
                                        .addLast(new MarpcFrameEncoder())
                                        .addLast(new NettyClientHandler(pendingRequests));
                            }
                        });

                ChannelFuture future = bootstrap.connect(host, port).sync();
                log.info("[NettyRpcClient] 连接成功: {}", key);
                return future.channel();
            } catch (Exception e) {
                throw new MarpcNetworkException(NETWORK_ERROR, "连接失败: " + key, e);
            }
        });
    }

    public void shutdown() {
        channelPool.values().forEach(Channel::close);
        workerGroup.shutdownGracefully();
    }
}
