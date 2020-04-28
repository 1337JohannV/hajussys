package util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import models.Address;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtil {

    public static List<Address> readFromAddressFile() {
        Gson gson = new Gson();
        List<Address> addresses;
        Type listType = new TypeToken<ArrayList<Address>>() {}.getType();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("./adresses.json"))) {
            String s = bufferedReader.lines().collect(Collectors.joining());
            addresses = gson.fromJson(s, listType);
            return addresses;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
