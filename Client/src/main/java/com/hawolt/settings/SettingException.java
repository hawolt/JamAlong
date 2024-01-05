package com.hawolt.settings;

public class SettingException extends Exception {
    public SettingException(String type) {
        super("Setting for " + type + " does not exist");
    }
}
