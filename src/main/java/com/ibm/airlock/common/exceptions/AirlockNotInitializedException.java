package com.ibm.airlock.common.exceptions;

/**
 * This exception will be thrown
 *
 * @author Denis Voloshin
 */
public class AirlockNotInitializedException extends Exception {
    private static final long serialVersionUID = -8931129311027230309L;

    /**
     * Construct a new AirlockNotInitializedException.
     * This exception will be thrown if the sdk has not been initialized.
     *
     * @param message a message to be added to this exception
     */
    public AirlockNotInitializedException(String message) {
        super(message);
    }
}
