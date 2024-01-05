package com.hawolt.chromium;

import com.hawolt.*;
import com.hawolt.audio.SystemAudio;
import com.hawolt.common.Pair;
import com.hawolt.discord.RichPresence;
import com.hawolt.http.BasicHttp;
import com.hawolt.http.Request;
import com.hawolt.http.Response;
import com.hawolt.http.misc.DownloadCallback;
import com.hawolt.logger.Logger;
import com.hawolt.misc.Debouncer;
import com.hawolt.settings.SettingManager;
import com.hawolt.source.Audio;
import com.hawolt.source.AudioSource;
import com.hawolt.source.StreamUpdateListener;
import com.hawolt.source.impl.AbstractAudioSource;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;


/**
 * Created: 31/07/2022 00:57
 * Author: Twitter @hawolt
 **/

public class LocalExecutor {
    public static HostType HOST_TYPE = HostType.UNKNOWN;
    public static String PARTY_ID;

    public static long RESET_TIMESTAMP;

    public static void configure(SettingManager manager, int websocketPort, PlaybackHandler playbackHandler, AbstractAudioSource source, RemoteClient remoteClient) {
        path("/v1", () -> {
            path("/config", () -> {
                get("/invoke", SELF_UPDATE);
                get("/version", UPDATE_CHECK);
                get("/skip", context -> playbackHandler.skip());
                get("/websocket", context -> context.result(String.valueOf(websocketPort)));
                get("/reset", new ContextBiConsumer<>(Pair.of(remoteClient, playbackHandler), RESET));
                get("/gain", context -> context.result(String.valueOf(manager.getClientSettings().getClientVolumeGain())));
                get("/gain/{value}", new ContextBiConsumer<>(manager, GAIN));
            });
            path("/api", () -> {
                get("/load", new ContextBiConsumer<>(source, LOAD));
                get("/discover", new ContextBiConsumer<>(remoteClient, DISCOVER));
                get("/host", new ContextBiConsumer<>(Pair.of(remoteClient, playbackHandler), HOST));
                get("/namechange/{name}/{partyId}", new ContextBiConsumer<>(Pair.of(remoteClient, manager), NAMECHANGE));
                get("/visibility/{partyId}/{status}", new ContextBiConsumer<>(remoteClient, VISIBILITY));
                get("/join/{partyId}", new ContextBiConsumer<>(Pair.of(remoteClient, playbackHandler), JOIN));
            });
        });
    }

    private static BiConsumer<Context, AbstractAudioSource> LOAD = (context, source) -> {
        String url = context.queryParam("url");
        if (url == null) context.result("NO_URL");
        else {
            String plain = new String(Base64.getDecoder().decode(url.getBytes()));
            source.load(plain);
            context.result(plain);
        }
    };

    private static BiConsumer<Context, Pair<RemoteClient, PlaybackHandler>> HOST = (context, pair) -> {
        LocalExecutor.HOST_TYPE = HostType.HOST;
        RemoteClient remoteClient = pair.getK();
        PlaybackHandler playbackHandler = pair.getV();
        JSONObject object = remoteClient.executeBlocking("create");
        LocalExecutor.PARTY_ID = object.getString("result").split(" ")[0];
        Main.presence.ifPresent(presence -> presence.set(LocalExecutor.PARTY_ID));
        context.result(object.toString());
        playbackHandler.addStreamUpdateListener(new StreamUpdateListener() {
            @Override
            public void onAudioUpdate(Audio audio, long timestamp) {
                remoteClient.executeAsynchronous("revalidate", object -> {
                    String result = object.getString("result");
                    if ("UNKNOWN_ROOM".equals(result)) {
                        JSONObject instruction = new JSONObject();
                        instruction.put("instruction", "kill");
                        SocketServer.forward(instruction.toString());
                        playbackHandler.reset();
                    }
                    Logger.debug("revalidated:{}", result.equals(LocalExecutor.PARTY_ID));
                }, LocalExecutor.PARTY_ID, String.valueOf(timestamp), audio.source());
            }

            @Override
            public void onAudioPeekUpdate(Audio audio) {
                remoteClient.executeAsynchronous("preload", object -> {
                    boolean success = object.getString("result").equals(LocalExecutor.PARTY_ID);
                    Logger.debug("preload:{}", success);
                }, LocalExecutor.PARTY_ID, audio.source());
            }
        });
    };

    public static JSONObject join(RemoteClient remoteClient, PlaybackHandler playbackHandler, String partyId) {
        LocalExecutor.HOST_TYPE = HostType.ATTENDEE;
        JSONObject object = remoteClient.executeBlocking("join", partyId);
        String[] arguments = object.getString("result").split(" ");
        if (arguments.length > 0) {
            LocalExecutor.PARTY_ID = arguments[0];
            Main.presence.ifPresent(presence -> presence.set(LocalExecutor.PARTY_ID));
            playbackHandler.clearStreamUpdateListeners();
            if (arguments.length > 2) {
                playbackHandler.setTimestamp(Long.parseLong(arguments[1]));
                AudioSource source = playbackHandler.getAudioSource();
                source.load(arguments[2]);
                if (arguments.length > 3) {
                    source.preload(arguments[3]);
                }
            }
        }
        return object;
    }

    private static BiConsumer<Context, Pair<RemoteClient, PlaybackHandler>> JOIN = (context, pair) -> {
        RemoteClient remoteClient = pair.getK();
        PlaybackHandler playbackHandler = pair.getV();
        String partyId = context.pathParam("partyId");
        context.result(join(remoteClient, playbackHandler, partyId).toString());
    };

    private static BiConsumer<Context, Pair<RemoteClient, SettingManager>> NAMECHANGE = (context, pair) -> {
        RemoteClient remoteClient = pair.getK();
        SettingManager manager = pair.getV();
        String name = context.pathParam("name");
        JSONObject object = remoteClient.executeBlocking("name", context.pathParam("partyId"), name);
        if (object.getString("result").equals(name)) {
            manager.write("name", name);
        }
        context.result(object.toString());
    };
    private static BiConsumer<Context, RemoteClient> DISCOVER = (context, remoteClient) -> {
        Main.presence.ifPresent(presence -> presence.setIdlePresence());
        JSONObject object = remoteClient.executeBlocking("discover");
        context.result(object.toString());
    };
    private static BiConsumer<Context, Pair<RemoteClient, PlaybackHandler>> RESET = (context, pair) -> {
        LocalExecutor.RESET_TIMESTAMP = System.currentTimeMillis();
        RemoteClient remoteClient = pair.getK();
        JSONObject object = remoteClient.executeBlocking("leave", LocalExecutor.PARTY_ID == null ? "nil" : LocalExecutor.PARTY_ID);
        PlaybackHandler playbackHandler = pair.getV();
        playbackHandler.reset();
        context.result(object.toString());
    };
    private static BiConsumer<Context, RemoteClient> VISIBILITY = (context, remoteClient) -> {
        JSONObject object = remoteClient.executeBlocking("visibility", context.pathParam("partyId"), context.pathParam("status"));
        context.result(object.toString());
    };

    private static BiConsumer<Context, SettingManager> GAIN = (context, manager) -> {
        float gain = Float.parseFloat(context.pathParam("value"));
        SystemAudio.setGain(gain);
        Main.debouncer.debounce("gain", () -> {
            manager.write("gain", gain);
        }, 200, TimeUnit.MILLISECONDS);
    };

    private static JSONObject release;

    private static void fetchLatestRelease() throws IOException {
        Request request = new Request(StaticConstant.PROJECT_RELEASES);
        LocalExecutor.release = new JSONObject(request.execute().getBodyAsString());
    }

    private static Handler UPDATE_CHECK = context -> {
        if (release == null) fetchLatestRelease();
        if (!release.has("tag_name")) {
            context.result("false");
        } else {
            String tag = release.getString("tag_name");
            if (!Main.version.equals(tag) && release.has("assets")) {
                context.result("true");
            } else {
                context.result("false");
            }
        }
    };
    private static Handler SELF_UPDATE = context -> {
        if (release == null) fetchLatestRelease();
        if (release.has("assets")) {
            JSONArray assets = release.getJSONArray("assets");
            if (assets.length() > 0) {
                JSONObject asset = assets.getJSONObject(0);
                if (asset.has("browser_download_url")) {
                    invokeSelfUpdate(asset.getString("browser_download_url"));
                }
            }
        }
    };

    private static void invokeSelfUpdate(String url) throws IOException {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", String.join("-", "JamAlong", Main.version));
            Map<String, List<String>> map = connection.getHeaderFields();
            int length = map.containsKey("Content-Length") ? Integer.parseInt(map.get("Content-Length").get(0)) : -1;
            DownloadCallback callback = new DownloadCallback() {
                private double length, value = -1D;
                private int current;

                @Override
                public void add(int i) {
                    this.current += i;
                    if (length == -1) return;
                    JSONObject object = new JSONObject();
                    object.put("instruction", "download");
                    double current = Math.floor((this.current / length) * 100D);
                    if (current != value) {
                        value = current;
                        object.put("progress", value);
                        Logger.debug("[updater] {}%", value);
                        SocketServer.forward(object.toString());
                    }
                }

                @Override
                public void notify(int i) {
                    Logger.debug("[updater] target size: {}", i);
                    this.length = i;
                }
            };
            callback.notify(length);
            String name = url.substring(url.lastIndexOf("/") + 1);
            String filename = String.join(".", StaticConstant.PROJECT, "jar");
            Path path = StaticConstant.APPLICATION_CACHE.resolve(filename);
            Logger.debug("[updater] invoke download: {}", url);
            byte[] b = read(connection, callback);
            Logger.debug("[updater] writing: {}", path);
            Files.write(
                    path,
                    b,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            ProcessBuilder builder = new ProcessBuilder(
                    "java",
                    "-jar",
                    path.toString()
            );
            builder.start();
            Logger.debug("[updater] restarting for new version");
            System.exit(1);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private static byte[] read(HttpURLConnection connection, DownloadCallback callback) throws IOException {
        try (InputStream stream = connection.getInputStream()) {
            return BasicHttp.get(stream, callback);
        } catch (IOException e1) {
            try (InputStream stream = connection.getErrorStream()) {
                return stream == null ? null : BasicHttp.get(stream, callback);
            } catch (IOException e2) {
                throw e2;
            }
        }
    }
}