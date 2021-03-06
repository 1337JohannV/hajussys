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
        System.out.println("Enter catalogue server ip: ");
        this.server.catalogueServerIp = scanner.nextLine();
        if (server.isRunning()) {
            System.out.println(String.format("Server listening at port: %s", server.getPort()));
            System.out.println("Commands: current, update, help, download, stop.");
            System.out.println("Write help for explanation of commands.");
            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
            Runnable checkNodes = () -> getAddresses(server.catalogueServerIp);
            executorService.scheduleAtFixedRate(checkNodes, 0, 60, TimeUnit.SECONDS);
            String line;
            do {
                line = scanner.nextLine();
                if (line.equalsIgnoreCase("current")) {
                    System.out.println(server.addressList);
                }
                if (line.equalsIgnoreCase("update")) {
                    getAddresses(server.catalogueServerIp);
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

    private void getAddresses(String serverIp) {
        System.out.println("Updating Neighbours");
        Request request = new Request(String.format("http://%s:1215/addresses", serverIp), "get", null, server.serverAddress);
        HttpResponse<String> response = request.sendRequest();
        List<Address> addresses = gson.fromJson(response.body(), listType);
        this.server.addressList = addresses.stream()
                .filter(address -> !address.equals(this.server.serverAddress))
                .collect(Collectors.toList());
        System.out.print("Current Neighbours: ");
        System.out.println(this.server.addressList);
    }

    private void sendDownloadRequest(String url) {
        this.server.setCurrentFileName(url.replace(".", "_")
                .replace("%3A", "_")
                .replace("%2F%2F", "")
                .replace("%2F","_"));
        this.server.addressList.forEach(address -> {
            Request request = new Request(address.getHttpAddress(String.format("/download?id=%s&url=%s", this.server.currentId.toString(), url)),
                    "get", null, server.serverAddress);
            HttpResponse<String> response = request.sendRequest();
            if (response != null) {
                System.out.println("RESPONSE RECEIVED");
                this.server.setCurrentFileName("");
            } else {
                System.out.println("DOWNLOAD REQUEST FAILED");
                this.server.setCurrentFileName("");
            }
        });
    }
    private void commandExplanations(){
        System.out.println("current - Shows current list of neighbours.");
        System.out.println("update - Updates list of neighbours manually");
        System.out.println("download - Starts download html file of website. -> download URL");
        System.out.println("help - Shows explanation of commands.");
        System.out.println("stop - Stops this node.");
    }


    public static void main(String[] args) throws InterruptedException {
        Main main = new Main();
        main.startServer();
    }

}
