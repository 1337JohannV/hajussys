package application;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import models.Address;
import request.Request;
import server.Server;
import util.Ping;

import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

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
            updateAddresses();
            String line;
            do {
                line = scanner.nextLine();
                if (line.equalsIgnoreCase("current_addresses")) {
                    System.out.println("calls from cmd");
                    System.out.println(server.addressList);
                }
                if (line.equalsIgnoreCase("update_address")) {
                    System.out.println("calls from cmd");
                    updateAddresses();
                }
                if (line.equalsIgnoreCase("read")) {
                    System.out.println();

                }
            } while (!Objects.equals(line, "stop"));
            server.stopServer();
            System.out.println("Stopping server...");
        }
    }

    private void addRequestAddress(List<Address> addresses, Address address) {
        if (addresses.contains(address) || address == this.server.serverAddress) {
            return;
        }
        addresses.add(address);
    }

    private void updateAddresses() throws InterruptedException {
        this.server.addressList.remove(this.server.serverAddress);
        List<Address> updatedAddresses = new ArrayList<>();
        this.server.addressList.parallelStream().forEach(address -> {
            if (Ping.isAddressReachable(address)) {
                updatedAddresses.add(address);
                try {
                    Request request = new Request(address.getHttpAddress(String.format("/addr?ip=%s&port=%s",
                            this.server.ipAddress, this.server.port)), "get", null);
                    HttpResponse<String> response = request.sendRequest();
                    List<Address> addresses = gson.fromJson(response.body(), listType);
                    addresses.forEach(ads -> addRequestAddress(updatedAddresses, ads));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
        this.server.addressList = updatedAddresses;
    }

    public static void main(String[] args) throws InterruptedException {
        Main main = new Main();
        main.startServer();
    }

}
