package com.hawolt.discord;

import com.hawolt.*;
import com.hawolt.chromium.LocalExecutor;
import com.hawolt.chromium.SocketServer;
import com.hawolt.cryptography.SHA256;
import com.hawolt.logger.Logger;
import com.hawolt.os.OperatingSystem;
import com.hawolt.os.SystemManager;
import de.jcm.discordgamesdk.*;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.activity.ActivityType;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class RichPresence implements Runnable, InstructionListener {
    public static Optional<RichPresence> create(RemoteClient remoteClient, PlaybackHandler playbackHandler) {
        String processName = switch (OperatingSystem.getOperatingSystemType()) {
            case MAC -> "Discord.app";
            case LINUX -> "Discord";
            case WINDOWS -> "Discord.exe";
            default -> null;
        };
        try {
            if (processName == null || !SystemManager.getInstance().isProcessRunning(processName)) {
                return Optional.empty();
            }
        } catch (IOException e) {
            Logger.error(e);
        }
        return Optional.of(new RichPresence(remoteClient, playbackHandler));
    }

    private final Instant reference = Instant.now();
    private final PlaybackHandler playbackHandler;
    private final RemoteClient remoteClient;
    private Core core;

    private RichPresence(RemoteClient remoteClient, PlaybackHandler playbackHandler) {
        this.playbackHandler = playbackHandler;
        this.remoteClient = remoteClient;
        ExecutorService loader = ExecutorManager.getService("rpc-loader");
        loader.execute(this);
        loader.shutdown();
    }

    @Override
    public void run() {
        try {
            File discordLibrary = DiscordLibraryManager.downloadDiscordLibrary();
            if (discordLibrary == null) throw new Exception("Failed to download library");
            Core.init(discordLibrary);
            try (CreateParams params = new CreateParams()) {
                params.setClientID(StaticConstant.DISCORD_APPLICATION_ID);
                params.setFlags(CreateParams.Flags.NO_REQUIRE_DISCORD);
                params.registerEventHandler(new DiscordEventHandler() {
                    @Override
                    public void onActivityJoin(String secret) {
                        Logger.debug("onActivityJoin:{}", secret);
                        if (core == null) return;
                        set(secret);
                        JSONObject kill = new JSONObject();
                        kill.put("instruction", "kill");
                        SocketServer.forward(kill.toString());
                        SocketServer.forward(LocalExecutor.join(remoteClient, playbackHandler, secret).toString());
                    }
                });
                try {
                    this.core = new Core(params);
                    this.core.setLogHook(LogLevel.ERROR, (logLevel, s) -> Logger.error("[discord-rpc] if rpc is working this can be ignored: {}", s));
                    String javaHome = System.getProperty("java.home");
                    if (javaHome == null) return;
                    File file = Paths.get(javaHome).resolve("bin").resolve("javaw.exe").toFile();
                    if (!file.exists()) return;
                    String javaPath = file.getAbsolutePath();
                    String jarPath = Main.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
                            .getPath()
                            .substring(1)
                            .replace("/", "\\");
                    if (jarPath.endsWith(".jar")) {
                        String command = new StringBuilder()
                                .append("\"")
                                .append(javaPath)
                                .append("\" -jar \"")
                                .append(jarPath)
                                .append("\" \"%1\"")
                                .toString();
                        Logger.debug("[discord] {}", command);
                        this.core.activityManager().registerCommand(command);
                    }
                    this.setIdlePresence();
                    while (true) {
                        core.runCallbacks();
                        try {
                            Thread.sleep(16);
                        } catch (InterruptedException ignored) {
                        }
                    }
                } catch (Exception e) {
                    Logger.error(e);
                } finally {
                    if (core != null) core.close();
                }
            }
        } catch (Exception e) {
            Logger.error("[discord-rpc] an error has occurred");
            Logger.error(e);
        }
    }

    public void setIdlePresence() {
        this.set(null);
    }

    private int partySize;

    public void set(String secret) {
        try (Activity activity = new Activity()) {
            activity.assets().setLargeImage("logo");
            activity.setType(ActivityType.LISTENING);
            String state = LocalExecutor.HOST_TYPE == HostType.HOST ? "Hosting" :
                    LocalExecutor.HOST_TYPE == HostType.ATTENDEE ? "Listening" : "Idle";
            activity.setDetails(state);
            String details = partySize > 1 ? "Jamming" :
                    !"Idle".equals(state) ? "Alone" : "Not Listening";
            activity.setState(details);
            if (secret != null) {
                activity.party().setID(SHA256.hash(secret));
                activity.party().size().setCurrentSize(Math.max(1, partySize));
                activity.party().size().setMaxSize(100);
                activity.secrets().setJoinSecret(secret);
            }

            if (reference != null) activity.timestamps().setStart(reference);
            core.activityManager().updateActivity(activity);
        }
    }

    @Override
    public void onInstruction(JSONObject object) {
        if (!"list".equals(object.getString("instruction"))) return;
        this.partySize = object.getJSONArray("users").length();
        if (LocalExecutor.PARTY_ID != null) set(LocalExecutor.PARTY_ID);
    }
}