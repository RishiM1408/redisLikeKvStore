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
import java.lang.ProcessHandle;

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
        logger.debug("Received command: {} from {}", cmd, ctx.channel().remoteAddress());

        try {
            switch (cmd) {
                case "PING" -> handlePing(ctx);
                case "SET" -> handleSet(ctx, command);
                case "GET" -> handleGet(ctx, command);
                case "DEL" -> handleDel(ctx, command);
                case "EXISTS" -> handleExists(ctx, command);
                case "EXPIRE" -> handleExpire(ctx, command);
                case "INFO" -> handleInfo(ctx);
                case "COMMAND" -> handleCommand(ctx);
                case "CLIENT" -> handleClient(ctx, command);
                case "CONFIG" -> handleConfig(ctx, command);
                case "HELLO" -> handleHello(ctx, command);
                case "AUTH" -> handleAuth(ctx, command);
                case "SELECT" -> handleSelect(ctx, command);
                default -> sendError(ctx, "ERR unknown command '" + cmd + "'");
            }
            logger.debug("Command {} processed successfully for {}", cmd, ctx.channel().remoteAddress());
        } catch (Exception e) {
            logger.error("Error processing command: {} from {}", cmd, ctx.channel().remoteAddress(), e);
            sendError(ctx, "ERR " + e.getMessage());
        }
    }

    private void handleAuth(ChannelHandlerContext ctx, List<String> command) {
        // In development mode, accept any auth attempt
        ctx.writeAndFlush("+OK\r\n");
    }

    private void handleSelect(ChannelHandlerContext ctx, List<String> command) {
        // Accept any database selection in development mode
        ctx.writeAndFlush("+OK\r\n");
    }

    private void handleInfo(ChannelHandlerContext ctx) {
        StringBuilder info = new StringBuilder();
        info.append("# Server\r\n");
        info.append("redis_version:1.0.0\r\n");
        info.append("redis_mode:standalone\r\n");
        info.append("os:").append(System.getProperty("os.name")).append(" ");
        info.append(System.getProperty("os.version")).append(" ");
        info.append(System.getProperty("os.arch")).append("\r\n");
        info.append("process_id:").append(ProcessHandle.current().pid()).append("\r\n");
        info.append("tcp_port:").append(6379).append("\r\n");
        info.append("uptime_in_seconds:").append(System.currentTimeMillis() / 1000).append("\r\n");
        info.append("uptime_in_days:").append(System.currentTimeMillis() / (1000 * 60 * 60 * 24)).append("\r\n");
        info.append("\r\n");

        info.append("# Clients\r\n");
        info.append("connected_clients:1\r\n");
        info.append("client_recent_max_input_buffer:0\r\n");
        info.append("client_recent_max_output_buffer:0\r\n");
        info.append("blocked_clients:0\r\n");
        info.append("\r\n");

        info.append("# Memory\r\n");
        info.append("used_memory:").append(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                .append("\r\n");
        info.append("used_memory_human:")
                .append((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024)
                .append("M\r\n");
        info.append("used_memory_peak:").append(Runtime.getRuntime().maxMemory()).append("\r\n");
        info.append("used_memory_peak_human:").append(Runtime.getRuntime().maxMemory() / 1024 / 1024).append("M\r\n");
        info.append("mem_fragmentation_ratio:1.0\r\n");
        info.append("\r\n");

        info.append("# Stats\r\n");
        info.append("total_connections_received:1\r\n");
        info.append("total_commands_processed:0\r\n");
        info.append("instantaneous_ops_per_sec:0\r\n");
        info.append("rejected_connections:0\r\n");
        info.append("sync_full:0\r\n");
        info.append("sync_partial_ok:0\r\n");
        info.append("sync_partial_err:0\r\n");
        info.append("\r\n");

        info.append("# Keyspace\r\n");
        long size = storageEngine.size();
        if (size > 0) {
            info.append("db0:keys=").append(size).append(",expires=0,avg_ttl=0\r\n");
        }

        String response = "$" + info.length() + "\r\n" + info.toString() + "\r\n";
        ctx.writeAndFlush(response);
    }

    private void handleCommand(ChannelHandlerContext ctx) {
        StringBuilder response = new StringBuilder();
        response.append("*5\r\n");
        // Command count
        response.append(":6\r\n");
        // Supported commands
        response.append("*6\r\n");
        response.append("$4\r\nPING\r\n");
        response.append("$3\r\nGET\r\n");
        response.append("$3\r\nSET\r\n");
        response.append("$3\r\nDEL\r\n");
        response.append("$6\r\nEXISTS\r\n");
        response.append("$6\r\nEXPIRE\r\n");
        ctx.writeAndFlush(response.toString());
    }

    private void handleClient(ChannelHandlerContext ctx, List<String> command) {
        if (command.size() < 2) {
            sendError(ctx, "ERR wrong number of arguments for 'client' command");
            return;
        }

        String subCommand = command.get(1).toUpperCase();
        switch (subCommand) {
            case "LIST" -> {
                String response = "*1\r\n$"
                        + "id=1 addr=127.0.0.1:6379 fd=6 name= age=0 idle=0 flags=N db=0 sub=0 psub=0 multi=-1 qbuf=0 qbuf-free=32768 obl=0 oll=0 omem=0 events=r cmd=client"
                                .length()
                        + "\r\nid=1 addr=127.0.0.1:6379 fd=6 name= age=0 idle=0 flags=N db=0 sub=0 psub=0 multi=-1 qbuf=0 qbuf-free=32768 obl=0 oll=0 omem=0 events=r cmd=client\r\n";
                ctx.writeAndFlush(response);
            }
            case "SETNAME" -> ctx.writeAndFlush("+OK\r\n");
            case "GETNAME" -> ctx.writeAndFlush("$-1\r\n");
            default -> sendError(ctx, "ERR unknown subcommand '" + subCommand + "'");
        }
    }

    private void handleConfig(ChannelHandlerContext ctx, List<String> command) {
        if (command.size() < 2) {
            sendError(ctx, "ERR wrong number of arguments for 'config' command");
            return;
        }

        String subCommand = command.get(1).toUpperCase();
        switch (subCommand) {
            case "GET" -> {
                if (command.size() < 3) {
                    sendError(ctx, "ERR wrong number of arguments for 'config get' command");
                    return;
                }
                // Return empty array for unknown config
                ctx.writeAndFlush("*0\r\n");
            }
            case "SET" -> ctx.writeAndFlush("+OK\r\n");
            default -> sendError(ctx, "ERR unknown subcommand '" + subCommand + "'");
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

    private void handleHello(ChannelHandlerContext ctx, List<String> command) {
        StringBuilder response = new StringBuilder();
        response.append("*14\r\n");
        response.append("$6\r\nserver\r\n");
        response.append("$19\r\nredis-like-kv-store\r\n");
        response.append("$7\r\nversion\r\n");
        response.append("$5\r\n1.0.0\r\n");
        response.append("$5\r\nproto\r\n");
        response.append("$1\r\n2\r\n");
        response.append("$2\r\nid\r\n");
        response.append("$1\r\n1\r\n");
        response.append("$4\r\nmode\r\n");
        response.append("$10\r\nstandalone\r\n");
        response.append("$4\r\nrole\r\n");
        response.append("$6\r\nmaster\r\n");
        response.append("$6\r\nmodules\r\n");
        response.append("*0\r\n");
        ctx.writeAndFlush(response.toString());
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