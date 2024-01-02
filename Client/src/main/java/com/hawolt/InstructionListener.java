package com.hawolt;

import org.json.JSONObject;

public interface InstructionListener {
    void onInstruction(JSONObject object);
}
