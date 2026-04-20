import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * QuelessIndia — Java HTTP Backend
 * Run: javac -cp ".;mysql-connector-j-9.x.x.jar" Server.java
 *       java  -cp ".;mysql-connector-j-9.x.x.jar" Server
 * Open: http://localhost:8080
 */
public class Server {

    // ========== DB CONFIG — change these ==========
    static final String DB_URL  = "jdbc:mysql://localhost:3306/queless_india?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    static final String DB_USER = "root";
    static final String DB_PASS = "";   // ← your MySQL password

    public static void main(String[] args) throws Exception {
        initDatabase();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Static files (serve HTML/CSS/JS from ./web/ folder)
        server.createContext("/", new StaticHandler());

        // API routes
        server.createContext("/api/login",            exchange -> route(exchange, Server::handleLogin));
        server.createContext("/api/register",         exchange -> route(exchange, Server::handleRegister));
        server.createContext("/api/services",         exchange -> route(exchange, Server::handleServices));
        server.createContext("/api/services/status",  exchange -> route(exchange, Server::handleServicesStatus));
        server.createContext("/api/tokens",           exchange -> route(exchange, Server::handleTokens));
        server.createContext("/api/queue-status",     exchange -> route(exchange, Server::handleQueueStatus));
        server.createContext("/api/admin/tokens",     exchange -> route(exchange, Server::handleAdminTokens));
        server.createContext("/api/admin/users",      exchange -> route(exchange, Server::handleAdminUsers));
        server.createContext("/api/queue/next",       exchange -> route(exchange, Server::handleQueueNext));
        server.createContext("/api/queue/set",        exchange -> route(exchange, Server::handleQueueSet));
        server.createContext("/api/queue/reset",      exchange -> route(exchange, Server::handleQueueReset));

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("✅ QuelessIndia server running at http://localhost:8080");
    }

    // ========== ROUTING HELPER ==========
    interface Handler { void handle(HttpExchange ex) throws Exception; }

    static void route(HttpExchange ex, Handler h) {
        try {
            addCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
            h.handle(ex);
        } catch (Exception e) {
            try { sendJson(ex, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}"); }
            catch (Exception ignored) {}
        }
    }

    static void addCors(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.add("Access-Control-Allow-Origin",  "*");
        h.add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type");
    }

    // ========== STATIC FILE HANDLER ==========
    static class StaticHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            addCors(ex);
            String path = ex.getRequestURI().getPath();
            if ("/".equals(path)) path = "/index.html";
            File f = new File("web" + path);
            if (!f.exists() || f.isDirectory()) {
                byte[] msg = "404 Not Found".getBytes();
                ex.sendResponseHeaders(404, msg.length);
                ex.getResponseBody().write(msg);
                ex.getResponseBody().close();
                return;
            }
            String ct = path.endsWith(".html") ? "text/html" :
                        path.endsWith(".css")  ? "text/css"  :
                        path.endsWith(".js")   ? "application/javascript" : "application/octet-stream";
            ex.getResponseHeaders().set("Content-Type", ct + ";charset=UTF-8");
            byte[] data = Files.readAllBytes(f.toPath());
            ex.sendResponseHeaders(200, data.length);
            ex.getResponseBody().write(data);
            ex.getResponseBody().close();
        }
    }

    // ========== HANDLERS ==========

    // POST /api/login
    static void handleLogin(HttpExchange ex) throws Exception {
        Map<String,String> body = parseJson(readBody(ex));
        String u = body.get("username"), p = body.get("password");
        try (Connection c = getCon();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id, role, full_name FROM users WHERE username=? AND password=?")) {
            ps.setString(1, u); ps.setString(2, p);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                sendJson(ex, 200, String.format(
                    "{\"success\":true,\"userId\":%d,\"role\":\"%s\",\"fullName\":\"%s\"}",
                    rs.getInt("id"), rs.getString("role"),
                    esc(rs.getString("full_name"))));
            } else {
                sendJson(ex, 401, "{\"success\":false,\"message\":\"Invalid credentials\"}");
            }
        }
    }

    // POST /api/register
    static void handleRegister(HttpExchange ex) throws Exception {
        Map<String,String> b = parseJson(readBody(ex));
        try (Connection c = getCon();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO users(username,password,full_name,email,role) VALUES(?,?,?,?,'USER')")) {
            ps.setString(1, b.get("username")); ps.setString(2, b.get("password"));
            ps.setString(3, b.get("fullName")); ps.setString(4, b.get("email"));
            ps.executeUpdate();
            sendJson(ex, 200, "{\"success\":true}");
        } catch (SQLIntegrityConstraintViolationException e) {
            sendJson(ex, 409, "{\"success\":false,\"message\":\"Username already taken\"}");
        }
    }

    // GET /api/services
    static void handleServices(HttpExchange ex) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        try (Connection c = getCon();
             ResultSet rs = c.createStatement().executeQuery("SELECT service_type FROM service_counter")) {
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(","); first = false;
                sb.append("\"").append(esc(rs.getString(1))).append("\"");
            }
        }
        sb.append("]");
        sendJson(ex, 200, sb.toString());
    }

    // GET /api/services/status
    static void handleServicesStatus(HttpExchange ex) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        try (Connection c = getCon();
             ResultSet rs = c.createStatement().executeQuery("SELECT service_type, current_token, last_issued FROM service_counter")) {
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(","); first = false;
                sb.append(String.format("{\"serviceType\":\"%s\",\"currentToken\":%d,\"lastIssued\":%d}",
                    esc(rs.getString(1)), rs.getInt(2), rs.getInt(3)));
            }
        }
        sb.append("]");
        sendJson(ex, 200, sb.toString());
    }

    // GET /api/tokens?userId=X   POST /api/tokens   DELETE /api/tokens/:id?userId=X
    static void handleTokens(HttpExchange ex) throws Exception {
        String method = ex.getRequestMethod();
        String query  = ex.getRequestURI().getQuery();
        String path   = ex.getRequestURI().getPath();

        if ("GET".equals(method)) {
            int uid = Integer.parseInt(getParam(query, "userId"));
            StringBuilder sb = new StringBuilder("[");
            try (Connection c = getCon();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM tokens WHERE user_id=? ORDER BY id DESC")) {
                ps.setInt(1, uid);
                ResultSet rs = ps.executeQuery();
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(","); first = false;
                    sb.append(tokenToJson(rs));
                }
            }
            sb.append("]");
            sendJson(ex, 200, sb.toString());

        } else if ("POST".equals(method)) {
            Map<String,String> b = parseJson(readBody(ex));
            String svc = b.get("serviceType");
            int uid    = Integer.parseInt(b.get("userId"));

            try (Connection c = getCon()) {
                c.setAutoCommit(false);
                PreparedStatement ps = c.prepareStatement(
                    "SELECT last_issued, current_token FROM service_counter WHERE service_type=? FOR UPDATE");
                ps.setString(1, svc);
                ResultSet rs = ps.executeQuery(); rs.next();
                int last = rs.getInt(1), cur = rs.getInt(2);
                int tokNo = last + 1;
                int ahead = Math.max(0, tokNo - cur - 1);
                String report = "~" + (ahead * 10) + " min from now";
                String reason = b.getOrDefault("reason","") +
                    (b.getOrDefault("notes","").isEmpty() ? "" : " | " + b.get("notes"));

                c.prepareStatement("UPDATE service_counter SET last_issued=" + tokNo + " WHERE service_type='" + svc + "'").executeUpdate();

                PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO tokens(token_no,user_id,name,service_type,reason,appointment_time,report_time,status) VALUES(?,?,?,?,?,?,?,'WAITING')");
                ins.setInt(1, tokNo); ins.setInt(2, uid);
                ins.setString(3, b.get("name")); ins.setString(4, svc);
                ins.setString(5, reason); ins.setString(6, b.get("appointmentTime"));
                ins.setString(7, report); ins.executeUpdate();
                c.commit();

                sendJson(ex, 200, String.format(
                    "{\"success\":true,\"tokenNo\":%d,\"serviceType\":\"%s\",\"currentToken\":%d,\"ahead\":%d,\"reportTime\":\"%s\"}",
                    tokNo, esc(svc), cur, ahead, esc(report)));
            }

        } else if ("DELETE".equals(method)) {
            // path = /api/tokens/123
            int id  = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
            int uid = Integer.parseInt(getParam(query, "userId"));
            try (Connection c = getCon();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM tokens WHERE id=? AND user_id=?")) {
                ps.setInt(1, id); ps.setInt(2, uid);
                int rows = ps.executeUpdate();
                sendJson(ex, 200, "{\"success\":" + (rows > 0) + "}");
            }
        }
    }

    // GET /api/queue-status?service=Hospital
    static void handleQueueStatus(HttpExchange ex) throws Exception {
        String svc = URLDecoder.decode(getParam(ex.getRequestURI().getQuery(), "service"), "UTF-8");
        try (Connection c = getCon();
             PreparedStatement ps = c.prepareStatement("SELECT current_token FROM service_counter WHERE service_type=?")) {
            ps.setString(1, svc);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) sendJson(ex, 200, "{\"currentToken\":" + rs.getInt(1) + "}");
            else sendJson(ex, 404, "{\"error\":\"Service not found\"}");
        }
    }

    // /api/admin/tokens  — GET all, PUT /:id, DELETE /:id, PUT /:id/complete
    static void handleAdminTokens(HttpExchange ex) throws Exception {
        String method = ex.getRequestMethod();
        String path   = ex.getRequestURI().getPath(); // /api/admin/tokens  or /api/admin/tokens/5  or /api/admin/tokens/5/complete

        if ("GET".equals(method)) {
            StringBuilder sb = new StringBuilder("[");
            try (Connection c = getCon();
                 ResultSet rs = c.createStatement().executeQuery("SELECT * FROM tokens ORDER BY id DESC")) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(","); first = false;
                    sb.append(tokenToJson(rs));
                }
            }
            sb.append("]");
            sendJson(ex, 200, sb.toString());

        } else if ("PUT".equals(method)) {
            String[] parts = path.split("/");
            int id = Integer.parseInt(parts[parts.length - (path.endsWith("/complete") ? 2 : 1)]);
            if (path.endsWith("/complete")) {
                try (Connection c = getCon();
                     PreparedStatement ps = c.prepareStatement("UPDATE tokens SET status='COMPLETED' WHERE id=?")) {
                    ps.setInt(1, id); ps.executeUpdate();
                    sendJson(ex, 200, "{\"success\":true}");
                }
            } else {
                Map<String,String> b = parseJson(readBody(ex));
                try (Connection c = getCon();
                     PreparedStatement ps = c.prepareStatement(
                         "UPDATE tokens SET name=?,service_type=?,reason=?,appointment_time=?,report_time=?,status=? WHERE id=?")) {
                    ps.setString(1, b.get("name")); ps.setString(2, b.get("serviceType"));
                    ps.setString(3, b.get("reason")); ps.setString(4, b.get("appointmentTime"));
                    ps.setString(5, b.get("reportTime")); ps.setString(6, b.get("status"));
                    ps.setInt(7, id); ps.executeUpdate();
                    sendJson(ex, 200, "{\"success\":true}");
                }
            }

        } else if ("DELETE".equals(method)) {
            int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
            try (Connection c = getCon();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM tokens WHERE id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
                sendJson(ex, 200, "{\"success\":true}");
            }
        }
    }

    // /api/admin/users — GET all, PUT /:id, DELETE /:id
    static void handleAdminUsers(HttpExchange ex) throws Exception {
        String method = ex.getRequestMethod();
        String path   = ex.getRequestURI().getPath();

        if ("GET".equals(method)) {
            StringBuilder sb = new StringBuilder("[");
            try (Connection c = getCon();
                 ResultSet rs = c.createStatement().executeQuery("SELECT id,username,full_name,email,role FROM users ORDER BY id")) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(","); first = false;
                    sb.append(String.format("{\"id\":%d,\"username\":\"%s\",\"fullName\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}",
                        rs.getInt(1), esc(rs.getString(2)), esc(rs.getString(3)), esc(rs.getString(4)), esc(rs.getString(5))));
                }
            }
            sb.append("]");
            sendJson(ex, 200, sb.toString());

        } else if ("PUT".equals(method)) {
            int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
            Map<String,String> b = parseJson(readBody(ex));
            try (Connection c = getCon();
                 PreparedStatement ps = c.prepareStatement("UPDATE users SET full_name=?,email=?,role=? WHERE id=?")) {
                ps.setString(1, b.get("fullName")); ps.setString(2, b.get("email"));
                ps.setString(3, b.get("role")); ps.setInt(4, id);
                ps.executeUpdate();
                sendJson(ex, 200, "{\"success\":true}");
            }

        } else if ("DELETE".equals(method)) {
            int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
            try (Connection c = getCon()) {
                c.createStatement().executeUpdate("DELETE FROM tokens WHERE user_id=" + id);
                c.createStatement().executeUpdate("DELETE FROM users WHERE id=" + id + " AND role!='ADMIN'");
                sendJson(ex, 200, "{\"success\":true}");
            }
        }
    }

    // POST /api/queue/next
    static void handleQueueNext(HttpExchange ex) throws Exception {
        Map<String,String> b = parseJson(readBody(ex));
        String svc = b.get("serviceType");
        try (Connection c = getCon()) {
            c.createStatement().executeUpdate(
                "UPDATE service_counter SET current_token=current_token+1 WHERE service_type='" + svc + "'");
            ResultSet rs = c.createStatement().executeQuery(
                "SELECT current_token FROM service_counter WHERE service_type='" + svc + "'");
            rs.next(); int cur = rs.getInt(1);
            c.createStatement().executeUpdate(
                "UPDATE tokens SET status='SERVING' WHERE service_type='" + svc + "' AND token_no=" + cur);
            c.createStatement().executeUpdate(
                "UPDATE tokens SET status='COMPLETED' WHERE service_type='" + svc + "' AND token_no<" + cur);
            sendJson(ex, 200, "{\"success\":true,\"currentToken\":" + cur + "}");
        }
    }

    // POST /api/queue/set
    static void handleQueueSet(HttpExchange ex) throws Exception {
        Map<String,String> b = parseJson(readBody(ex));
        String svc = b.get("serviceType");
        int n      = Integer.parseInt(b.get("tokenNo"));
        try (Connection c = getCon();
             PreparedStatement ps = c.prepareStatement("UPDATE service_counter SET current_token=? WHERE service_type=?")) {
            ps.setInt(1, n); ps.setString(2, svc); ps.executeUpdate();
            sendJson(ex, 200, "{\"success\":true}");
        }
    }

    // POST /api/queue/reset
    static void handleQueueReset(HttpExchange ex) throws Exception {
        Map<String,String> b = parseJson(readBody(ex));
        String svc = b.get("serviceType");
        try (Connection c = getCon();
             PreparedStatement ps = c.prepareStatement("UPDATE service_counter SET current_token=0,last_issued=0 WHERE service_type=?")) {
            ps.setString(1, svc); ps.executeUpdate();
            sendJson(ex, 200, "{\"success\":true}");
        }
    }

    // ========== DB INIT ==========
    static void initDatabase() throws SQLException {
        try (Connection base = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                DB_USER, DB_PASS)) {
            base.createStatement().executeUpdate("CREATE DATABASE IF NOT EXISTS queless_india");
        }
        try (Connection c = getCon(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(80) UNIQUE NOT NULL, password VARCHAR(80) NOT NULL, full_name VARCHAR(120), email VARCHAR(120), role VARCHAR(20) DEFAULT 'USER')");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS tokens (id INT AUTO_INCREMENT PRIMARY KEY, token_no INT NOT NULL, user_id INT, name VARCHAR(120), service_type VARCHAR(60), reason VARCHAR(255), appointment_time VARCHAR(30), report_time VARCHAR(30), status VARCHAR(20) DEFAULT 'WAITING', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS service_counter (service_type VARCHAR(60) PRIMARY KEY, current_token INT DEFAULT 0, last_issued INT DEFAULT 0)");
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users WHERE role='ADMIN'");
            rs.next();
            if (rs.getInt(1) == 0)
                st.executeUpdate("INSERT INTO users(username,password,full_name,email,role) VALUES('admin','admin123','Administrator','admin@queless.in','ADMIN')");
            for (String s : new String[]{"Hospital","Bank","Government Office","Railway","Passport Office"})
                st.executeUpdate("INSERT IGNORE INTO service_counter(service_type) VALUES('" + s + "')");
        }
        System.out.println("✅ Database ready");
    }

    static Connection getCon() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // ========== UTILITIES ==========
    static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes); os.close();
    }

    static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), "UTF-8");
    }

    static String getParam(String query, String key) {
        if (query == null) return null;
        for (String p : query.split("&")) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }

    // Minimal JSON parser — handles flat {"key":"val","key2":"val2"}
    static Map<String,String> parseJson(String json) {
        Map<String,String> m = new HashMap<>();
        if (json == null || json.isBlank()) return m;
        json = json.trim().replaceAll("^\\{|\\}$", "");
        // split on , not inside quotes
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
            if (kv.length == 2) {
                String k = kv[0].trim().replaceAll("\"","");
                String v = kv[1].trim().replaceAll("^\"|\"$","");
                m.put(k, v);
            }
        }
        return m;
    }

    static String tokenToJson(ResultSet rs) throws SQLException {
        return String.format(
            "{\"id\":%d,\"tokenNo\":%d,\"serviceType\":\"%s\",\"name\":\"%s\",\"reason\":\"%s\",\"appointmentTime\":\"%s\",\"reportTime\":\"%s\",\"status\":\"%s\",\"userId\":%d}",
            rs.getInt("id"), rs.getInt("token_no"), esc(rs.getString("service_type")),
            esc(rs.getString("name")), esc(rs.getString("reason")),
            esc(rs.getString("appointment_time")), esc(rs.getString("report_time")),
            esc(rs.getString("status")), rs.getInt("user_id"));
    }

    static String esc(String s) { return s == null ? "" : s.replace("\\","\\\\").replace("\"","\\\""); }
}