package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import models.Address;
import models.Path;
import request.Request;
import util.Encoder;
import util.Response;

import java.net.BindException;
import java.util.*;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;


public class Server {

    private HttpServer server;
    public List<Address> addressList = new ArrayList<>();
    public List<Path> pathList = new ArrayList<>();
    public int port;
    public String ipAddress;
    public Address serverAddress;
    private double laziness = 0.5d;
    public UUID currentId;
    public String catalogueServerIp;


    public Server() {
        while (this.server == null) {
            try {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Enter port: ");
                this.port = scanner.nextInt();
                server = HttpServer.create(new InetSocketAddress(getPort()), 0);
                server.createContext("/", new RequestHandler(this));
                server.setExecutor(null);
                this.serverAddress = new Address(getIpAddress(), getPort());
                this.ipAddress = getIpAddress();
                server.start();
            } catch (BindException e) {
                System.out.println("Port already in use! Enter a different port.");
            } catch (InputMismatchException e) {
                System.out.println("Port must be a number! Enter a number.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void stopServer() {
        this.server.stop(0);
        this.server = null;
    }

    public int getPort() {
        return port;
    }

    private static String getIpAddress() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    public boolean isRunning() {
        return this.server != null;
    }


    static class RequestHandler implements HttpHandler {
        private Server server;
        private Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        private String response;
        private Address requestAddress;

        RequestHandler(Server server) {
            this.server = server;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String> requestAddresses = exchange.getRequestHeaders().get("X-FORWARDED-FOR");
            if (requestAddresses != null && requestAddresses.size() != 0) {
                requestAddress = gson.fromJson(requestAddresses.get(0), Address.class);
            } else {
                requestAddress = new Address(exchange.getRemoteAddress()
                        .getAddress()
                        .toString()
                        .split("/")[1], 1215);
            }
            if (exchange.getRequestMethod().equalsIgnoreCase("get")) {
                if ("/download".equals(exchange.getRequestURI().getPath())) {
                    downloadRequest(exchange);
                }
            } else if (exchange.getRequestMethod().equalsIgnoreCase("post")) {
                if ("/file".equals(exchange.getRequestURI().getPath())) {
                    fileRequest(exchange);
                }
            } else {
                unsupportedRequest(exchange);
            }
        }

        private void downloadRequest(HttpExchange exchange) throws  IOException {
            String fileUrl = getQueryStrings(exchange.getRequestURI().getQuery()).get("url");
            String fileId = getQueryStrings(exchange.getRequestURI().getQuery()).get("id");
            System.out.println(this.server.pathList);
            Random r = new Random();
            double randomValue = 1 * r.nextDouble();
            if (this.server.currentId != null && this.server.currentId.toString().equals(fileId)) {
                System.out.println("IGNORES DOWNLOAD, THIS SERVER INITIATED DOWNLOAD REQUEST");
                this.response = new Response(200, null, null).toString();
                exchange.sendResponseHeaders(200, this.response.getBytes().length);
            } else if (this.server.pathList.stream().anyMatch(p -> p.getId().equals(fileId) && p.getDownload() != null)) {
                System.out.println("IGNORES DOWNLOAD, THIS SERVER ALREADY RESPONDED TO IT");
                this.response = new Response(200, null, null).toString();
                exchange.sendResponseHeaders(200, this.response.getBytes().length);
            } else if (randomValue > this.server.laziness) {
                System.out.println("DOWNLOADS FILE");
                this.addPath(new Path(fileId, requestAddress, null));
                Request req = new Request(fileUrl, "get", null, null);
                HttpResponse<InputStream> response = req.sendRequest(true);
                if (response != null) {
                    byte[] data = response.body().readAllBytes();
                    String mimeType = response.headers().firstValue("Content-Type").orElse(null);
                    final String mimeTypeFinal = mimeType != null ? mimeType.split(";")[0] : null;
                    String encodedFile = Base64.getEncoder().encodeToString(data);
                    this.response = new Response(200, null, null).toString();
                    exchange.sendResponseHeaders(200, this.response.getBytes().length);
                    Optional<Path> optionalPath = this.server.pathList.stream().filter(p -> p.getId().equals(fileId)).findAny();
                    if (optionalPath.isPresent() && optionalPath.get().getDownload() != null) {
                        System.out.println("SENDS REQUEST TO ONE IN PATH TABLE");
                        sendFileRequest(optionalPath.get().getDownload(), fileId, new Response(200, mimeTypeFinal, encodedFile));
                    } else {
                        System.out.println("SENDS REQUEST TO ALL");
                        this.server.addressList.forEach(address
                                -> sendFileRequest(address, fileId, new Response(200, mimeTypeFinal, encodedFile)));
                    }
                } else {
                    this.response = new Response(500, null, null).toString();
                    exchange.sendResponseHeaders(500, this.response.getBytes().length);

                }
            } else {
                System.out.println("DOES NOT DOWNLOAD FILE");
                this.response = new Response(200, null, null).toString();
                exchange.sendResponseHeaders(200, response.getBytes().length);
                List<Address> availableAddresses = this.server.addressList
                        .stream().filter(ad -> !ad.equals(this.requestAddress)).collect(Collectors.toList());
                availableAddresses.forEach(address -> sendDownloadRequest(address, String.format("/download?id=%s&url=%s", fileId, fileUrl)));
            }
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }

        private void fileRequest(HttpExchange exchange) throws IOException {
            InputStream inputStream = exchange.getRequestBody();
            OutputStream outputStream;
            String fileId = getQueryStrings(exchange.getRequestURI().getQuery()).get("id");
            String requestBody = getRequestBody(inputStream);
            if (this.server.currentId != null && this.server.currentId.toString().equals(fileId)) {
                System.out.println("FILE RECEIVED");
                Encoder.decodeToFile(gson.fromJson(requestBody, Response.class), fileId);
                this.response = new Response(200, null, null).toString();
            } else if (this.server.pathList.stream().anyMatch(p -> p.getId().equals(fileId) && p.getFile() != null)) {
                System.out.println("IGNORES SEND FILE REQUEST");
                this.response = new Response(200, null, null).toString();
                exchange.sendResponseHeaders(200, response.getBytes().length);
            } else {
                this.addPath(new Path(fileId, null, this.requestAddress));
                this.response = new Response(200, null, null).toString();
                Optional<Path> optionalPath = this.server.pathList.stream().filter(p -> p.getId().equals(fileId)).findAny();
                if (optionalPath.isPresent() && optionalPath.get().getDownload() != null) {
                    System.out.println("SENDS REQUEST TO ONE IN PATH TABLE");
                    sendFileRequest(optionalPath.get().getDownload(), fileId, requestBody);
                } else {
                    System.out.println("SENDS REQUEST TO ALL");
                    this.server.addressList.forEach(address
                            -> sendFileRequest(address, fileId, requestBody));
                }
            }
            exchange.sendResponseHeaders(200, response.getBytes().length);
            outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }

        private void unsupportedRequest(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(404, response.getBytes().length);
            OutputStream outputStream = exchange.getResponseBody();
            this.response = "{\"status\": 404}";
            outputStream.write(response.getBytes());
            outputStream.close();
        }

        private void sendDownloadRequest(Address address, String query) {
            Request request = new Request(address.getHttpAddress(query), "get", null, this.server.serverAddress);
            request.sendRequest();
        }

        private void sendFileRequest(Address address, String fileId, Response response) {
            Request request = new Request(address.getHttpAddress(String.format("/file?id=%s", fileId)),
                    "post", response.toString(), this.server.serverAddress);
            request.sendRequest();

        }

        private void sendFileRequest(Address address, String fileId, String response) {
            Request request = new Request(address.getHttpAddress(String.format("/file?id=%s", fileId)),
                    "post", response, this.server.serverAddress);
            request.sendRequest();
        }

        private void addPath(Path path) {
            List<Path> paths = this.server.pathList.stream().filter(p -> p.getId().equals(path.getId())).collect(Collectors.toList());
            if (paths.isEmpty()) {
                this.server.pathList.add(path);
            } else {
                paths.forEach(singlePath -> {
                    if (singlePath.getDownload() == null && path.getDownload() != null) {
                        singlePath.setDownload(path.getDownload());
                    }
                    if (singlePath.getFile() == null && path.getFile() != null) {
                        singlePath.setFile(path.getFile());
                    }
                });
            }
        }


        private String getRequestBody(InputStream inputStream) throws IOException {
            int b;
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader bufferReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            while ((b = bufferReader.read()) != -1) {
                stringBuilder.append((char) b);
            }
            bufferReader.close();
            inputStreamReader.close();
            return stringBuilder.toString();
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
