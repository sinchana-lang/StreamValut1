import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.*;

/**
 * GET /  →  Serves index.html and other static files from STATIC_DIR.
 */
public class StaticFileHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        VideoListHandler.addCors(exchange);

        String requestPath = exchange.getRequestURI().getPath();

        if (requestPath.contains("..")) {
            sendError(exchange, 400, "Bad request");
            return;
        }

        if (requestPath.equals("/") || requestPath.isEmpty()) {
            requestPath = "/index.html";
        }

        Path filePath = Paths.get(VideoStreamingServer.STATIC_DIR, requestPath).normalize();

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            filePath = Paths.get(VideoStreamingServer.STATIC_DIR, "index.html");
        }

        if (!Files.exists(filePath)) {
            sendError(exchange, 404, "Not found");
            return;
        }

        byte[] body = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().set("Content-Type", detectMime(filePath.getFileName().toString()));
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private String detectMime(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filename.endsWith(".css"))  return "text/css; charset=UTF-8";
        if (filename.endsWith(".js"))   return "application/javascript; charset=UTF-8";
        if (filename.endsWith(".json")) return "application/json";
        if (filename.endsWith(".png"))  return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".svg"))  return "image/svg+xml";
        return "application/octet-stream";
    }

    private void sendError(HttpExchange ex, int code, String msg) throws IOException {
        byte[] body = msg.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "text/plain");
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }
}
