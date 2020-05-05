package util;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class Encoder {
    public static String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
    //TODO: Fix saving images etc
    public static void decodeToFile(Response response, String fileId) throws IOException {
        String fileFormat = response.getMimeType() != null ? response.getMimeType().split("/")[1] : null;
        byte[] data = Base64.getDecoder().decode(response.getContent().getBytes(StandardCharsets.UTF_8));
        Path destination = Paths.get(".\\downloaded-files", String.format("%s.%s",fileId, fileFormat));
        Files.write(destination, data);
    }
}
