import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class AuthHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        VideoListHandler.addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (path.equals("/api/register")) {
            handleRegister(exchange, body);
        } else if (path.equals("/api/login")) {
            handleLogin(exchange, body);
        } else {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    private void handleRegister(HttpExchange exchange, String body) throws IOException {
        String name     = extractJson(body, "name");
        String email    = extractJson(body, "email");
        String phone    = extractJson(body, "phone");
        String age      = extractJson(body, "age");
        String password = extractJson(body, "password");

        if (name == null || email == null || password == null ||
            name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Name, email, and password are required\"}");
            return;
        }
        if (!email.contains("@")) {
            sendJson(exchange, 400, "{\"error\":\"Invalid email address\"}");
            return;
        }
        if (password.length() < 4) {
            sendJson(exchange, 400, "{\"error\":\"Password must be at least 4 characters\"}");
            return;
        }

        String sql = "INSERT INTO users (name, email, phone, age, password) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email.toLowerCase());
            ps.setString(3, phone == null ? "" : phone);
            ps.setString(4, age   == null ? "" : age);
            ps.setString(5, password);
            ps.executeUpdate();
            System.out.println("[auth] Registered: " + email);
            sendJson(exchange, 200,
                "{\"success\":true,\"name\":\"" + escapeJson(name) +
                "\",\"email\":\"" + escapeJson(email) + "\"}");
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate") || e.getMessage().contains("duplicate")) {
                sendJson(exchange, 409, "{\"error\":\"Email already registered\"}");
            } else {
                System.out.println("[auth] DB error: " + e.getMessage());
                sendJson(exchange, 500, "{\"error\":\"Database error\"}");
            }
        }
    }

    private void handleLogin(HttpExchange exchange, String body) throws IOException {
        String email    = extractJson(body, "email");
        String password = extractJson(body, "password");

        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Email and password are required\"}");
            return;
        }

        String sql = "SELECT name, email, phone, age, password FROM users WHERE email = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (!rs.next() || !rs.getString("password").equals(password)) {
                sendJson(exchange, 401, "{\"error\":\"Invalid email or password\"}");
                return;
            }
            System.out.println("[auth] Login: " + email);
            sendJson(exchange, 200,
                "{\"success\":true,\"name\":\"" + escapeJson(rs.getString("name")) +
                "\",\"email\":\"" + escapeJson(rs.getString("email")) +
                "\",\"phone\":\"" + escapeJson(rs.getString("phone")) +
                "\",\"age\":\"" + escapeJson(rs.getString("age")) + "\"}");
        } catch (SQLException e) {
            sendJson(exchange, 500, "{\"error\":\"Database error\"}");
        }
    }

    private String extractJson(String json, String key) {
        String search = "\"" + key + "\"";
        int ki = json.indexOf(search);
        if (ki < 0) return null;
        int colon = json.indexOf(':', ki + search.length());
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bodyBytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, bodyBytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bodyBytes); }
    }
}