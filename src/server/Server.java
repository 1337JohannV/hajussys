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

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


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
        private Gson gson= new GsonBuilder().disableHtmlEscaping().create();
        private String response;
        private Address requestAddress;

        RequestHandler(Server server) {
            this.server = server;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            final String path = exchange.getRequestURI().getPath();
            server.currentQueries = getQueryStrings(exchange.getRequestURI().getQuery());
            final List<String> requestAddresses = exchange.getRequestHeaders().get("X-FORWARDED-FOR");
            if (requestAddresses != null && requestAddresses.size() != 0) {
                requestAddresses.forEach(ad -> requestAddress = gson.fromJson(ad, Address.class));
            } else {
                final String requestIp = exchange.getRemoteAddress().getAddress().toString().split("/")[1];
                requestAddress = new Address(requestIp, 1215);
            }
            System.out.println("REQUEST ADDRESS:" + requestAddress);
            if (exchange.getRequestMethod().equalsIgnoreCase("get")) {
                if ("/download".equals(path)) {
                    String fileUrl = server.currentQueries.get("url");
                    String fileId = server.currentQueries.get("id");
                    System.out.println(this.server.pathList);
                    Random r = new Random();
                    double randomValue = 1 * r.nextDouble();
                    if (this.server.currentId != null && this.server.currentId.toString().equals(fileId)) {
                        System.out.println("IGNORES DOWNLOAD, THIS SERVER INITIATED DOWNLOAD REQUEST");
                        this.response = new Response(200, null, null).toString();
                        exchange.sendResponseHeaders(200, this.response.getBytes().length);
                    } else if (this.server.pathList.stream().anyMatch(p-> p.getId().equals(fileId) && p.getDownload() != null)) {
                        System.out.println("IGNORES DOWNLOAD, THIS SERVER ALREADY RESPONDED TO IT");
                        this.response = new Response(200, null, null).toString();
                        exchange.sendResponseHeaders(200, this.response.getBytes().length);
                    }
                    else if (randomValue > this.server.laziness) {
                        System.out.println("DOWNLOADS FILE");
                        this.addPath(new Path(fileId, requestAddress, null));
                        Request req = new Request(fileUrl, "get", null, null);
                        HttpResponse response = req.sendRequest();
                        if (response != null) {
                            String mimeType = response.headers().firstValue("Content-Type").orElse(null);
                            final String mimeTypeFinal = mimeType != null ? mimeType.split(";")[0] : null;
                            String encodedFile = Base64.getEncoder().encodeToString(response.body().toString().getBytes(StandardCharsets.UTF_8));
                            this.response = new Response(200, null, null).toString();
                            exchange.sendResponseHeaders(200, this.response.getBytes().length);
                            Optional<Path> optionalPath = this.server.pathList.stream().filter(p -> p.getId().equals(fileId)).findAny();
                            if (optionalPath.isPresent()) {
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

            } else if (exchange.getRequestMethod().equalsIgnoreCase("post")) {
                InputStream inputStream;
                OutputStream outputStream;
                if ("/file".equals(path)) {
                    String fileId = this.server.currentQueries.get("id");
                    inputStream = exchange.getRequestBody();
                    String requestBody = getRequestBody(inputStream);
                    this.addPath(new Path(fileId, null, this.requestAddress));
                    if (this.server.currentId != null && this.server.currentId.toString().equals(fileId)) {
                        System.out.println("FILE RECEIVED");
                        System.out.println(requestBody);
                        Encoder.decodeToFile(gson.fromJson(requestBody, Response.class), fileId);
                        this.response = new Response(200, null, null).toString();
                    } else {
                        this.response = new Response(200, null, null).toString();
                        Optional<Path> optionalPath = this.server.pathList.stream().filter(p-> p.getId().equals(fileId)).findAny();
                        if (optionalPath.isPresent()) {
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
            Optional<Path> k = this.server.pathList.stream().filter(p -> p.getId().equals(path.getId())).findAny();
            if (k.isEmpty()) {
                this.server.pathList.add(path);
            } else {
                this.server.pathList.stream().filter(p -> p.getId().equals(path.getId())).forEach(x -> {
                    if (x.getDownload() == null && path.getDownload() != null) {
                        x.setDownload(path.getDownload());
                    }
                    if (x.getFile() == null && path.getFile() != null) {
                        x.setFile(path.getFile());
                    }
                });
            }
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
