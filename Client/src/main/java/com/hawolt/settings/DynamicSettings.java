package com.hawolt.settings;

import org.json.JSONObject;

public class DynamicSettings extends DynamicObject {
    protected final SettingService service;

    public DynamicSettings(JSONObject o, SettingService service) {
        super(o);
        this.service = service;
    }
}
