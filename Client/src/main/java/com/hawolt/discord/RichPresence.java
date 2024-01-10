package com.hawolt.discord;

import com.hawolt.*;
import com.hawolt.audio.AudioManager;
import com.hawolt.localhost.LocalExecutor;
import com.hawolt.chromium.SocketServer;
import com.hawolt.cryptography.SHA256;
import com.hawolt.logger.Logger;
import com.hawolt.misc.ExecutorManager;
import com.hawolt.misc.HostType;
import com.hawolt.os.OperatingSystem;
import com.hawolt.os.SystemManager;
import com.hawolt.remote.InstructionListener;
import com.hawolt.remote.RemoteClient;
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
    public static Optional<RichPresence> create(Application application) {
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
        return Optional.of(new RichPresence(application));
    }

    private final Instant reference = Instant.now();
    private final LocalExecutor localExecutor;
    private final AudioManager audioManager;
    private final RemoteClient remoteClient;
    private final SocketServer socketServer;
    private Core core;

    private RichPresence(Application application) {
        this.audioManager = application.getAudioManager();
        this.remoteClient = application.getRemoteClient();
        this.socketServer = application.getSocketServer();
        this.localExecutor = application.getLocalExecutor();
        ExecutorService loader = ExecutorManager.getService("rpc-loader");
        loader.execute(this);
        loader.shutdown();
    }

    @Override
    public void run() {
        try {
            File discordLibrary = DiscordGameSDK.loadFromCacheOrDownload();
            if (discordLibrary == null) throw new Exception("Failed to download library");
            Core.init(discordLibrary);
            try (CreateParams params = new CreateParams()) {
                params.setClientID(StaticConstant.DISCORD_APPLICATION_ID);
                params.setFlags(CreateParams.Flags.NO_REQUIRE_DISCORD);
                params.registerEventHandler(new DiscordEventHandler() {
                    @Override
                    public void onActivityJoin(String secret) {
                        Logger.info("onActivityJoin:{}", secret);
                        if (core == null) return;
                        set(secret);
                        JSONObject kill = new JSONObject();
                        kill.put("instruction", "kill");
                        socketServer.forward(kill.toString());
                        socketServer.forward(localExecutor.join(remoteClient, audioManager, secret).toString());
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
            String state = localExecutor.getHostType() == HostType.HOST ? "Hosting" :
                    localExecutor.getHostType() == HostType.ATTENDEE ? "Listening" : "Idle";
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
        if (localExecutor.getPartyId() != null) set(localExecutor.getPartyId());
    }
}
