package com.kvstore.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kvstore.core.StorageEngine;

public class KVStoreServer {
    private static final Logger logger = LoggerFactory.getLogger(KVStoreServer.class);
    private final int port;
    private final StorageEngine storageEngine;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public KVStoreServer(int port) {
        this.port = port;
        this.storageEngine = new StorageEngine();
    }

    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new RedisCommandDecoder(),
                                    new RedisCommandHandler(storageEngine));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();
            logger.info("KVStore server started on port {}", port);
            f.channel().closeFuture().sync();
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        logger.info("KVStore server shutdown complete");
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 6379;
        new KVStoreServer(port).start();
    }
}