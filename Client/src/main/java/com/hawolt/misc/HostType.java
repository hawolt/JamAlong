package com.hawolt;

/**
 * Enum to differentiate between the different listening types
 * <p>
 * UNKNOWN      User is not part of a party
 * ATTENDEE     User is part of a party
 * HOST         User is hosting a party
 */
public enum HostType {
    ATTENDEE, HOST, UNKNOWN;
}
