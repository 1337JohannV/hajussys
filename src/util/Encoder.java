package util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Encoder {
    public static String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
