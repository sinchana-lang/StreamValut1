import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * GET /api/videos
 * Returns a JSON array of video metadata found in the VIDEO_DIR folder.
 */
public class VideoListHandler implements HttpHandler {

    private static final Set<String> VIDEO_EXTENSIONS =
            new HashSet<>(Arrays.asList(".mp4", ".mkv", ".webm", ".avi", ".mov", ".m4v"));

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        File dir = new File(VideoStreamingServer.VIDEO_DIR);
        File[] files = dir.listFiles();

        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            for (File f : files) {
                if (!f.isFile()) continue;
                String ext = getExtension(f.getName()).toLowerCase();
                if (!VIDEO_EXTENSIONS.contains(ext)) continue;

                if (!first) json.append(",");
                first = false;

                String id   = sanitizeId(f.getName());
                String name = stripExtension(f.getName());
                long   size = f.length();

                json.append("{")
                    .append("\"id\":\"").append(escapeJson(id)).append("\",")
                    .append("\"name\":\"").append(escapeJson(name)).append("\",")
                    .append("\"size\":\"").append(humanSize(size)).append("\",")
                    .append("\"genre\":\"Video\",")
                    .append("\"duration\":\"\"")
                    .append("}");
            }
        }
        json.append("]");

        byte[] body = json.toString().getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }

    private String sanitizeId(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.0f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.0f MB", mb);
        return String.format("%.2f GB", mb / 1024.0);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static void addCors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Range");
        ex.getResponseHeaders().set("Access-Control-Expose-Headers","Content-Range, Accept-Ranges, Content-Length");
    }
}
