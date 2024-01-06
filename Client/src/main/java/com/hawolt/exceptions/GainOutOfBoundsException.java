package com.hawolt.exceptions;

/**
 * to be thrown when the specified Gain value is out of the possible FloatControl bounds.
 */
public class GainOutOfBoundsException extends RuntimeException {
    public GainOutOfBoundsException(float value) {
        super("Gain value has to be between -80 and 6, specified value was " + value);
    }
}
