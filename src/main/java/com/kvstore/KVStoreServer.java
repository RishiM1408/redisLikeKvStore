package com.kvstore;

public class KVStoreServer {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 6379;
        new com.kvstore.network.KVStoreServer(port).start();
    }
}