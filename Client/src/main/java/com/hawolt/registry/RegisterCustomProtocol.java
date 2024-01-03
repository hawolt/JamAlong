package com.hawolt.registry;

import com.hawolt.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class RegisterCustomProtocol {
    private static final String ADD_URL_PROTOCOL = "reg add HKEY_CLASSES_ROOT\\%s /v \"URL Protocol\" /f";
    private static final String QUERY_COMMAND = "REG QUERY HKEY_CLASSES_ROOT\\%s\\shell\\open\\command";
    private static final String QUERLY_URL = "REG QUERY HKEY_CLASSES_ROOT\\%s";

    public static void register(String customProtocol) throws URISyntaxException, RegistryPermissionException, IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) return;
        File file = Paths.get(javaHome).resolve("bin").resolve("java.exe").toFile();
        if (!file.exists()) return;
        String javaPath = file.getAbsolutePath();
        String jarPath = RegisterCustomProtocol.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
                .getPath()
                .substring(1)
                .replace("/", "\\");
        if (!jarPath.endsWith(".jar")) return;
        String command = new StringBuilder()
                .append("reg add HKEY_CLASSES_ROOT\\")
                .append(customProtocol)
                .append("\\shell\\open\\command /t REG_SZ /ve /d \"\"\"")
                .append(javaPath)
                .append("\"\" -jar \"\"")
                .append(jarPath)
                .append("\"\" \"\"stealth\"\" \"\"%1\"\"\" /f")
                .toString();
        String addShellPath = ProcessExecutor.execute(command, true, true);
        Logger.debug(addShellPath);
        String addUrlProtocol = ProcessExecutor.execute(String.format(ADD_URL_PROTOCOL, customProtocol), true, true);
        Logger.debug(addUrlProtocol);
    }

    public static boolean checkup(String path, String protocol) throws RegistryPermissionException, IOException, InterruptedException {
        String queryCommand = ProcessExecutor.execute(String.format(QUERY_COMMAND, protocol), true, true);
        String queryURL = ProcessExecutor.execute(String.format(QUERLY_URL, protocol), true, true);
        return queryCommand.contains(path) && queryURL.contains("URL Protocol");
    }
}
