import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.LinkedList;

public class LibraryManagementServer {
    private static final int PORT = 8080;
    private static final Stack<String> returnedBooks = new Stack<>();
    private static final Queue<String> waitingStudents = new LinkedList<>();
    private static final Path PUBLIC_DIR = Path.of("public");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/books/pop", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Use POST for this operation.\"}");
                return;
            }
            synchronized (returnedBooks) {
                if (returnedBooks.empty()) {
                    sendJson(exchange, 200, "{\"message\":\"No returned books in the stack.\",\"item\":null,\"items\":[]}");
                } else {
                    String book = returnedBooks.pop();
                    sendJson(exchange, 200, "{\"message\":\"Processed returned book.\",\"item\":" + quote(book)
                            + ",\"items\":" + stackJson() + "}");
                }
            }
        });
        server.createContext("/api/books", exchange -> handleBooks(exchange));
        server.createContext("/api/waitlist/dequeue", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Use POST for this operation.\"}");
                return;
            }
            synchronized (waitingStudents) {
                if (waitingStudents.isEmpty()) {
                    sendJson(exchange, 200, "{\"message\":\"The student queue is empty.\",\"item\":null,\"items\":[]}");
                } else {
                    String student = waitingStudents.remove();
                    sendJson(exchange, 200, "{\"message\":\"Student served successfully.\",\"item\":" + quote(student)
                            + ",\"items\":" + queueJson() + "}");
                }
            }
        });
        server.createContext("/api/waitlist", exchange -> handleWaitlist(exchange));
        server.createContext("/", exchange -> serveStatic(exchange));
        server.setExecutor(null);
        server.start();
        System.out.println("Library Management System is running at http://localhost:" + PORT);
        System.out.println("Press Ctrl+C to stop the server.");
    }

    private static void handleBooks(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            synchronized (returnedBooks) {
                sendJson(exchange, 200, "{\"items\":" + stackJson() + "}");
            }
        } else if ("POST".equalsIgnoreCase(method)) {
            String book = readField(exchange, "book");
            if (book == null || book.isBlank()) {
                sendJson(exchange, 400, "{\"error\":\"Please enter a book title.\"}");
                return;
            }
            synchronized (returnedBooks) {
                returnedBooks.push(book.trim());
                sendJson(exchange, 200, "{\"message\":\"Book added to the stack.\",\"items\":" + stackJson() + "}");
            }
        } else {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed.\"}");
        }
    }

    private static void handleWaitlist(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            synchronized (waitingStudents) {
                sendJson(exchange, 200, "{\"items\":" + queueJson() + "}");
            }
        } else if ("POST".equalsIgnoreCase(method)) {
            String student = readField(exchange, "student");
            if (student == null || student.isBlank()) {
                sendJson(exchange, 400, "{\"error\":\"Please enter a student name.\"}");
                return;
            }
            synchronized (waitingStudents) {
                waitingStudents.add(student.trim());
                sendJson(exchange, 200, "{\"message\":\"Student added to the waiting queue.\",\"items\":" + queueJson() + "}");
            }
        } else {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed.\"}");
        }
    }

    // Reads a simple JSON object and extracts the requested string field.
    private static String readField(HttpExchange exchange, String field) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String key = "\"" + field + "\"";
        int keyIndex = body.indexOf(key);
        if (keyIndex < 0) return null;
        int colon = body.indexOf(':', keyIndex + key.length());
        if (colon < 0) return null;
        int firstQuote = body.indexOf('"', colon + 1);
        if (firstQuote < 0) return null;
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = firstQuote + 1; i < body.length(); i++) {
            char c = body.charAt(i);
            if (escaped) {
                value.append(c == 'n' ? '\n' : c == 't' ? '\t' : c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return value.toString();
            } else {
                value.append(c);
            }
        }
        return null;
    }

    private static String stackJson() {
        StringBuilder result = new StringBuilder("[");
        for (int i = returnedBooks.size() - 1; i >= 0; i--) {
            if (result.length() > 1) result.append(',');
            result.append(quote(returnedBooks.get(i)));
        }
        return result.append(']').toString();
    }

    private static String queueJson() {
        StringBuilder result = new StringBuilder("[");
        boolean first = true;
        for (String student : waitingStudents) {
            if (!first) result.append(',');
            result.append(quote(student));
            first = false;
        }
        return result.append(']').toString();
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (c < 32) result.append(String.format("\\u%04x", (int) c));
                    else result.append(c);
                }
            }
        }
        return result.append('"').toString();
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void serveStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/".equals(path)) path = "/index.html";
        // Prevent path traversal outside the public directory.
        Path file = PUBLIC_DIR.resolve(path.substring(1)).normalize();
        if (!file.startsWith(PUBLIC_DIR.normalize()) || !Files.isRegularFile(file)) {
            byte[] notFound = "404 - File not found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, notFound.length);
            exchange.getResponseBody().write(notFound);
            exchange.close();
            return;
        }
        String name = file.getFileName().toString();
        String contentType = name.endsWith(".html") ? "text/html; charset=UTF-8"
                : name.endsWith(".css") ? "text/css; charset=UTF-8"
                : name.endsWith(".js") ? "application/javascript; charset=UTF-8"
                : "application/octet-stream";
        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
