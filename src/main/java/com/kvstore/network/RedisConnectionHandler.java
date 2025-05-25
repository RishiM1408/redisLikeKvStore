package com.kvstore.network;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisConnectionHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("New connection from {}", ctx.channel().remoteAddress());
        // Send Redis 6+ compatible greeting
        String greeting = "-NOAUTH Authentication required.\r\n";
        ctx.writeAndFlush(greeting).addListener(future -> {
            if (future.isSuccess()) {
                logger.info("Sent greeting to {}", ctx.channel().remoteAddress());
            } else {
                logger.error("Failed to send greeting to {}: {}",
                        ctx.channel().remoteAddress(),
                        future.cause().getMessage());
            }
        });
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Connection closed from {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.ALL_IDLE) {
                logger.warn("Connection idle timeout for {}", ctx.channel().remoteAddress());
                // Don't send error message, just close quietly
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Channel exception for {}: {}",
                ctx.channel().remoteAddress(),
                cause.getMessage(),
                cause);
        // Don't send error message, just close quietly
        ctx.close();
    }
}