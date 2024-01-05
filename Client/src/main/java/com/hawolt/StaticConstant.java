package com.hawolt;

import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticConstant {
    public static final String PROJECT = "JamAlong";
    public static final String PROJECT_DATA = "project.json";
    public static final String PROJECT_RELEASES = "https://api.github.com/repos/hawolt/jamalong/releases/latest";
    public static final Path APPLICATION_CACHE = Paths.get(System.getProperty("java.io.tmpdir")).resolve("jamalong");
    public static final Path APPLICATION_SETTINGS = Paths.get(System.getProperty("user.home")).resolve(PROJECT);
    public static final String CLIENT_SETTING_fILE = ".jamalong";
    public static final long DISCORD_APPLICATION_ID = 1191924673441697822L;
    public static final int SELF_PORT = 42069;
}
