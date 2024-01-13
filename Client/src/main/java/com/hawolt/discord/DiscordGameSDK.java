package com.hawolt.discord;

import com.hawolt.misc.StaticConstant;
import com.hawolt.io.Core;
import com.hawolt.os.OperatingSystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class to download and cache the Discord Game SDK .
 */

public class DiscordGameSDK {
    private static final long BYTE_COUNT = 22808634L;

    public static File loadFromCacheOrDownload() throws IOException {
        String libraryBaseName = "discord_game_sdk";
        String suffix = switch (OperatingSystem.getOperatingSystemType()) {
            case WINDOWS -> ".dll";
            case MAC -> ".dylib";
            case LINUX -> ".so";
            case UNKNOWN -> throw new RuntimeException("Unable to determine OSType");
        };
        String operatingSystemArchitecture = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        if (operatingSystemArchitecture.equals("amd64")) {
            operatingSystemArchitecture = "x86_64";
        }
        String pathWithinArchive = "lib/" + operatingSystemArchitecture + "/" + libraryBaseName + suffix;
        Path path = StaticConstant.APPLICATION_CACHE;
        Path sdk = path.resolve("discord_game_sdk.zip");
        Files.createDirectories(path);
        ByteArrayOutputStream byteArrayOutputStream;
        ZipInputStream archive = null;
        boolean corrupt = false;
        if (sdk.toFile().exists()) {
            byte[] b = Files.readAllBytes(sdk);
            archive = new ZipInputStream(new ByteArrayInputStream(b));
            corrupt = b.length != BYTE_COUNT;
        }
        if (corrupt || !sdk.toFile().exists()) {
            URL downloadUrl = new URL("https://dl-game-sdk.discordapp.net/2.5.6/discord_game_sdk.zip");
            HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
            connection.setRequestProperty("User-Agent", "discord-game-sdk4j");
            byteArrayOutputStream = Core.read(connection.getInputStream());
            Files.write(
                    sdk,
                    byteArrayOutputStream.toByteArray(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            archive = new ZipInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        }
        ZipEntry entry;
        while ((entry = archive.getNextEntry()) != null) {
            if (entry.getName().equals(pathWithinArchive)) {
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "java-" + libraryBaseName + System.nanoTime());
                if (!tempDir.mkdir()) throw new IOException("Cannot create temporary directory");
                tempDir.deleteOnExit();
                File tmp = new File(tempDir, libraryBaseName + suffix);
                tmp.deleteOnExit();
                Files.copy(archive, tmp.toPath());
                archive.close();
                return tmp;
            }
            archive.closeEntry();
        }
        archive.close();
        throw new IOException("Unable to locate the required files");
    }
}
