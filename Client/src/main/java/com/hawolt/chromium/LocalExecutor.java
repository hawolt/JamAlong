package com.hawolt.chromium;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;


/**
 * Created: 31/07/2022 00:57
 * Author: Twitter @hawolt
 **/

public class LocalExecutor {
    public static void configure() {
        path("/v1", () -> {
            path("/config", () -> {
                get("/close", context -> System.exit(0));
                get("/minimize", Jamalong.MINIMIZE);
                get("/maximize", Jamalong.MAXIMIZE);
            });
        });
    }
}