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
    Address toAdd;

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
        executorService.scheduleAtFixedRate(checkNodes, 0, 5, TimeUnit.SECONDS);
    }

    private class RequestHandler implements HttpHandler {
        private CatalogueServer server;
        private Gson gson = new Gson();

        RequestHandler(CatalogueServer cs) {
            server = cs;
        }


        @Override
        public void handle(HttpExchange exchange) throws IOException {
            final String path = exchange.getRequestURI().getPath();
            System.out.println("REQ RECEIVED");
            exchange.getRequestHeaders().get("X-FORWARDED-FOR").forEach(str -> {
                System.out.println(str);
                toAdd = gson.fromJson(str, Address.class);
            });
            this.addRequestAddress(this.server.addressList, toAdd);
            if (path.equals("/addresses")) {
                String response = gson.toJson(this.server.addressList);
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

    }

    public static void main(String[] args) throws IOException {
        CatalogueServer server = new CatalogueServer();
    }
}