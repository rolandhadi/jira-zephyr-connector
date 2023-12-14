/**
 * JiraZephyrConnector: HTTP Proxy Server for JIRA/Zephyr Scale Integration
 * Author: Roland Ross Hadi
 *
 * Description:
 * This Java program is designed to establish an HTTP server that acts as a proxy for integrating with a JIRA instance.
 * The server is configured to run on a customizable port and handles HTTP requests targeting JIRA's REST API endpoints.
 *
 * Key Features:
 * 1. Handles CORS (Cross-Origin Resource Sharing) preflight requests.
 * 2. Supports basic authentication for secure access to the JIRA server.
 * 3. Processes and relays both GET and POST requests to the JIRA server.
 * 4. Allows dynamic configuration through system properties for different deployment environments.
 * 5. Manages specific API endpoints (like test plans and test runs) using the ProxyHandler class.
 * 6. Ensures robust communication between the client and the JIRA server, including error handling.
 *
 * Usage:
 * The server can be configured and run with custom settings for port, JIRA URL, origin, and user credentials.
 * Suitable for scenarios requiring middleware solutions for secure, cross-domain communication with JIRA APIs.
 *
 * Note:
 * This implementation showcases the practical use of Java's networking capabilities for building middleware solutions.
 */

// Import necessary Java networking and I/O classes.
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;

// Define the main class, JiraZephyrConnector, for the HTTP server.
public class JiraZephyrConnector {

    // Define default values for server configuration.
    private static final int DEFAULT_PORT = 8383;
    private static final String DEFAULT_JIRA_URL = "http://localhost:8182";
    private static final String DEFAULT_ORIGIN = "http://localhost:8484";
    private static final String DEFAULT_JIRA_USERNAME = "admin";
    private static final String DEFAULT_JIRA_PASSWORD = "0000abc!";

    // The main method to start the HTTP server.
    public static void main(String[] args) throws IOException {
        // Retrieve server settings from system properties or use defaults.
        int serverPort = Integer.parseInt(System.getProperty("server.port", String.valueOf(DEFAULT_PORT)));
        String targetBaseUrl = System.getProperty("jira.url", DEFAULT_JIRA_URL);
        String allowedOrigin = System.getProperty("allowed.origin", DEFAULT_ORIGIN);
        String jiraUsername = System.getProperty("jira.username", DEFAULT_JIRA_USERNAME);
        String jiraPassword = System.getProperty("jira.password", DEFAULT_JIRA_PASSWORD);

        // Create and configure the HTTP server.
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        // Create contexts (endpoints) for the server with corresponding handlers.
        server.createContext("/rest/atm/1.0/testplan/", new ProxyHandler(targetBaseUrl, allowedOrigin, jiraUsername, jiraPassword));
        server.createContext("/rest/atm/1.0/testrun/search", new ProxyHandler(targetBaseUrl, allowedOrigin, jiraUsername, jiraPassword));
        server.createContext("/rest/atm/1.0/testrun", new ProxyHandler(targetBaseUrl, allowedOrigin, jiraUsername, jiraPassword));

        // Start the server.
        server.start();
        System.out.println("Server started on port " + serverPort + "...");
    }

    // Define the ProxyHandler class, which handles incoming HTTP requests.
    static class ProxyHandler implements HttpHandler {

        // Variables to hold configuration details.
        private final String targetBaseUrl;
        private final String allowedOrigin;
        private final String jiraUsername;
        private final String jiraPassword;

        // Constructor for the ProxyHandler.
        ProxyHandler(String targetBaseUrl, String allowedOrigin, String jiraUsername, String jiraPassword) {
            this.targetBaseUrl = targetBaseUrl;
            this.allowedOrigin = allowedOrigin;
            this.jiraUsername = jiraUsername;
            this.jiraPassword = jiraPassword;
        }

        // Method to handle incoming HTTP requests.
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Handle preflight (CORS) requests separately.
                if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                    handlePreflightRequest(exchange);
                    return;
                }

                // Build the target URL and create a connection to the JIRA server.
                String targetUrl = targetBaseUrl + exchange.getRequestURI().toString();
                HttpURLConnection conn = createConnection(targetUrl, exchange);

                // Handle POST requests by sending request body.
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendRequestBody(exchange, conn);
                }

                // Get the response from the target URL.
                int responseCode = conn.getResponseCode();
                InputStream is = getResponseStream(conn, responseCode);

                // Set response headers for the client and send the response.
                setResponseHeaders(exchange, conn);
                setCORSHeaders(exchange);
                sendResponseBody(exchange, is, responseCode);
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            } finally {
                exchange.close();
            }
        }

        // Method to handle preflight requests for CORS.
        private void handlePreflightRequest(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.set("Access-Control-Allow-Origin", allowedOrigin);
            headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.set("Access-Control-Allow-Headers", "Authorization, Content-Type, Cookie");
            headers.set("Access-Control-Allow-Credentials", "true");
            exchange.sendResponseHeaders(204, -1);
        }

        // Method to set CORS headers for the response.
        private void setCORSHeaders(HttpExchange exchange) {
            Headers headers = exchange.getResponseHeaders();
            headers.set("Access-Control-Allow-Origin", allowedOrigin);
            headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.set("Access-Control-Allow-Headers", "Authorization, Content-Type, Cookie");
            headers.set("Access-Control-Allow-Credentials", "true");
        }

        // Method to create a connection to the target URL.
        private HttpURLConnection createConnection(String targetUrl, HttpExchange exchange) throws IOException {
            HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
            conn.setRequestMethod(exchange.getRequestMethod());

            // Add basic authentication header for JIRA.
            String encodedCredentials = java.util.Base64.getEncoder().encodeToString((jiraUsername + ":" + jiraPassword).getBytes());
            conn.setRequestProperty("Authorization", "Basic " + encodedCredentials);

            // Copy other request headers from the client to the JIRA request.
            copyRequestHeaders(exchange, conn);

            return conn;
        }

        // Method to copy headers from the HTTP exchange to the HttpURLConnection.
        private static void copyRequestHeaders(HttpExchange exchange, HttpURLConnection conn) {
            Headers requestHeaders = exchange.getRequestHeaders();
            requestHeaders.forEach((key, values) -> values.forEach(value -> conn.addRequestProperty(key, value)));
        }

        // Method to send the request body for POST requests.
        private static void sendRequestBody(HttpExchange exchange, HttpURLConnection conn) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                conn.setDoOutput(true);
                try (InputStream is = exchange.getRequestBody();
                     OutputStream os = conn.getOutputStream()) {
                    is.transferTo(os);
                }
            }
        }

        // Method to get the response stream from the HttpURLConnection.
        private static InputStream getResponseStream(HttpURLConnection conn, int responseCode) throws IOException {
            return responseCode < HttpURLConnection.HTTP_BAD_REQUEST ? conn.getInputStream() : conn.getErrorStream();
        }

        // Method to set response headers for the client response.
        private static void setResponseHeaders(HttpExchange exchange, HttpURLConnection conn) {
            Headers responseHeaders = exchange.getResponseHeaders();
            conn.getHeaderFields().forEach((key, values) -> {
                if (key != null) {
                    responseHeaders.put(key, values);
                }
            });
        }

        // Method to send the response body to the client.
        private static void sendResponseBody(HttpExchange exchange, InputStream is, int responseCode) throws IOException {
            exchange.sendResponseHeaders(responseCode, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    os.write(buffer, 0, length);
                }
            }
        }
    }
}

// Instructions for compiling and running the server with specific configurations.
// javac -source 1.8 -target 1.8 JiraZephyrConnector.java
// java -Dserver.port=8383 -Djira.url=http://xxx.xxx.xxx.xxx -Dallowed.origin=http://xxx.xxx.xxx.xxx -Djira.username=username -Djira.password=password JiraZephyrConnector
