import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * GET /stream/{videoId}
 * Streams video with HTTP Range Request support (RFC 7233).
 */
public class VideoStreamHandler implements HttpHandler {

    private static final int  CHUNK_SIZE = 1024 * 256; // 256 KB
    private static final long MAX_RANGE  = 1024L * 1024 * 4; // 4 MB max slice

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        VideoListHandler.addCors(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path    = exchange.getRequestURI().getPath();
        String videoId = path.replaceFirst("^/stream/", "");

        if (videoId.contains("..") || videoId.contains("/")) {
            sendError(exchange, 400, "Invalid video id");
            return;
        }

        File videoFile = resolveVideo(videoId);
        if (videoFile == null || !videoFile.isFile()) {
            sendError(exchange, 404, "Video not found: " + videoId);
            return;
        }

        long fileSize = videoFile.length();
        String mimeType = detectMime(videoFile.getName());

        String rangeHeader = exchange.getRequestHeaders().getFirst("Range");

        long start = 0;
        long end   = Math.min(fileSize - 1, MAX_RANGE - 1);
        boolean isRangeRequest = false;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            isRangeRequest = true;
            String[] parts = rangeHeader.substring(6).split("-");
            try {
                start = Long.parseLong(parts[0].trim());
                end   = (parts.length > 1 && !parts[1].trim().isEmpty())
                        ? Long.parseLong(parts[1].trim())
                        : Math.min(fileSize - 1, start + MAX_RANGE - 1);
            } catch (NumberFormatException e) {
                sendError(exchange, 416, "Invalid Range header");
                return;
            }

            if (start >= fileSize || end >= fileSize || start > end) {
                exchange.getResponseHeaders().set("Content-Range", "bytes */" + fileSize);
                sendError(exchange, 416, "Range not satisfiable");
                return;
            }
        }

        long contentLength = end - start + 1;

        exchange.getResponseHeaders().set("Content-Type",   mimeType);
        exchange.getResponseHeaders().set("Accept-Ranges",  "bytes");
        exchange.getResponseHeaders().set("Content-Length", String.valueOf(contentLength));

        if (isRangeRequest) {
            exchange.getResponseHeaders().set("Content-Range",
                    "bytes " + start + "-" + end + "/" + fileSize);
            exchange.sendResponseHeaders(206, contentLength);
        } else {
            exchange.sendResponseHeaders(200, contentLength);
        }

        try (RandomAccessFile raf = new RandomAccessFile(videoFile, "r");
             OutputStream out    = exchange.getResponseBody()) {

            raf.seek(start);
            byte[] buffer    = new byte[CHUNK_SIZE];
            long   remaining = contentLength;

            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read   = raf.read(buffer, 0, toRead);
                if (read < 0) break;
                out.write(buffer, 0, read);
                remaining -= read;
            }
            out.flush();
        } catch (IOException e) {
            System.out.println("[stream] Client disconnected: " + videoId);
        }
    }

    private File resolveVideo(String videoId) {
        File dir = new File(VideoStreamingServer.VIDEO_DIR);
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File f : files) {
            if (f.getName().replaceAll("[^a-zA-Z0-9._-]", "_").equals(videoId)) return f;
        }
        File direct = new File(dir, videoId);
        return direct.exists() ? direct : null;
    }

    private String detectMime(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp4"))  return "video/mp4";
        if (lower.endsWith(".mkv"))  return "video/x-matroska";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".avi"))  return "video/x-msvideo";
        if (lower.endsWith(".mov"))  return "video/quicktime";
        if (lower.endsWith(".m4v"))  return "video/mp4";
        return "application/octet-stream";
    }

    private void sendError(HttpExchange ex, int code, String msg) throws IOException {
        byte[] body = msg.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }
}
