package com.hawolt.discord;

import com.hawolt.ExecutorManager;
import com.hawolt.StaticConstant;
import com.hawolt.logger.Logger;
import com.hawolt.os.OperatingSystem;
import com.hawolt.os.SystemManager;
import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.LogLevel;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.activity.ActivityType;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class RichPresence implements Runnable {
    public static Optional<RichPresence> create() {
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
        return Optional.of(new RichPresence());
    }

    private final Instant reference = Instant.now();
    private Core core;

    private RichPresence() {
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
                try {
                    this.core = new Core(params);
                    this.core.setLogHook(LogLevel.ERROR, (logLevel, s) -> Logger.error("[discord-rpc] if rpc is working this can be ignored: {}", s));
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
        this.set("we love music", "Idle", reference);
    }

    public void set(String details, String state) {
        this.set(details, state, null);
    }

    public void set(String details, String state, Instant instant) {
        try (Activity activity = new Activity()) {
            activity.assets().setLargeImage("logo");
            activity.setType(ActivityType.LISTENING);
            activity.setDetails(details);
            if (instant != null) activity.timestamps().setStart(instant);
            core.activityManager().updateActivity(activity);
        }
    }
}
