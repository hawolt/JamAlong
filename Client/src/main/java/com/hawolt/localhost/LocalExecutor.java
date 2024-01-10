package com.hawolt.localhost;

import com.hawolt.*;
import com.hawolt.audio.AudioManager;
import com.hawolt.audio.AudioSystemWrapper;
import com.hawolt.chromium.SocketServer;
import com.hawolt.common.Pair;
import com.hawolt.data.media.hydratable.impl.track.Track;
import com.hawolt.data.media.search.Explorer;
import com.hawolt.data.media.search.query.impl.SearchQuery;
import com.hawolt.discord.RichPresence;
import com.hawolt.http.BasicHttp;
import com.hawolt.http.Request;
import com.hawolt.http.misc.DownloadCallback;
import com.hawolt.logger.Logger;
import com.hawolt.misc.HostType;
import com.hawolt.misc.Network;
import com.hawolt.remote.RemoteClient;
import com.hawolt.settings.SettingManager;
import com.hawolt.media.Audio;
import com.hawolt.media.AudioSource;
import com.hawolt.media.StreamUpdateListener;
import com.hawolt.media.impl.AbstractAudioSource;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class LocalExecutor implements DownloadCallback {
    private final Application application;
    private HostType hostType = HostType.UNKNOWN;
    private double length, value = -1D;
    private JSONObject release;
    private String partyId;
    private int current;

    public LocalExecutor(Application application) {
        this.application = application;
    }

    public HostType getHostType() {
        return hostType;
    }

    public String getPartyId() {
        return partyId;
    }

    public void configure() {
        path("/v1", () -> {
            path("/config", () -> {
                get("/reset", new ContextBiConsumer<>(application, RESET));
                get("/skip", context -> application.getAudioManager().skip());
                get("/gain/{value}", new ContextBiConsumer<>(application, GAIN));
                get("/invoke", new ContextBiConsumer<>(application, SELF_UPDATE));
                get("/version", new ContextBiConsumer<>(application, UPDATE_CHECK));
                get("/websocket", context -> context.result(String.valueOf(application.getWebSocketPort())));
                get("/gain", context -> context.result(String.valueOf(application.getSettingManager().getClientSettings().getClientVolumeGain())));
            });
            path("/api", () -> {
                get("/load", new ContextBiConsumer<>(application, LOAD));
                get("/host", new ContextBiConsumer<>(application, HOST));
                get("/reveal", new ContextBiConsumer<>(application, REVEAL));
                get("/discover", new ContextBiConsumer<>(application, DISCOVER));
                get("/join/{partyId}", new ContextBiConsumer<>(application, JOIN));
                get("/chat/{message}", new ContextBiConsumer<>(application, MESSAGE));
                get("/namechange/{name}/{partyId}", new ContextBiConsumer<>(application, NAMECHANGE));
                get("/visibility/{partyId}/{status}", new ContextBiConsumer<>(application, VISIBILITY));
                get("/gatekeeper/{partyId}/{status}", new ContextBiConsumer<>(application, GATEKEEPER));
            });
        });
    }

    private BiConsumer<Context, Application> LOAD = (context, application) -> {
        String url = context.queryParam("url");
        if (url == null) context.result("NO_URL");
        else {
            String plain = new String(Base64.getDecoder().decode(url.getBytes()));
            if (plain.startsWith("https")) {
                application.getAudioSource().load(plain);
                context.result(plain);
            } else {
                SearchQuery query = new SearchQuery(plain);
                try {
                    Iterator<Track> iterator = Explorer.browse(query).iterator();
                    if (iterator.hasNext()) {
                        String link = iterator.next().getLink();
                        application.getAudioSource().load(link);
                        context.result(link);
                    }
                } catch (Exception e) {
                    Logger.error(e);
                }
            }
        }
    };

    private BiConsumer<Context, Application> HOST = (context, application) -> {
        this.hostType = HostType.HOST;
        RemoteClient remoteClient = application.getRemoteClient();
        AudioManager audioManager = application.getAudioManager();
        JSONObject object = remoteClient.executeBlocking("create");
        this.partyId = object.getString("result").split(" ")[0];
        application.getRichPresence().ifPresent(presence -> presence.set(this.partyId));
        context.result(object.toString());
        audioManager.addStreamUpdateListener(new StreamUpdateListener() {
            @Override
            public void onAudioUpdate(Audio audio, long timestamp) {
                remoteClient.executeAsynchronous("revalidate", object -> {
                    String result = object.getString("result");
                    if ("UNKNOWN_ROOM".equals(result)) {
                        JSONObject instruction = new JSONObject();
                        instruction.put("instruction", "kill");
                        application.getSocketServer().forward(instruction.toString());
                        audioManager.reset();
                    }
                    Logger.debug("revalidated:{}", result.equals(LocalExecutor.this.partyId));
                }, LocalExecutor.this.partyId, String.valueOf(timestamp), audio.source());
            }

            @Override
            public void onAudioPeekUpdate(Audio audio) {
                remoteClient.executeAsynchronous("preload", object -> {
                    boolean success = object.getString("result").equals(LocalExecutor.this.partyId);
                    Logger.debug("preload:{}", success);
                }, LocalExecutor.this.partyId, audio.source());
            }
        });
    };

    public JSONObject join(RemoteClient remoteClient, AudioManager audioManager, String partyId) {
        this.hostType = HostType.ATTENDEE;
        this.application.getAudioManager().setGatekeeper(true);
        JSONObject object = remoteClient.executeBlocking("join", partyId);
        String[] arguments = object.getString("result").split(" ");
        if (arguments.length > 0) {
            this.partyId = arguments[0];
            application.getRichPresence().ifPresent(presence -> presence.set(this.partyId));
            audioManager.clearStreamUpdateListeners();
            if (arguments.length > 2) {
                audioManager.setTimestamp(Long.parseLong(arguments[1]));
                AudioSource source = audioManager.getAudioSource();
                source.load(arguments[2]);
                if (arguments.length > 3) {
                    source.preload(arguments[3]);
                }
            }
        }
        return object;
    }

    private BiConsumer<Context, Application> REVEAL = (context, application) -> {
        Audio current = application.getAudioManager().getCurrent();
        if (current == null) return;
        try {
            Network.browse(current.source());
        } catch (IOException e) {
            Logger.error(e);
        }
    };

    private BiConsumer<Context, Application> JOIN = (context, application) -> {
        RemoteClient remoteClient = application.getRemoteClient();
        AudioManager audioManager = application.getAudioManager();
        String partyId = context.pathParam("partyId");
        context.result(join(remoteClient, audioManager, partyId).toString());
    };
    private BiConsumer<Context, Application> MESSAGE = (context, application) -> {
        RemoteClient remoteClient = application.getRemoteClient();
        String message = context.pathParam("message");
        JSONObject response = remoteClient.executeBlocking(
                "chat",
                partyId,
                message
        );
    };

    private BiConsumer<Context, Application> NAMECHANGE = (context, application) -> {
        RemoteClient remoteClient = application.getRemoteClient();
        SettingManager manager = application.getSettingManager();
        String name = context.pathParam("name");
        JSONObject object = remoteClient.executeBlocking("name", context.pathParam("partyId"), name);
        String plaintext = new String(Base64.getDecoder().decode(context.pathParam("name").getBytes(StandardCharsets.UTF_8)));
        String result = object.getString("result").replace(" \uD83D\uDC51", "");
        if (result.equals(plaintext)) {
            manager.write("name", plaintext);
        }
        context.result(object.toString());
    };

    private BiConsumer<Context, Application> DISCOVER = (context, application) -> {
        application.getRichPresence().ifPresent(RichPresence::setIdlePresence);
        JSONObject object = application.getRemoteClient().executeBlocking("discover");
        context.result(object.toString());
    };
    private BiConsumer<Context, Application> RESET = (context, application) -> {
        AudioManager audioManager = application.getAudioManager();
        audioManager.getAudioSource().setPartyLeaveTimestamp(System.currentTimeMillis());
        RemoteClient remoteClient = application.getRemoteClient();
        JSONObject object = remoteClient.executeBlocking("leave", this.partyId == null ? "nil" : this.partyId);
        audioManager.reset();
        context.result(object.toString());
    };
    private BiConsumer<Context, Application> VISIBILITY = (context, application) -> {
        JSONObject object = application.getRemoteClient().executeBlocking("visibility", context.pathParam("partyId"), context.pathParam("status"));
        context.result(object.toString());
    };
    private BiConsumer<Context, Application> GATEKEEPER = (context, application) -> {
        JSONObject object = application.getRemoteClient().executeBlocking("gatekeeper", context.pathParam("partyId"), context.pathParam("status"));
        context.result(object.toString());
    };

    private BiConsumer<Context, Application> GAIN = (context, application) -> {
        float gain = Float.parseFloat(context.pathParam("value"));
        application.getAudioManager().getSystemAudio().setGain(gain);
        application.getDebouncer().debounce("gain", () -> {
            application.getSettingManager().write("gain", gain);
        }, 200, TimeUnit.MILLISECONDS);
    };

    private void fetchLatestRelease() {
        Request request = new Request(StaticConstant.PROJECT_RELEASES);
        try {
            this.release = new JSONObject(request.execute().getBodyAsString());
        } catch (IOException e) {
            this.release = new JSONObject();
        }
    }

    private BiConsumer<Context, Application> UPDATE_CHECK = (context, application) -> {
        if (release == null) fetchLatestRelease();
        if (!release.has("tag_name")) {
            context.result("false");
        } else {
            String tag = release.getString("tag_name");
            if (!application.getVersion().equals(tag) && release.has("assets")) {
                context.result("true");
            } else {
                context.result("false");
            }
        }
    };

    private BiConsumer<Context, Application> SELF_UPDATE = (context, application) -> {
        if (release == null) fetchLatestRelease();
        if (release.has("assets")) {
            JSONArray assets = release.getJSONArray("assets");
            if (assets.length() > 0) {
                JSONObject asset = assets.getJSONObject(0);
                if (asset.has("browser_download_url")) {
                    try {
                        if (application.getServerSocket() != null) {
                            application.setGracefulShutdown(true);
                            application.getServerSocket().close();
                            application.nullifyServerSocket();
                        }
                    } catch (IOException e) {
                        Logger.error(e);
                    }
                    invokeSelfUpdate(asset.getString("browser_download_url"));
                }
            }
        }
    };

    private void invokeSelfUpdate(String url) {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", String.join("-", "JamAlong", application.getVersion()));
            Map<String, List<String>> map = connection.getHeaderFields();
            int length = map.containsKey("Content-Length") ? Integer.parseInt(map.get("Content-Length").get(0)) : -1;
            notify(length);
            String name = url.substring(url.lastIndexOf("/") + 1);
            String filename = String.join(".", StaticConstant.PROJECT, "jar");
            Path path = StaticConstant.APPLICATION_CACHE.resolve(filename);
            Logger.info("[updater] invoke download: {}", url);
            byte[] b = read(connection, this);
            write(path, b);
            try {
                write(Paths.get(System.getProperty("user.dir")).resolve(name), b);
            } catch (Exception e) {
                // ignored
            }
            ProcessBuilder builder = new ProcessBuilder("java", "-jar", path.toString());
            builder.start();
            Logger.info("[updater] restarting for new version");
            System.exit(1);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private void write(Path path, byte[] b) throws IOException {
        Logger.debug("[updater] writing: {}", path);
        Files.write(path, b, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private byte[] read(HttpURLConnection connection, DownloadCallback callback) throws IOException {
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
            application.getSocketServer().forward(object.toString());
        }
    }

    @Override
    public void notify(int i) {
        Logger.debug("[updater] target size: {}", i);
        this.length = i;
    }
}