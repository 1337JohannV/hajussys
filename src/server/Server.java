package server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import models.Address;
import models.Path;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class Server {

    private HttpServer server;
    public List<Address> addressList = new ArrayList<>();
    public List<Path> pathList = new ArrayList<>();
    public int port;
    public String ipAddress;
    public Address serverAddress;
    private Map<String, String> currentQueries = new HashMap<>();
    private double laziness = 0.5d;
    public UUID currentId;


    public Server(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new RequestHandler(this));
            server.setExecutor(null);
            this.serverAddress = new Address(getIpAddress(), port);
            this.port = port;
            this.ipAddress = getIpAddress();
            server.start();
        } catch (Exception e) {
            this.server = null;
            System.out.println("Port already in use");
        }
    }

    public void stopServer() {
        this.server.stop(0);
        this.server = null;
    }

    private static String getIpAddress() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    public boolean isRunning() {
        return this.server != null;
    }


    static class RequestHandler implements HttpHandler {

        private Server server;
        private Gson gson = new Gson();
        private String response;

        RequestHandler(Server server) {
            this.server = server;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            final String path = exchange.getRequestURI().getPath();
            server.currentQueries = getQueryStrings(exchange.getRequestURI().getQuery());

            if (exchange.getRequestMethod().equalsIgnoreCase("get")) {
                switch (path) {
                    case "/download":
                        Random r = new Random();
                        double randomValue = 1 * r.nextDouble();
                        System.out.println(exchange.getRequestHeaders().get("X-FORWARDED-FOR"));
                        System.out.println(exchange.getRequestURI() + "URI");
                        System.out.println(randomValue + "random");
                        this.response = "DOWNLOAD";
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                        OutputStream outputStream = exchange.getResponseBody();
                        outputStream.write(response.getBytes());
                        outputStream.close();
                    case "/getblocks":

                }

            } else if (exchange.getRequestMethod().equalsIgnoreCase("post")) {
                InputStream inputStream;
                OutputStream outputStream;
                switch(path) {
                    case "/inv":
                        inputStream = exchange.getRequestBody();
                        this.response = "1";
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                        outputStream = exchange.getResponseBody();
                        outputStream.write(response.getBytes());
                        outputStream.close();
                    case "/file":
                        /*
                        inputStream = exchange.getRequestBody();
                        this.response = "1";
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                        outputStream = exchange.getResponseBody();
                        outputStream.write(response.getBytes());
                        outputStream.close();
                        */
                }
            } else {
                exchange.sendResponseHeaders(404, response.getBytes().length);
                OutputStream outputStream = exchange.getResponseBody();
                this.response = "{\"status\": 404}";
                outputStream.write(response.getBytes());
                outputStream.close();
            }
        }

        private boolean isCorrectAddress() {
            return true;
        }

        private void addRequestAddress(List<Address> addresses, Address address) {
            if (addresses.contains(address)) {
                System.out.println("REQUEST ADDRESS ALREADY PRESENT");
                return;
            }
            addresses.add(address);
        }

        private String getRequestBody(InputStream inputStream) throws IOException {

            InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            int b;
            StringBuilder sb = new StringBuilder(512);
            while ((b = br.read()) != -1) {
                sb.append((char) b);
            }
            br.close();
            isr.close();
            return sb.toString();
        }

        private Map<String, String> getQueryStrings(String query) {
            if (query == null) {
                return Collections.emptyMap();
            }
            Map<String, String> result = new HashMap<>();
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], entry[1]);
                } else {
                    result.put(entry[0], "");
                }
            }
            return result;
        }


    }

}
