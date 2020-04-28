package request;


import models.Address;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


public class Request {

    private HttpClient httpClient;
    private HttpRequest httpRequest;

    public Request(String address, String method, String content, Address forwardedFor) {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        if (method.equalsIgnoreCase("post")) {
            httpRequest = HttpRequest.newBuilder()
                    .header("Content-type", "application/json")
                    .header("X-Forwarded-For", forwardedFor.toString())
                    .uri(URI.create(address))
                    .POST(HttpRequest.BodyPublishers.ofString(content))
                    .timeout(Duration.ofSeconds(5))
                    .build();
        } else {
            httpRequest = HttpRequest.newBuilder()
                    .header("Content-type", "application/json")
                    .header("X-Forwarded-For", forwardedFor.toString())
                    .uri(URI.create(address))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
        }
    }

    public HttpResponse<String> sendRequest() {
        try {
            return this.httpClient.send(this.httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            return null;
        }
    }


}
