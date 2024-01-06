package com.hawolt.settings;

import com.hawolt.Main;
import com.hawolt.StaticConstant;
import com.hawolt.misc.ExecutorManager;
import com.hawolt.misc.Unsafe;
import com.hawolt.logger.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class SettingManager implements SettingService {
    private final Map<String, List<SettingListener<?>>> map = new HashMap<>();
    private final ClientSettings client;
    private String username;

    public SettingManager() {
        this.client = new ClientSettings(load(), this);
    }

    @Override
    public ClientSettings getClientSettings() {
        return client;
    }

    @Override
    public void write(String name, Object o) {
        Logger.debug("[settings] write '{}' as '{}'", name, o == null ? "null" : o);
        client.put(name, o);
        write();
        this.dispatch(name, o);
    }

    @Override
    public void addSettingListener(String name, SettingListener<?> listener) {
        Logger.debug("[settings] add '{}' listener with source '{}'", name, listener.getClass().getCanonicalName());
        if (!map.containsKey(name)) map.put(name, new ArrayList<>());
        this.map.get(name).add(listener);
    }

    private void dispatch(String name, Object value) {
        Logger.debug("[settings] dispatch '{}' as '{}'", name, value == null ? "null" : value);
        List<SettingListener<?>> list = map.get(name);
        if (list == null) return;
        for (SettingListener<?> listener : list) {
            ExecutorService service = ExecutorManager.getService("pool");
            service.execute(() -> listener.onSettingWrite(name, Unsafe.cast(value)));
        }
    }

    private JSONObject load() {
        Logger.debug("[settings] load local setting");
        Path path = StaticConstant.APPLICATION_SETTINGS.resolve(StaticConstant.CLIENT_SETTING_fILE);
        try {
            return new JSONObject(new String(Files.readAllBytes(path)));
        } catch (IOException e) {
            Logger.info("Unable to locate client-setting file", path.toFile());
        }
        return new JSONObject();
    }

    private void write() {
        Logger.debug("[settings] write local setting");
        Path path = StaticConstant.APPLICATION_SETTINGS.resolve(StaticConstant.CLIENT_SETTING_fILE);
        try {
            Files.createDirectories(path.getParent());
            byte[] content = client.toString().getBytes(StandardCharsets.UTF_8);
            Files.write(
                    path,
                    content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            Logger.info("Unable to create directory {}", path.toFile());
        }
    }
}
