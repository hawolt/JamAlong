package com.hawolt.chromium;

import com.hawolt.HostType;
import com.hawolt.PlaybackHandler;
import com.hawolt.RemoteClient;
import com.hawolt.audio.SystemAudio;
import com.hawolt.common.Pair;
import com.hawolt.source.AudioSource;
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
                get("/websocket", context -> context.result(String.valueOf(websocketPort)));
                get("/gain/{value}", context -> SystemAudio.setGain(Float.parseFloat(context.pathParam("value"))));
            });
            path("/api", () -> {
                get("/load", new ContextBiConsumer<>(source, LOAD));
                get("/host", new ContextBiConsumer<>(remoteClient, HOST));
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

    private static BiConsumer<Context, RemoteClient> HOST = (context, remoteClient) -> {
        LocalExecutor.HOST_TYPE = HostType.HOST;
        JSONObject object = remoteClient.executeBlocking("create");
        LocalExecutor.PARTY_ID = object.getString("result");
        context.result(object.toString());
        /*
        object -> {
            String roomId = object.getString("result");
            Logger.debug("Created room:{}", roomId);
            soundcloudInterface.getInput().setEnabled(true);
            soundcloudInterface.getInput().setText("");
            client.setRoomId(roomId);
            input.setText(roomId);
            start.setEnabled(false);
            client.addStreamUpdateListener(new StreamUpdateListener() {
                @Override
                public void onAudioUpdate(Audio audio, long timestamp) {
                    client.execute("revalidate", object -> {
                        boolean success = object.getString("result").equals(client.getRoomId());
                        Logger.debug("revalidated:{}", success);
                    }, client.getRoomId(), String.valueOf(timestamp), audio.source());
                }

                @Override
                public void onAudioPeekUpdate(Audio audio) {
                    client.execute("preload", object -> {
                        boolean success = object.getString("result").equals(client.getRoomId());
                        Logger.debug("preload:{}", success);
                    }, client.getRoomId(), audio.source());
                }
            });
        }
         */
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
                context.pathParam("name"),
                context.pathParam("partyId")
        );
        context.result(object.toString());
    };
}