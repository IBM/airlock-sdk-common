package com.ibm.airlock.common.exceptions;

/**
 * This exception will be thrown when the app version doesn't match the defaults file
 *
 * @author Denis Voloshin
 */
public class AirlockMismatchSeasonException extends Exception {
    private static final long serialVersionUID = -2681127506783026738L;

    /**
     * Construct a new AirlockMismatchSeasonException
     *
     * @param message a message to be added to this exception
     */
    public AirlockMismatchSeasonException(String message) {
        super(message);
    }
}
