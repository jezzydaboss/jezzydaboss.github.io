import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/***********************************************************
 *  MetricsServer
 *
 *  ENHANCEMENT (Milestone Three follow-up, CS 499): a real,
 *  network-reachable HTTP endpoint exposing live NameIndex and
 *  timing metrics, making the "Cross-Tier Application
 *  Connectivity" claim true rather than aspirational. Built on
 *  com.sun.net.httpserver.HttpServer, which ships in the JDK
 *  itself - no external dependencies (no Spring, no third-party
 *  HTTP library), which keeps this consistent with the rest of
 *  the artifact's dependency-free style.
 *
 *  Runs on its own daemon thread pool so it never blocks the
 *  console menu loop, and is explicitly stopped when the
 *  application quits.
 ***********************************************************/
public class MetricsServer {

    private final int port;
    private HttpServer server;
    private volatile String currentPayload = "{}";

    public MetricsServer(int port) {
        this.port = port;
    }

    public boolean start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/metrics", new MetricsHandler());
            server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "metrics-server");
                t.setDaemon(true); // ENHANCEMENT: daemon thread so it never
                                    // prevents the JVM from exiting on its own
                return t;
            }));
            server.start();
            System.out.println("MetricsServer: listening on http://localhost:" + port + "/metrics");
            return true;
        } catch (IOException e) {
            System.out.println("MetricsServer: ERROR - could not start on port " + port + ": " + e.getMessage());
            return false;
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public void updatePayload(String jsonPayload) {
        this.currentPayload = jsonPayload;
    }

    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = currentPayload; // volatile read - always the latest snapshot

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            // ENHANCEMENT: allows a browser dashboard served from a different
            // origin to fetch() this endpoint without being blocked by CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
