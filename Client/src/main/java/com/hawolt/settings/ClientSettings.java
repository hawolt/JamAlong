package com.hawolt.settings;

import org.json.JSONObject;

public class ClientSettings extends DynamicSettings {
    public ClientSettings(JSONObject o, SettingService service) {
        super(o, service);
    }

    public String getUsername() {
        return getByKeyOrDefault("name", "anon");
    }

    public int getClientVolumeGain() {
        return getByKeyOrDefault("gain", -40);
    }
}
