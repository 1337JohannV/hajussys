package models;

import com.google.gson.Gson;

import java.util.Objects;

public class Address {
    private String ip;
    private int port;

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public Address(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof Address && Objects.equals(((Address) obj).ip, this.ip)
                && ((Address) obj).port == this.port);
    }

    public String getHttpAddress(String path) {
        return String.format("http://%s:%s%s", ip, port, path);
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
