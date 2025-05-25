package com.kvstore.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;

public class RedisCommandDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(RedisCommandDecoder.class);
    private static final char CR = '\r';
    private static final char LF = '\n';
    private static final char ARRAY_PREFIX = '*';
    private static final char BULK_STRING_PREFIX = '$';
    private static final char INTEGER_PREFIX = ':';
    private static final char SIMPLE_STRING_PREFIX = '+';
    private static final char ERROR_PREFIX = '-';

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try {
            Object decoded = decodeRedisMessage(in);
            if (decoded != null) {
                out.add(decoded);
            }
        } catch (Exception e) {
            logger.error("Error decoding Redis message", e);
            // Send error response instead of closing connection
            ctx.writeAndFlush("-ERR Protocol error: " + e.getMessage() + "\r\n");
        }
    }

    private Object decodeRedisMessage(ByteBuf in) {
        if (!in.isReadable()) {
            return null;
        }

        char firstByte = (char) in.getByte(in.readerIndex());
        try {
            return switch (firstByte) {
                case ARRAY_PREFIX -> decodeArray(in);
                case BULK_STRING_PREFIX -> decodeBulkString(in);
                case INTEGER_PREFIX -> decodeInteger(in);
                case SIMPLE_STRING_PREFIX -> decodeSimpleString(in);
                case ERROR_PREFIX -> decodeError(in);
                default -> {
                    logger.warn("Unsupported Redis message type: {}", firstByte);
                    yield null;
                }
            };
        } catch (Exception e) {
            logger.error("Failed to decode message type {}: {}", firstByte, e.getMessage());
            throw e;
        }
    }

    private List<String> decodeArray(ByteBuf in) {
        in.skipBytes(1); // Skip *
        int length = readInteger(in);

        if (length == -1) {
            return null; // Null array
        }

        if (length < 0) {
            throw new IllegalArgumentException("Invalid array length: " + length);
        }

        List<String> array = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            String element = decodeBulkString(in);
            if (element != null) {
                array.add(element);
            }
        }
        return array;
    }

    private String decodeBulkString(ByteBuf in) {
        in.skipBytes(1); // Skip $
        int length = readInteger(in);

        if (length == -1) {
            return null; // Null string
        }

        if (length < 0) {
            throw new IllegalArgumentException("Invalid bulk string length: " + length);
        }

        if (in.readableBytes() < length + 2) { // +2 for CRLF
            throw new IllegalStateException("Not enough data for bulk string of length " + length);
        }

        byte[] bytes = new byte[length];
        in.readBytes(bytes);

        // Verify CRLF
        if (in.readByte() != CR || in.readByte() != LF) {
            throw new IllegalStateException("Missing CRLF terminator");
        }

        return new String(bytes);
    }

    private Long decodeInteger(ByteBuf in) {
        in.skipBytes(1); // Skip :
        String num = readLine(in);
        return Long.parseLong(num);
    }

    private String decodeSimpleString(ByteBuf in) {
        in.skipBytes(1); // Skip +
        return readLine(in);
    }

    private String decodeError(ByteBuf in) {
        in.skipBytes(1); // Skip -
        return readLine(in);
    }

    private int readInteger(ByteBuf in) {
        return Integer.parseInt(readLine(in));
    }

    private String readLine(ByteBuf in) {
        StringBuilder sb = new StringBuilder();
        char c;

        while (in.isReadable()) {
            c = (char) in.readByte();
            if (c == CR) {
                if (!in.isReadable()) {
                    throw new IllegalStateException("Missing LF in CRLF");
                }
                if (in.readByte() == LF) {
                    return sb.toString();
                }
                throw new IllegalStateException("Expected LF after CR");
            }
            sb.append(c);
        }
        throw new IllegalStateException("Incomplete line");
    }
}