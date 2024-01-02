package com.hawolt.chromium;

import com.hawolt.HostType;
import com.hawolt.PlaybackHandler;
import com.hawolt.RemoteClient;
import com.hawolt.audio.SystemAudio;
import com.hawolt.common.Pair;
import com.hawolt.logger.Logger;
import com.hawolt.source.Audio;
import com.hawolt.source.AudioSource;
import com.hawolt.source.StreamUpdateListener;
import com.hawolt.source.impl.AbstractAudioSource;
import io.javalin.http.Context;
import org.json.JSONObject;

import java.util.Base64;
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

    public static void configure(int websocketPort, PlaybackHandler playbackHandler, AbstractAudioSource source, RemoteClient remoteClient) {
        path("/v1", () -> {
            path("/config", () -> {
                get("/skip", context -> playbackHandler.skip());
                get("/websocket", context -> context.result(String.valueOf(websocketPort)));
                get("/gain/{value}", context -> SystemAudio.setGain(Float.parseFloat(context.pathParam("value"))));
            });
            path("/api", () -> {
                get("/load", new ContextBiConsumer<>(source, LOAD));
                get("/discover", new ContextBiConsumer<>(remoteClient, DISCOVER));
                get("/host", new ContextBiConsumer<>(Pair.of(remoteClient, playbackHandler), HOST));
                get("/namechange/{name}/{partyId}", new ContextBiConsumer<>(remoteClient, NAMECHANGE));
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
        context.result(object.toString());
        playbackHandler.addStreamUpdateListener(new StreamUpdateListener() {
            @Override
            public void onAudioUpdate(Audio audio, long timestamp) {
                remoteClient.executeAsynchronous("revalidate", object -> {
                    boolean success = object.getString("result").equals(LocalExecutor.PARTY_ID);
                    Logger.debug("revalidated:{}", success);
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
    private static BiConsumer<Context, Pair<RemoteClient, PlaybackHandler>> JOIN = (context, pair) -> {
        LocalExecutor.HOST_TYPE = HostType.ATTENDEE;
        RemoteClient remoteClient = pair.getK();
        PlaybackHandler playbackHandler = pair.getV();
        JSONObject object = remoteClient.executeBlocking(
                "join",
                context.pathParam("partyId")
        );
        context.result(object.toString());
        String[] arguments = object.getString("result").split(" ");
        if (arguments.length == 0) return;
        LocalExecutor.PARTY_ID = arguments[0];
        playbackHandler.clearStreamUpdateListeners();
        if (arguments.length == 1) return;
        playbackHandler.setTimestamp(Long.parseLong(arguments[1]));
        AudioSource source = playbackHandler.getAudioSource();
        source.load(arguments[2]);
        if (arguments.length == 3) return;
        source.preload(arguments[3]);
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
        JSONObject object = remoteClient.executeBlocking(
                "discover"
        );
        context.result(object.toString());
    };
}