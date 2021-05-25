package com.ibm.airlock.common;

/**
 * This exception will be thrown
 *
 * @author Rachel Levy
 */
public class AirlockNotInitializedException extends Exception {
    /**
     * Construct a new AirlockNotInitializedException.
     * This exception will be thrown if the sdk has not been initialized.
     *
     * @param mag a message to be added to this exception
     */
    public AirlockNotInitializedException(String mag) {
        super(mag);
    }
}
