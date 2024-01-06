package com.hawolt.remote;

import org.json.JSONObject;

public interface InstructionListener {
    void onInstruction(JSONObject object);
}
