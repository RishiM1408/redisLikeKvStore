package com.kvstore.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kvstore.core.StorageEngine;
import java.util.concurrent.TimeUnit;

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
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            logger.info("Initializing channel for client: {}", ch.remoteAddress());
                            ch.pipeline()
                                    .addLast("logging", new LoggingHandler(LogLevel.DEBUG))
                                    .addLast("idleStateHandler", new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS))
                                    .addLast("connectionHandler", new RedisConnectionHandler())
                                    .addLast("decoder", new RedisCommandDecoder())
                                    .addLast("handler", new RedisCommandHandler(storageEngine));
                            logger.info("Channel pipeline configured for client: {}", ch.remoteAddress());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_RCVBUF, 65536)
                    .childOption(ChannelOption.SO_SNDBUF, 65536)
                    .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000);

            logger.info("Starting Redis-like KV Store server on port {}", port);
            ChannelFuture f = b.bind(port).sync();
            logger.info("KVStore server started successfully on port {}", port);

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down server...");
                shutdown();
                logger.info("Server shutdown complete");
            }));

            f.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.error("Failed to start server: ", e);
            throw e;
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        logger.info("Shutting down KVStore server...");
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