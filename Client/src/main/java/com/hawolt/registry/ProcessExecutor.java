package com.hawolt.registry;

import com.hawolt.logger.Logger;

import java.io.IOException;
import java.util.Scanner;

public class ProcessExecutor {
    public static String execute(String command, boolean await, boolean read) throws IOException, RegistryPermissionException, InterruptedException {
        Logger.debug("[process] {}", command);
        ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", command);
        processBuilder.redirectErrorStream(true);
        java.lang.Process process = processBuilder.start();
        if (await) process.waitFor();
        Scanner scanner = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
        String output = read ? scanner.hasNext() ? scanner.next() : "" : null;
        if (read) {
            Logger.debug("[registry] {}", output);
            if (output.contains("ERROR: Access is denied.")) {
                throw new RegistryPermissionException(command);
            }
        }
        return output;
    }
}