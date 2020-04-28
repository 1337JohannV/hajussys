package util;

import models.Address;

import java.net.*;

public class Ping {

    public static boolean isAddressReachable(Address address) {
        Socket socket = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(address.getIp(), address.getPort());
        try {
            socket.connect(socketAddress, 250);
            System.out.println(String.format("AVAILABLE: %s", address.getHttpAddress("")));
            return true;
        } catch (Exception e) {
            System.out.println(String.format("UNAVAILABLE: %s", address.getHttpAddress("")));
            return false;
        }

    }
}
