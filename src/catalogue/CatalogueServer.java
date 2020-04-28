package catalogue;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import models.Address;
import com.sun.net.httpserver.HttpHandler;
import util.Ping;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CatalogueServer {

    private HttpServer server;
    public List<Address> addressList = new ArrayList<>();
    private Map<String, String> currentQueries = new HashMap<>();

    public CatalogueServer() throws IOException {

        server = HttpServer.create(new InetSocketAddress(1215), 0);
        server.createContext("/", new RequestHandler(this));
        server.setExecutor(null);
        server.start();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        Runnable checkNodes = () -> {
            System.out.println("Updating node list");
            List<Address> updatedList = new ArrayList<>();
            addressList.forEach(address -> {
                if (Ping.isAddressReachable(address)) {
                    updatedList.add(address);
                }
            });
            addressList = updatedList;
            System.out.println("Addresses: " + addressList);
        };
        executorService.scheduleAtFixedRate(checkNodes,0, 5, TimeUnit.SECONDS);
    }

    private class RequestHandler implements HttpHandler {
        private CatalogueServer server;
        private String response;
        private Gson gson = new Gson();

        RequestHandler(CatalogueServer cs) {
            server = cs;
        }


        @Override
        public void handle(HttpExchange exchange) throws IOException {
            final String path = exchange.getRequestURI().getPath();
            System.out.println("REQ RECEIVED");
            if (exchange.getRequestURI().getQuery() != null) {
                this.server.currentQueries = getQueryStrings(exchange.getRequestURI().getQuery());
                this.addRequestAddress(this.server.addressList, new Address(server.currentQueries.get("ip"),
                        Integer.parseInt(server.currentQueries.get("port"))));
            }

            if (path.equals("/addresses")) {
                this.response = gson.toJson(this.server.addressList);
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();

            }


        }

        private void addRequestAddress(List<Address> addresses, Address address) {
            if (addresses.contains(address)) {
                return;
            }
            addresses.add(address);
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

    public static void main(String[] args) throws IOException {
        CatalogueServer server = new CatalogueServer();
    }
}