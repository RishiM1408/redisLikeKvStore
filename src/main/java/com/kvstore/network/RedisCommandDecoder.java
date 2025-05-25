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

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try {
            Object decoded = decodeRedisMessage(in);
            if (decoded != null) {
                out.add(decoded);
            }
        } catch (Exception e) {
            logger.error("Error decoding Redis message", e);
            ctx.close();
        }
    }

    private Object decodeRedisMessage(ByteBuf in) {
        if (!in.isReadable()) {
            return null;
        }

        char firstByte = (char) in.getByte(in.readerIndex());
        switch (firstByte) {
            case ARRAY_PREFIX:
                return decodeArray(in);
            case BULK_STRING_PREFIX:
                return decodeBulkString(in);
            default:
                logger.error("Unsupported Redis message type: {}", firstByte);
                return null;
        }
    }

    private List<String> decodeArray(ByteBuf in) {
        in.skipBytes(1); // Skip *
        int length = readInteger(in);
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
            return null;
        }

        if (in.readableBytes() < length + 2) { // +2 for CRLF
            return null;
        }

        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        in.skipBytes(2); // Skip CRLF

        return new String(bytes);
    }

    private int readInteger(ByteBuf in) {
        StringBuilder sb = new StringBuilder();
        char c;

        while (in.isReadable()) {
            c = (char) in.readByte();
            if (c == CR) {
                if (in.readByte() == LF) {
                    return Integer.parseInt(sb.toString());
                }
            }
            sb.append(c);
        }

        throw new IllegalStateException("Invalid integer format");
    }
}