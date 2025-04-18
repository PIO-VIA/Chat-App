package org.personnal.serveur.utils;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class NetworkUtils {

    public static String encodeFileToBase64(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] decodeBase64ToFile(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    public static void saveBytesToFile(byte[] data, String destinationPath) throws IOException {
        Files.write(new File(destinationPath).toPath(), data);
    }
}

