package com.kvstore.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import com.kvstore.core.StorageEngine;
import com.kvstore.core.DataType;
import com.kvstore.core.StorageEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;
import java.time.Duration;
import java.time.Instant;

public class RedisCommandHandler extends SimpleChannelInboundHandler<List<String>> {
    private static final Logger logger = LoggerFactory.getLogger(RedisCommandHandler.class);
    private final StorageEngine storageEngine;

    public RedisCommandHandler(StorageEngine storageEngine) {
        this.storageEngine = storageEngine;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, List<String> command) {
        if (command == null || command.isEmpty()) {
            sendError(ctx, "ERR empty command");
            return;
        }

        String cmd = command.get(0).toUpperCase();
        try {
            switch (cmd) {
                case "PING":
                    handlePing(ctx);
                    break;
                case "SET":
                    handleSet(ctx, command);
                    break;
                case "GET":
                    handleGet(ctx, command);
                    break;
                case "DEL":
                    handleDel(ctx, command);
                    break;
                case "EXISTS":
                    handleExists(ctx, command);
                    break;
                case "EXPIRE":
                    handleExpire(ctx, command);
                    break;
                default:
                    sendError(ctx, "ERR unknown command '" + cmd + "'");
            }
        } catch (Exception e) {
            logger.error("Error processing command: " + cmd, e);
            sendError(ctx, "ERR " + e.getMessage());
        }
    }

    private void handlePing(ChannelHandlerContext ctx) {
        ctx.writeAndFlush("+PONG\r\n");
    }

    private void handleSet(ChannelHandlerContext ctx, List<String> command) {
        if (command.size() < 3) {
            sendError(ctx, "ERR wrong number of arguments for 'set' command");
            return;
        }

        String key = command.get(1);
        String value = command.get(2);

        // Handle optional EX/PX argument
        if (command.size() >= 5) {
            String expireType = command.get(3).toUpperCase();
            try {
                long expireValue = Long.parseLong(command.get(4));
                Instant expireAt = switch (expireType) {
                    case "EX" -> Instant.now().plusSeconds(expireValue);
                    case "PX" -> Instant.now().plusMillis(expireValue);
                    default -> throw new IllegalArgumentException("Invalid expire type: " + expireType);
                };

                StorageEntry entry = new StorageEntry(value, DataType.STRING);
                entry.setExpiresAt(expireAt);
                storageEngine.set(key, entry, DataType.STRING);
            } catch (NumberFormatException e) {
                sendError(ctx, "ERR value is not an integer or out of range");
                return;
            }
        } else {
            storageEngine.set(key, value, DataType.STRING);
        }

        ctx.writeAndFlush("+OK\r\n");
    }

    private void handleGet(ChannelHandlerContext ctx, List<String> command) {
        if (command.size() != 2) {
            sendError(ctx, "ERR wrong number of arguments for 'get' command");
            return;
        }

        String key = command.get(1);
        Optional<StorageEntry> entry = storageEngine.get(key);

        if (entry.isEmpty() || entry.get().isExpired()) {
            ctx.writeAndFlush("$-1\r\n");
            return;
        }

        String value = (String) entry.get().getValue();
        ctx.writeAndFlush("$" + value.length() + "\r\n" + value + "\r\n");
    }

    private void handleDel(ChannelHandlerContext ctx, List<String> command) {
        if (command.size() != 2) {
            sendError(ctx, "ERR wrong number of arguments for 'del' command");
            return;
        }

        String key = command.get(1);
        boolean deleted = storageEngine.delete(key);
        ctx.writeAndFlush(":" + (deleted ? 1 : 0) + "\r\n");
    }

    private void handleExists(ChannelHandlerContext ctx, List<String> command) {
        if (command.size() != 2) {
            sendError(ctx, "ERR wrong number of arguments for 'exists' command");
            return;
        }

        String key = command.get(1);
        boolean exists = storageEngine.exists(key);
        ctx.writeAndFlush(":" + (exists ? 1 : 0) + "\r\n");
    }

    private void handleExpire(ChannelHandlerContext ctx, List<String> command) {
        if (command.size() != 3) {
            sendError(ctx, "ERR wrong number of arguments for 'expire' command");
            return;
        }

        String key = command.get(1);
        try {
            long seconds = Long.parseLong(command.get(2));
            Optional<StorageEntry> entry = storageEngine.get(key);

            if (entry.isEmpty()) {
                ctx.writeAndFlush(":0\r\n");
                return;
            }

            entry.get().setExpiresAt(Instant.now().plusSeconds(seconds));
            ctx.writeAndFlush(":1\r\n");
        } catch (NumberFormatException e) {
            sendError(ctx, "ERR value is not an integer or out of range");
        }
    }

    private void sendError(ChannelHandlerContext ctx, String message) {
        ctx.writeAndFlush("-" + message + "\r\n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Channel exception", cause);
        ctx.close();
    }
}