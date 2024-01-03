package com.hawolt.registry;

public class RegistryPermissionException extends Exception {
    private final String command;

    public RegistryPermissionException(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
