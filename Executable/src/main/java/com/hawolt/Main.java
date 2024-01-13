package com.hawolt;

import com.hawolt.http.BasicHttp;
import com.hawolt.http.Request;
import com.hawolt.logger.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Main {
    public static final Path APPLICATION_CACHE = Paths.get(System.getProperty("java.io.tmpdir")).resolve("jamalong");
    private static final String PROJECT_RELEASES = "https://api.github.com/repos/hawolt/jamalong/releases?per_page=2";
    public static final String PROJECT = "JamAlong";

    public static void main(String[] args) {
        String filename = String.join(".", PROJECT, "jar");
        Path path = APPLICATION_CACHE.resolve(filename);
        Logger.debug("[updater] {}", path);
        try {
            if (!path.toFile().exists()) downloadLatestRelease(path);
            String java = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("java.exe").toString();
            ProcessBuilder builder = new ProcessBuilder(
                    java,
                    "-jar",
                    path.toString(),
                    args.length > 0 ? args[0] : ""
            );
            builder.start();
        } catch (IOException e) {
            Logger.error(e);
        }
        Logger.debug("[updater] restarting for new version");
        System.exit(1);
    }

    private static void downloadLatestRelease(Path target) throws IOException {
        Request request = new Request(PROJECT_RELEASES);
        JSONObject release = new JSONObject(request.execute().getBodyAsString());
        if (release.has("assets")) {
            JSONArray assets = release.getJSONArray("assets");
            if (assets.length() > 0) {
                JSONObject asset = assets.getJSONObject(0);
                if (asset.has("browser_download_url")) {
                    invokeSelfUpdate(target, asset.getString("browser_download_url"));
                }
            }
        }
    }

    private static void invokeSelfUpdate(Path path, String url) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "JamAlong");
        Logger.debug("[updater] invoke download: {}", url);
        byte[] b = BasicHttp.get(connection.getInputStream());
        Logger.debug("[updater] writing: {}", path);
        Files.createDirectories(path.getParent());
        Files.write(
                path,
                b,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }
}