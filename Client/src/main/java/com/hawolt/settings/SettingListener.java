package com.hawolt.settings;

public interface SettingListener<T> {
    void onSettingWrite(String name, T value);
}
