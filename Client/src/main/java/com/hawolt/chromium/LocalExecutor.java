package com.hawolt.chromium;

import com.hawolt.HostType;
import com.hawolt.Main;
import com.hawolt.PlaybackHandler;
import com.hawolt.RemoteClient;
import com.hawolt.audio.SystemAudio;
import com.hawolt.common.Pair;
import com.hawolt.discord.RichPresence;
import com.hawolt.logger.Logger;
import com.hawolt.source.Audio;
import com.hawolt.source.AudioSource;
import com.hawolt.source.StreamUpdateListener;
import com.hawolt.source.impl.AbstractAudioSource;
import io.javalin.http.Context;
import org.json.JSONObject;

import java.util.Base64;
import java.util.Optional;
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

    public static void configure(
            int websocketPort,
            PlaybackHandler playbackHandler,
            AbstractAudioSource source,
            RemoteClient remoteClient) {
        path("/v1", () -> {
            path("/config", () -> {
                get("/skip", context -> playbackHandler.skip());
                get("/websocket", context -> context.result(String.valueOf(websocketPort)));
                get("/reset", new ContextBiConsumer<>(Pair.of(remoteClient, playbackHandler), RESET));
                get("/gain/{value}", context -> SystemAudio.setGain(Float.parseFloat(context.pathParam("value"))));
            });
            path("/api", () -> {
                get("/load", new ContextBiConsumer<>(source, LOAD));
                get("/discover", new ContextBiConsumer<>(remoteClient, DISCOVER));
                get("/host", new ContextBiConsumer<>(Pair.of(remoteClient, playbackHandler), HOST));
                get("/namechange/{name}/{partyId}", new ContextBiConsumer<>(remoteClient, NAMECHANGE));
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
        Main.presence.ifPresent(presence -> presence.setPartyPresence("Hosting", LocalExecutor.PARTY_ID, LocalExecutor.PARTY_ID));
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
        JSONObject object = remoteClient.executeBlocking(
                "join",
                partyId
        );
        String[] arguments = object.getString("result").split(" ");
        if (arguments.length > 0) {
            LocalExecutor.PARTY_ID = arguments[0];
            Main.presence.ifPresent(presence -> presence.setPartyPresence("Listening", LocalExecutor.PARTY_ID, LocalExecutor.PARTY_ID));
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

    private static BiConsumer<Context, RemoteClient> NAMECHANGE = (context, remoteClient) -> {
        JSONObject object = remoteClient.executeBlocking(
                "name",
                context.pathParam("partyId"),
                context.pathParam("name")
        );
        context.result(object.toString());
    };
    private static BiConsumer<Context, RemoteClient> DISCOVER = (context, remoteClient) -> {
        Main.presence.ifPresent(presence -> presence.setIdlePresence());
        JSONObject object = remoteClient.executeBlocking(
                "discover"
        );
        context.result(object.toString());
    };
    private static BiConsumer<Context, Pair<RemoteClient, PlaybackHandler>> RESET = (context, pair) -> {
        LocalExecutor.RESET_TIMESTAMP = System.currentTimeMillis();
        RemoteClient remoteClient = pair.getK();
        JSONObject object = remoteClient.executeBlocking(
                "leave",
                LocalExecutor.PARTY_ID == null ? "nil" : LocalExecutor.PARTY_ID
        );
        PlaybackHandler playbackHandler = pair.getV();
        playbackHandler.reset();
        context.result(object.toString());
    };
    private static BiConsumer<Context, RemoteClient> VISIBILITY = (context, remoteClient) -> {
        JSONObject object = remoteClient.executeBlocking(
                "visibility",
                context.pathParam("partyId"),
                context.pathParam("status")
        );
        context.result(object.toString());
    };
}