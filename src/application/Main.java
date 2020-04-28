package application;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import models.Address;
import request.Request;
import server.Server;
import util.Encoder;

import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private Server server;
    private Type listType = new TypeToken<ArrayList<Address>>() {
    }.getType();
    private Gson gson = new Gson();

    private void startServer() throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter port");
        int serverPort = scanner.nextInt();
        server = new Server(serverPort);
        if (server.isRunning()) {
            System.out.println(String.format("Server listening at port: %s", serverPort));
            System.out.println("Write stop to terminate");
            System.out.println("Commands: update_address, current_addresses");
            System.out.println("CHECKING AVAILABLE NODES...");
            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
            Runnable checkNodes = this::getAddresses;
            executorService.scheduleAtFixedRate(checkNodes, 0, 5, TimeUnit.SECONDS);
            String line;
            do {
                line = scanner.nextLine();
                if (line.equalsIgnoreCase("current")) {
                    System.out.println("calls from cmd");
                    System.out.println(server.addressList);
                }
                if (line.equalsIgnoreCase("update")) {
                    System.out.println("calls from cmd");
                    getAddresses();
                }
                if (line.equalsIgnoreCase("read")) {
                    System.out.println();

                }
                if (line.startsWith("download ")) {
                    String url = Encoder.encodeValue(line.split(" ")[1]);
                    this.server.currentId = UUID.randomUUID();
                    this.sendDownloadRequest(url);

                }
            } while (!Objects.equals(line, "stop"));
            server.stopServer();
            System.out.println("Stopping server...");
        }
    }

    private void addRequestAddress(List<Address> addresses, Address address) {
        if (addresses.contains(address) || address.equals(this.server.serverAddress)) {
            return;
        }

        addresses.add(address);
    }

    private void getAddresses() {
        Request request = new Request("http://localhost:1215/addresses", "get", null, server.serverAddress);
        HttpResponse<String> response = request.sendRequest();
        List<Address> addresses = gson.fromJson(response.body(), listType);
        this.server.addressList.clear();
        addresses.forEach(address -> addRequestAddress(this.server.addressList, address));
    }

    private void sendDownloadRequest(String url) {
        this.server.addressList.forEach(address -> {
            Request request = new Request(address.getHttpAddress(String.format("/download?id=%s&url=%s", this.server.currentId.toString(), url)),
                    "get", null, server.serverAddress);
            HttpResponse<String> response = request.sendRequest();
            System.out.println(response);
        });
    }


    public static void main(String[] args) throws InterruptedException {
        Main main = new Main();
        main.startServer();
    }

}
