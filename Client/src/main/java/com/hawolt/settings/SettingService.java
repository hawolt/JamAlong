package com.hawolt.settings;

public interface SettingService {
    ClientSettings getClientSettings();

    void write(String name, Object o);

    void addSettingListener(String name, SettingListener<?> listener);
}
