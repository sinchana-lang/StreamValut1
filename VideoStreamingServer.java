import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;

public class VideoStreamingServer {

    static final int    PORT       = 9090;
    static final String VIDEO_DIR  = "./videos";
    static final String STATIC_DIR = "./";

    public static void main(String[] args) throws IOException {

        DatabaseManager.init();
        Files.createDirectories(Paths.get("videos"));

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        AuthHandler authHandler = new AuthHandler();
        server.createContext("/api/register", authHandler);
        server.createContext("/api/login",    authHandler);

        server.createContext("/api/videos",  new VideoListHandler());
        server.createContext("/stream/",     new VideoStreamHandler());
        server.createContext("/upload",      new VideoUploadHandler());

        server.createContext("/",            new StaticFileHandler());

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("==============================================");
        System.out.println("  VideoStreaming Server is RUNNING!");
        System.out.println("  Project folder : StreamVault1");
        System.out.println("  Open browser   : http://localhost:" + PORT);
        System.out.println("  Videos folder  : " + Paths.get(VIDEO_DIR).toAbsolutePath());
        System.out.println("==============================================");
    }
}