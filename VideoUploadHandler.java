import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * POST /upload
 * Accepts multipart/form-data with a "file" field and saves to VIDEO_DIR.
 */
public class VideoUploadHandler implements HttpHandler {

    private static final long MAX_FILE_SIZE = 4L * 1024 * 1024 * 1024; // 4 GB

    private static final Set<String> ALLOWED_EXT =
            new HashSet<>(Arrays.asList(".mp4", ".mkv", ".webm", ".avi", ".mov", ".m4v"));

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        VideoListHandler.addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            sendJson(exchange, 400, "{\"error\":\"Expected multipart/form-data\"}");
            return;
        }

        String boundary = null;
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                boundary = part.substring(9).trim();
                break;
            }
        }
        if (boundary == null) {
            sendJson(exchange, 400, "{\"error\":\"Missing boundary\"}");
            return;
        }

        try {
            MultipartData data = parseMultipart(exchange.getRequestBody(), boundary);

            if (data == null || data.filename == null) {
                sendJson(exchange, 400, "{\"error\":\"No file field found\"}");
                return;
            }

            String ext = getExtension(data.filename).toLowerCase();
            if (!ALLOWED_EXT.contains(ext)) {
                sendJson(exchange, 415, "{\"error\":\"Unsupported file type: " + ext + "\"}");
                return;
            }

            String safeName = Paths.get(data.filename).getFileName().toString()
                    .replaceAll("[^a-zA-Z0-9._-]", "_");

            Path dest = Paths.get(VideoStreamingServer.VIDEO_DIR, safeName);
            Files.write(dest, data.bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("[upload] Saved: " + dest.toAbsolutePath() + " (" + humanSize(data.bytes.length) + ")");

            sendJson(exchange, 200,
                    "{\"success\":true,\"name\":\"" + escapeJson(safeName) + "\",\"size\":" + data.bytes.length + "}");

        } catch (OutOfMemoryError e) {
            sendJson(exchange, 413, "{\"error\":\"File too large for memory\"}");
        }
    }

    private MultipartData parseMultipart(InputStream body, String boundary) throws IOException {
        byte[] raw = body.readAllBytes();
        String header = new String(raw, 0, Math.min(raw.length, 4096), "UTF-8");

        String filename = null;
        for (String line : header.split("\r\n")) {
            if (line.startsWith("Content-Disposition")) {
                int fi = line.indexOf("filename=\"");
                if (fi >= 0) {
                    int start = fi + 10;
                    int end   = line.indexOf("\"", start);
                    filename  = end > start ? line.substring(start, end) : null;
                }
            }
        }
        if (filename == null) return null;

        byte[] headerSep = "\r\n\r\n".getBytes("UTF-8");
        int dataStart = indexOf(raw, headerSep, 0);
        if (dataStart < 0) return null;
        dataStart += headerSep.length;

        byte[] boundaryBytes = ("\r\n--" + boundary).getBytes("UTF-8");
        int dataEnd = indexOf(raw, boundaryBytes, dataStart);
        if (dataEnd < 0) dataEnd = raw.length;

        byte[] fileBytes = Arrays.copyOfRange(raw, dataStart, dataEnd);

        MultipartData md = new MultipartData();
        md.filename = filename;
        md.bytes    = fileBytes;
        return md;
    }

    private int indexOf(byte[] haystack, byte[] needle, int from) {
        outer:
        for (int i = from; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    static class MultipartData {
        String filename;
        byte[] bytes;
    }

    private String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    private String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double mb = bytes / (1024.0 * 1024);
        return String.format("%.1f MB", mb);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] body = json.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }
}
