package com.ibm.airlock.common.exceptions;

/**
 * This exception will be thrown when the airlock defaults file parsing fails
 *
 * @author Denis Voloshin
 */
public class AirlockInvalidFileException extends Exception {
    private static final long serialVersionUID = -7034744975122798077L;
    /**
     * Construct a new AirlockInvalidFileException
     *
     * @param message a message to be added to this exception
     */
    public AirlockInvalidFileException(String message) {
        super(message);
    }
}
