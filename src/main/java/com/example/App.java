package com.example;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class App {
    public static String message() { return "Simple CI/CD App is running!"; }
    public static void main(String[] args) throws Exception {
        String secret = System.getenv().getOrDefault("APP_SECRET", "secret-not-set");
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String body = message() + "\nSecret configured: " + (!"secret-not-set".equals(secret));
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        });
        server.start();
        System.out.println("Application started on port 8080");
    }
}
