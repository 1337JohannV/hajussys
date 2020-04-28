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
            getAddresses();
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
        Request request = new Request(String.format("http://localhost:1215/addresses?ip=%s&port=%s",
                this.server.ipAddress, this.server.port), "get", null);
        HttpResponse<String> response = request.sendRequest();
        System.out.println(response + "res");
        List<Address> addresses = gson.fromJson(response.body(), listType);
        this.server.addressList.clear();
        addresses.forEach(address -> addRequestAddress(this.server.addressList, address));
    }


    public static void main(String[] args) throws InterruptedException {
        Main main = new Main();
        main.startServer();
    }

}
