package com.malinghan.marpc.transport.netty;

import com.malinghan.marpc.provider.ProviderBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

@Slf4j
public class NettyRpcServer implements InitializingBean, DisposableBean {

    private final ProviderBootstrap providerBootstrap;
    private final int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public NettyRpcServer(ProviderBootstrap providerBootstrap, int port) {
        this.providerBootstrap = providerBootstrap;
        this.port = port;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new MarpcFrameDecoder())
                                .addLast(new MarpcFrameEncoder())
                                .addLast(new NettyServerHandler(providerBootstrap));
                    }
                });

        ChannelFuture future = bootstrap.bind(port).sync();
        serverChannel = future.channel();
        log.info("[NettyRpcServer] 启动，监听端口: {}", port);
    }

    @Override
    public void destroy() throws Exception {
        if (serverChannel != null) serverChannel.close().sync();
        if (bossGroup != null) bossGroup.shutdownGracefully().sync();
        if (workerGroup != null) workerGroup.shutdownGracefully().sync();
        log.info("[NettyRpcServer] 已关闭");
    }
}
