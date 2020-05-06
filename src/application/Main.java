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
import java.util.stream.Collectors;
import java.util.concurrent.*;

public class Main {

    private Server server;
    private Type listType = new TypeToken<ArrayList<Address>>() {
    }.getType();
    private Gson gson = new Gson();

    private void startServer() throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        server = new Server();
        if (server.isRunning()) {
            System.out.println(String.format("Server listening at port: %s", server.getPort()));
            System.out.println("Commands: current, update, help, download, stop.");
            System.out.println("Write help for explanation of commands.");
            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
            Runnable checkNodes = this::getAddresses;
            executorService.scheduleAtFixedRate(checkNodes, 0, 60, TimeUnit.SECONDS);
            String line;
            do {
                line = scanner.nextLine();
                if (line.equalsIgnoreCase("current")) {
                    System.out.println(server.addressList);
                }
                if (line.equalsIgnoreCase("update")) {
                    getAddresses();
                }
                if(line.equalsIgnoreCase("help")) {
                    commandExplanations();
                }
                if (line.startsWith("download ")) {
                    final String url = Encoder.encodeValue(line.split(" ")[1]);
                    CompletableFuture.runAsync(() -> {
                        this.server.currentId = UUID.randomUUID();
                        this.sendDownloadRequest(url);
                    });

                }
            } while (!Objects.equals(line, "stop"));
            server.stopServer();
            System.out.println("Stopping server...");
        }
    }

    private void getAddresses() {
        System.out.println("Updating Neighbours");
        Request request = new Request("http://localhost:1215/addresses", "get", null, server.serverAddress);
        HttpResponse<String> response = request.sendRequest();
        List<Address> addresses = gson.fromJson(response.body(), listType);
        this.server.addressList = Arrays.stream(addresses.toArray(new Address[0]))
                .filter(address -> !address.equals(this.server.serverAddress))
                .collect(Collectors.toList());
        System.out.print("Current Neighbours: ");
        System.out.println(this.server.addressList);
    }

    private void sendDownloadRequest(String url) {
        this.server.addressList.forEach(address -> {
            Request request = new Request(address.getHttpAddress(String.format("/download?id=%s&url=%s", this.server.currentId.toString(), url)),
                    "get", null, server.serverAddress);
            HttpResponse<String> response = request.sendRequest();
            if (response != null) {
                System.out.println("RESPONSE RECEIVED");
                System.out.println(response.body());
            } else {
                System.out.println("DOWNLOAD REQUEST FAILED");
            }
        });
    }
    private void commandExplanations(){
        System.out.println("current - Shows current list of neighbours.");
        System.out.println("update - Updates list of neighbours manually");
        System.out.println("download - Starts download file process, sends request to all neighbors.");
        System.out.println("help - Shows explanation of commands.");
        System.out.println("stop - Stops this node.");
    }


    public static void main(String[] args) throws InterruptedException {
        Main main = new Main();
        main.startServer();
    }

}
